package com.signagepro.player.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.signagepro.player.api.ApiClient
import com.signagepro.player.api.PlaylistDto
import com.signagepro.player.cache.MediaCacheRepo
import com.signagepro.player.config.ConfigStore
import com.signagepro.player.net.ControlChannel
import com.signagepro.player.net.HeartbeatService
import com.signagepro.player.render.MediaRenderer
import com.signagepro.player.sync.NtpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * player 전체 흐름 오케스트레이션.
 *
 * 부팅:
 *   1. NTP 동기 (best-effort)
 *   2. /api/devices/:id → groupId
 *   3. /api/groups/:gid/playlist → PlaylistStore 저장 (실패 시 캐시)
 *   4. 미디어 전체 prefetch + LRU trim
 *   5. 슬롯 루프 시작
 *   6. HeartbeatService 시작 (TCP 10080)
 *   7. ControlChannel 시작 (Socket.io)
 *
 * playlist_updated/group_assignment_changed 이벤트 수신 시 [refreshPlaylist] 호출.
 * update_apk 이벤트 수신 시 [downloadAndInstallApk] 호출 (OTA 업데이트).
 */
class PlayerCoordinator(
    private val context: Context,
    private val config: ConfigStore,
    private val renderer: MediaRenderer,
    private val onStatus: (String) -> Unit,
    private val onDebug: (String) -> Unit = {}
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val ntp = NtpClient()
    private val cache = MediaCacheRepo(context)
    private val store = PlaylistStore(context)
    private val metrics = SystemMetrics(context)
    private val refreshMutex = Mutex()

    private var engine: PlaylistEngine? = null
    private var loopJob: Job? = null
    private var heartbeat: HeartbeatService? = null
    private var control: ControlChannel? = null
    private val scheduleManager = ScreenScheduleManager(context)

    fun start() {
        scope.launch {
            try {
                bootstrap()
            } catch (e: Exception) {
                Log.e(TAG, "bootstrap 실패", e)
                onStatus("오류: ${e.message ?: "알 수 없는 오류"}")
            }
        }
    }

    private suspend fun bootstrap() {
        val deviceId = config.deviceId ?: return onStatus("디바이스 ID 미설정")
        val serverUrl = config.serverUrl ?: return onStatus("서버 URL 미설정")
        val secret = config.deviceSecret ?: return onStatus("디바이스 시크릿 미설정")

        onStatus("시각 동기 중…")
        ntp.sync(serverUrl)

        engine = PlaylistEngine(ntp, deviceId)

        onStatus("playlist 조회 중…")
        val playlist = fetchPlaylistOrCached(serverUrl, deviceId)
        if (playlist == null || playlist.medias.isEmpty()) {
            onStatus("재생 가능한 미디어가 없습니다.")
        } else {
            engine!!.setPlaylist(playlist.medias)
            onStatus("미디어 다운로드 중…")
            prefetchAll(serverUrl, playlist)
            onStatus("")
            startLoop()
        }

        startHeartbeat(serverUrl, deviceId, secret)
        startControlChannel(serverUrl, deviceId)
        startTimeSyncLoop(serverUrl)
        scheduleManager.start(scope)
        if (DEBUG_OVERLAY) startDebugLoop(deviceId)
    }

    /** 주기적 시각 재동기 */
    private fun startTimeSyncLoop(serverUrl: String) {
        scope.launch {
            while (isActive) {
                delay(RESYNC_INTERVAL_MS)
                val ok = ntp.sync(serverUrl)
                Log.i(TAG, "시각 재동기: ${if (ok) ntp.sourceLabel else "실패"}")
            }
        }
    }

    /** 진단 오버레이 */
    private fun startDebugLoop(deviceId: String) {
        val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        scope.launch {
            while (isActive) {
                val slot = engine?.current()
                val dbg = buildString {
                    append("dev=$deviceId\n")
                    append("ntp=${ntp.sourceLabel}\n")
                    append("now=${fmt.format(Date(ntp.now()))}\n")
                    if (slot != null) {
                        append("slot=${slot.index + 1}/${slot.total} rem=${slot.remainingSec}s\n")
                        append("media=${slot.item.media.filename}\n")
                        append("trans=${slot.item.transition ?: "-"}")
                    } else {
                        append("slot=none")
                    }
                }
                onDebug(dbg)
                delay(1000)
            }
        }
    }

    private suspend fun fetchPlaylistOrCached(serverUrl: String, deviceId: String): PlaylistDto? {
        return try {
            val api = ApiClient.get(serverUrl)
            val device = api.getDevice(deviceId)
            // 스케줄 업데이트 (서버에서 내려온 enabled 스케줄만 포함됨)
            scheduleManager.update(device.schedules)
            val groupId = device.groupId ?: error("기기가 그룹에 배정되지 않았습니다")
            val playlist = api.getPlaylist(groupId)
            store.save(playlist)
            playlist
        } catch (e: Exception) {
            Log.w(TAG, "서버에서 playlist 가져오기 실패, 캐시 사용", e)
            store.load()
        }
    }

    private suspend fun prefetchAll(serverUrl: String, playlist: PlaylistDto) {
        val activeHashes = mutableSetOf<String>()
        for (item in playlist.medias) {
            item.media.hash?.let { activeHashes.add(it) }
            try {
                cache.ensure(serverUrl, item.media)
            } catch (e: Exception) {
                Log.w(TAG, "미디어 다운로드 실패: ${item.media.filename}", e)
            }
        }
        cache.trim(activeHashes)
    }

    private fun refreshPlaylist() {
        val deviceId = config.deviceId ?: return
        val serverUrl = config.serverUrl ?: return
        scope.launch {
            refreshMutex.withLock {
                Log.i(TAG, "playlist refresh 시작")
                val playlist = fetchPlaylistOrCached(serverUrl, deviceId) ?: return@withLock
                if (playlist.medias.isEmpty()) return@withLock
                prefetchAll(serverUrl, playlist)
                engine?.setPlaylist(playlist.medias)
                if (loopJob == null || loopJob?.isActive != true) startLoop()
                onStatus("")   // 재생 시작 → 상태 문구 제거
            }
        }
    }

    // ── OTA 업데이트 ──────────────────────────────────────────────────────────

    /**
     * APK 다운로드 후 설치 시도 순서:
     *  1. pm install -r        — 일부 상업용 STB ROM에서 UI 없이 자동 설치
     *  2. PackageInstaller API — Android 표준:
     *       STATUS_SUCCESS            → 무확인 자동 설치 완료
     *       STATUS_PENDING_USER_ACTION→ 시스템 설치 UI 자동 표시 (확인 1회)
     */
    private fun downloadAndInstallApk(apkUrl: String) {
        val serverUrl = config.serverUrl ?: return
        val fullUrl = if (apkUrl.startsWith("http")) apkUrl
                      else serverUrl.trimEnd('/') + apkUrl

        scope.launch {
            try {
                onStatus("업데이트 다운로드 중…")
                Log.i(TAG, "APK 다운로드: $fullUrl")

                val apkFile = withContext(Dispatchers.IO) {
                    // 외부 저장소 사용 — 패키지 인스톨러가 접근 가능한 위치
                    val dir = context.getExternalCacheDir() ?: context.cacheDir
                    val tmp = File(dir, "update.apk")
                    val request = Request.Builder().url(fullUrl).build()
                    ApiClient.http().newCall(request).execute().use { resp ->
                        if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
                        val body = resp.body ?: throw Exception("빈 응답")
                        tmp.outputStream().use { out -> body.byteStream().copyTo(out) }
                    }
                    tmp.setReadable(true, false)
                    tmp
                }

                Log.i(TAG, "다운로드 완료: ${apkFile.length()} bytes")
                onStatus("업데이트 설치 중…")

                // 1순위: pm install -r (일부 STB ROM에서 무확인)
                val pmOk = withContext(Dispatchers.IO) { trySilentInstallPm(apkFile) }
                if (pmOk) {
                    Log.i(TAG, "pm install 자동 설치 완료")
                    onStatus("업데이트 완료 — 재시작 중…")
                    return@launch
                }

                // 2순위: ACTION_VIEW — 시스템 설치 다이얼로그 (API 22에서 안정적)
                Log.i(TAG, "ACTION_VIEW 설치 시도")
                withContext(Dispatchers.Main) { installViaActionView(apkFile) }

            } catch (e: Exception) {
                Log.e(TAG, "업데이트 실패", e)
                onStatus("업데이트 실패: ${e.message}")
            }
        }
    }

    /** pm install -r 시도. exitCode==0 && "Success" 포함 시 true. */
    private fun trySilentInstallPm(apkFile: File): Boolean {
        return try {
            val proc = Runtime.getRuntime()
                .exec(arrayOf("pm", "install", "-r", apkFile.absolutePath))
            val exitCode = proc.waitFor()
            val output = proc.inputStream.bufferedReader().readText()
            val error  = proc.errorStream.bufferedReader().readText()
            Log.i(TAG, "pm install: exitCode=$exitCode out=$output err=$error")
            exitCode == 0 && output.contains("Success", ignoreCase = true)
        } catch (e: Exception) {
            Log.w(TAG, "pm install 실행 불가: ${e.message}")
            false
        }
    }

    /** ACTION_VIEW로 시스템 설치 다이얼로그 호출 (Android 5.1.1 / API 22 호환). */
    private fun installViaActionView(apkFile: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            onStatus("설치 화면을 확인하세요")
        } catch (e: Exception) {
            Log.e(TAG, "ACTION_VIEW 설치 실패", e)
            onStatus("설치 실패: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun startLoop() {
        loopJob?.cancel()
        loopJob = scope.launch {
            while (isActive) {
                val eng = engine
                val slot = eng?.current()
                if (slot == null) { delay(1_000); continue }

                val file = cache.cachedFile(slot.item.media)
                if (file != null) {
                    renderer.show(slot.item, file)
                } else {
                    Log.w(TAG, "캐시에 없음: ${slot.item.media.filename}")
                }

                val waitMs = (slot.nextSlotEpochMs - ntp.now()).coerceAtLeast(200L)
                delay(waitMs)
            }
        }
    }

    private fun startHeartbeat(serverUrl: String, deviceId: String, secret: String) {
        val host = URI(serverUrl).host ?: return
        heartbeat?.stop()
        val versionName = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        }.getOrDefault("unknown")
        heartbeat = HeartbeatService(
            serverHost = host,
            deviceId = deviceId,
            deviceSecret = secret,
            metrics = metrics,
            appVersion = versionName
        ).also { it.start() }
    }

    private fun startControlChannel(serverUrl: String, deviceId: String) {
        control?.stop()
        control = ControlChannel(
            serverUrl = serverUrl,
            selfDeviceId = deviceId,
            onPlaylistUpdated = { refreshPlaylist() },
            onAssignmentChanged = { refreshPlaylist() },
            onUpdateApk = { apkUrl -> downloadAndInstallApk(apkUrl) },
            // 서버 재연결 시 최신 playlist + 스케줄 재조회
            onReconnected = { refreshPlaylist() },
            // 스케줄 변경 시 기기 정보 재조회 → scheduleManager.update() 호출
            onScheduleChanged = { refreshPlaylist() }
        ).also { it.start() }
    }

    fun stop() {
        loopJob?.cancel()
        heartbeat?.stop()
        control?.stop()
        scheduleManager.stop()
        renderer.release()
        scope.coroutineContext[Job]?.cancel()
    }

    companion object {
        private const val TAG = "PlayerCoordinator"
        private const val DEBUG_OVERLAY = true
        private const val RESYNC_INTERVAL_MS = 5 * 60 * 1000L
    }
}
