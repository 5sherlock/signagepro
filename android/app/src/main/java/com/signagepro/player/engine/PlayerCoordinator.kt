package com.signagepro.player.engine

import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.media.AudioManager
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Build
import android.util.Log
import com.signagepro.player.SignageDeviceAdmin
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
    private val ntp = NtpClient(context)
    private val cache = MediaCacheRepo(context)
    private val store = PlaylistStore(context)
    private val metrics = SystemMetrics(context)
    private val refreshMutex = Mutex()

    private var engine: PlaylistEngine? = null
    private var loopJob: Job? = null
    private var heartbeat: HeartbeatService? = null
    private var control: ControlChannel? = null
    private val scheduleManager = ScreenScheduleManager(context)

    /** 다운로드 진행 상태 — heartbeat에 포함. "cur/total/pct" 형식 */
    @Volatile private var dlStatus: String? = null

    /**
     * 화면 켜짐/꺼짐 상태 — heartbeat에 포함.
     * true = 화면 켜짐 (기본값), false = 스케줄에 의해 화면 꺼짐.
     */
    @Volatile private var screenOn: Boolean = true

    /**
     * 현재 재생 중인 슬라이드 정보 — heartbeat에 포함.
     * 형식: "<index>|<total>|<filename>" (index 1-based, '|' 구분자)
     */
    @Volatile private var currentSlideInfo: String? = null

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    /** Visualizer — 전역 오디오 출력 레벨 측정 (session 0 = global mix) */
    private var visualizer: Visualizer? = null

    private fun initVisualizer() {
        try {
            visualizer?.release()
            visualizer = Visualizer(0).apply {   // 0 = 전역 출력 믹스
                captureSize = Visualizer.getCaptureSizeRange()[0]
                measurementMode = Visualizer.MEASUREMENT_MODE_PEAK_RMS
                enabled = true
            }
            Log.i(TAG, "Visualizer 초기화 완료 (글로벌 믹스)")
        } catch (e: Exception) {
            Log.w(TAG, "Visualizer 초기화 실패 — VU 비활성화: ${e.message}")
            visualizer = null
        }
    }

    /**
     * 실제 오디오 출력 레벨 (0~100).
     * mRms: 0.01 dBFS 단위, -9600(무음) ~ 0(풀스케일).
     * -60dB ~ 0dB 구간을 0 ~ 100으로 선형 매핑.
     */
    private fun getVuLevel(): Int {
        val v = visualizer ?: return 0
        return try {
            val msr = Visualizer.MeasurementPeakRms()
            if (v.getMeasurementPeakRms(msr) == Visualizer.SUCCESS) {
                val db = msr.mRms / 100.0          // -96.0 ~ 0.0 dB
                ((db + 60.0) / 60.0 * 100).toInt().coerceIn(0, 100)
            } else 0
        } catch (e: Exception) { 0 }
    }

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

        // 캐시가 있으면 서버 응답 기다리지 않고 즉시 재생 시작
        val cached = store.load()
        if (cached != null && cached.medias.isNotEmpty()) {
            engine!!.setPlaylist(cached.medias)
            startLoop()
            onStatus("")
            Log.i(TAG, "캐시로 즉시 재생 시작 (${cached.medias.size}개 항목)")
        } else {
            onStatus("서버 연결 중…")
        }

        // 서버 업데이트는 백그라운드에서 처리
        scope.launch {
            try {
                val fresh = fetchPlaylistOrCached(serverUrl, deviceId)
                if (fresh != null && fresh.medias.isNotEmpty()) {
                    prefetchAll(serverUrl, fresh)
                    engine?.setPlaylist(fresh.medias)
                    if (loopJob == null || loopJob?.isActive != true) startLoop()
                    onStatus("")
                    Log.i(TAG, "서버에서 playlist 갱신 완료 (${fresh.medias.size}개 항목)")
                } else if (loopJob?.isActive != true) {
                    onStatus("재생 가능한 미디어가 없습니다.")
                }
                // 부팅 시 스케줄 즉시 평가 — 다음 분 체크를 기다리지 않고 OFF 구간이면 화면 끄기
                scheduleManager.evaluateNow()
            } catch (e: Exception) {
                Log.w(TAG, "서버 갱신 실패 — 캐시로 계속 재생", e)
            }
        }

        initVisualizer()
        // 화면 ON/OFF 시 screenOn 상태 업데이트 → heartbeat에 포함
        scheduleManager.onScreenStateChange = { on -> screenOn = on }
        startHeartbeat(serverUrl, deviceId, secret)
        startControlChannel(serverUrl, deviceId)
        scheduleManager.start(scope)
        if (DEBUG_OVERLAY) startDebugLoop(deviceId)
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
        // 중복 hash 제거 — 같은 파일을 여러 기기에 배치해도 한 번만 다운로드
        val uniqueItems = playlist.medias.distinctBy { it.media.hash ?: it.media.id }
        val total = uniqueItems.size
        val activeHashes = mutableSetOf<String>()

        try {
            uniqueItems.forEachIndexed { idx, item ->
                item.media.hash?.let { activeHashes.add(it) }

                // 이미 캐시된 파일은 건너뜀
                if (cache.cachedFile(item.media) != null) {
                    onStatus("확인 중… (${idx + 1}/$total)  ${item.media.filename}")
                    return@forEachIndexed
                }

                try {
                    cache.ensure(serverUrl, item.media) { pct ->
                        dlStatus = "${idx + 1}/$total/$pct"
                        onStatus("다운로드 (${idx + 1}/$total)  ${item.media.filename}\n$pct%")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "미디어 다운로드 실패: ${item.media.filename}", e)
                    onStatus("다운로드 실패: ${item.media.filename}")
                }
            }
        } finally {
            dlStatus = null
            cache.trim(activeHashes)
        }
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
                // 항상 루프 재시작 — 이전 슬롯의 delay()를 취소하고 즉시 새 콘텐츠 적용.
                // (기존 if 분기는 OLD 슬롯의 긴 delay 동안 새 playlist가 반영되지 않아
                //  화면이 잠깐 갱신됐다가 다시 블랙으로 보이는 문제를 유발했음)
                startLoop()
                onStatus("")   // 재생 시작 → 상태 문구 제거
                // 스케줄 변경/재연결 시 현재 시각 기준으로 화면 상태 즉시 반영
                scheduleManager.evaluateNow()
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
                        val contentLength = body.contentLength()
                        val buf = ByteArray(32 * 1024)
                        var downloaded = 0L
                        var lastPct = -1
                        tmp.outputStream().use { out ->
                            body.byteStream().use { input ->
                                while (true) {
                                    val n = input.read(buf)
                                    if (n <= 0) break
                                    out.write(buf, 0, n)
                                    downloaded += n
                                    if (contentLength > 0) {
                                        val pct = (downloaded * 100L / contentLength).toInt().coerceIn(0, 99)
                                        if (pct >= lastPct + 5) {
                                            lastPct = pct
                                            // cur=0/total=0 → APK 업데이트 마커 (미디어와 구분)
                                            dlStatus = "0/0/$pct"
                                        }
                                    }
                                }
                            }
                        }
                    }
                    tmp.setReadable(true, false)
                    tmp
                }

                Log.i(TAG, "다운로드 완료: ${apkFile.length()} bytes")
                onStatus("업데이트 설치 중…")

                // 1순위: Device Owner PackageInstaller — 확인창 없이 자동 설치
                val pkgOk = withContext(Dispatchers.IO) { installViaPackageInstaller(apkFile) }
                if (pkgOk) {
                    Log.i(TAG, "PackageInstaller(Device Owner) 자동 설치 완료")
                    onStatus("업데이트 완료 — 재시작 중…")
                    return@launch
                }

                // 2순위: su pm install -r (rooted 기기)
                val pmOk = withContext(Dispatchers.IO) { trySilentInstallPm(apkFile) }
                if (pmOk) {
                    Log.i(TAG, "pm install 자동 설치 완료 -> 3초 후 자가 종료 및 재시작")
                    onStatus("업데이트 완료 — 재시작 중…")
                    kotlinx.coroutines.delay(3000)
                    android.os.Process.killProcess(android.os.Process.myPid())
                    return@launch
                }

                // 3순위: ACTION_VIEW — 시스템 설치 다이얼로그 (최후 수단)
                Log.i(TAG, "ACTION_VIEW 설치 시도")
                withContext(Dispatchers.Main) { installViaActionView(apkFile) }

            } catch (e: Exception) {
                Log.e(TAG, "업데이트 실패", e)
                onStatus("업데이트 실패: ${e.message}")
            } finally {
                dlStatus = null
            }
        }
    }

    /**
     * Device Owner 권한으로 PackageInstaller를 통해 무확인 자동 설치.
     * Device Owner가 아니면 즉시 false 반환.
     * 성공 시 앱이 자동으로 교체되고 재시작됨.
     */
    private fun installViaPackageInstaller(apkFile: File): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (!dpm.isDeviceOwnerApp(context.packageName)) {
            Log.i(TAG, "Device Owner 아님 — PackageInstaller 건너뜀")
            return false
        }
        return try {
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            val sessionId = installer.createSession(params)
            installer.openSession(sessionId).use { session ->
                session.openWrite("package", 0, apkFile.length()).use { out ->
                    apkFile.inputStream().copyTo(out)
                    session.fsync(out)
                }
                val intent = Intent("com.signagepro.player.INSTALL_COMPLETE")
                val flags = if (Build.VERSION.SDK_INT >= 23)
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                else PendingIntent.FLAG_UPDATE_CURRENT
                val pi = PendingIntent.getBroadcast(context, 0, intent, flags)
                session.commit(pi.intentSender)
            }
            Log.i(TAG, "PackageInstaller 세션 커밋 완료 (Device Owner)")
            true
        } catch (e: Exception) {
            Log.w(TAG, "PackageInstaller 실패: ${e.message}")
            false
        }
    }

    /**
     * pm install -r 시도.
     * 1순위: su root로 실행 (크라이저 STB 등 rooted 기기 — 확인 다이얼로그 없음)
     * 2순위: 일반 pm install (일부 ROM에서 허용)
     * exitCode==0 && "Success" 포함 시 true.
     */
    private fun trySilentInstallPm(apkFile: File): Boolean {
        // 1순위: root(su) 경유 — 설치 확인 다이얼로그 없이 자동 설치
        try {
            val proc = Runtime.getRuntime().exec(arrayOf(
                "su", "-c", "cp \"${apkFile.absolutePath}\" /data/local/tmp/update.apk && chmod 777 /data/local/tmp/update.apk && pm install -r /data/local/tmp/update.apk && rm /data/local/tmp/update.apk && sleep 2 && am start -n com.signagepro.player/.MainActivity"
            ))
            val exitCode = proc.waitFor()
            val output = proc.inputStream.bufferedReader().readText()
            val error  = proc.errorStream.bufferedReader().readText()
            Log.i(TAG, "su pm install: exitCode=$exitCode out=$output err=$error")
            if (exitCode == 0 && output.contains("Success", ignoreCase = true)) return true
        } catch (e: Exception) {
            Log.w(TAG, "su pm install 실행 불가: ${e.message}")
        }

        // 2순위: 일반 pm install (root 없는 일부 ROM 허용)
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf(
                "sh", "-c", "pm install -r \"${apkFile.absolutePath}\" && sleep 2 && am start -n com.signagepro.player/.MainActivity"
            ))
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
                var animMs = 0L
                if (file != null) {
                    // preloadImage는 이전 대기 중에 백그라운드로 완료됨.
                    // 미리 로드되지 않은 경우 MediaRenderer 내부 폴백(Main 스레드 디코딩)으로 처리.
                    animMs = renderer.show(slot.item, file)
                    currentSlideInfo = "${slot.index + 1}|${slot.total}|${slot.item.media.filename}"
                } else {
                    Log.w(TAG, "캐시에 없음: ${slot.item.media.filename}")
                }

                // ── 다음 슬라이드 선제 로드 ────────────────────────────────────────
                // show() 직후 대기 시간(평균 8~9초) 동안 다음 슬라이드를 IO 스레드에서 미리
                // 디코딩. waitMs 만료 시 renderer.show()가 IO 없이 즉시 실행되어
                // 기기 간 전환 시작 시각이 수십 ms 이내로 일치함.
                eng.nextItem()?.let { nextItem ->
                    cache.cachedFile(nextItem.media)?.let { nextFile ->
                        scope.launch(Dispatchers.IO) {
                            renderer.preloadImage(nextItem, nextFile)
                        }
                    }
                }

                // 최소 대기 = 애니메이션 시간 + 100ms 스케줄러 지연 여유.
                // (IO 선제 로드로 200ms 버퍼 불필요 → 100ms로 단축)
                val waitMs = (slot.nextSlotEpochMs - ntp.now()).coerceAtLeast(animMs + 100L)
                delay(waitMs)
            }
        }
    }

    private fun startHeartbeat(serverUrl: String, deviceId: String, secret: String) {
        val host = URI(serverUrl).host ?: return
        heartbeat?.stop()
        val versionName = runCatching {
            val name = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
            "$name (${com.signagepro.player.BuildConfig.BUILD_DATE})"
        }.getOrDefault("unknown")
        heartbeat = HeartbeatService(
            serverHost = host,
            deviceId = deviceId,
            deviceSecret = secret,
            metrics = metrics,
            appVersion = versionName,
            dlStatusProvider = { dlStatus },
            slideProvider = { currentSlideInfo },
            volumeProvider = {
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (max > 0) (cur.toFloat() / max * 15).toInt() else null
            },
            vuProvider = { getVuLevel() },
            timeProvider = { ntp.now() },
            screenStateProvider = { if (screenOn) "on" else "off" },
            onServerEpoch = { epochMs, sentAt -> ntp.syncFromHeartbeatAck(epochMs, sentAt) }
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
            onReconnected = { refreshPlaylist() },
            onScheduleChanged = { refreshPlaylist() },
            onSetVolume = { level ->
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val scaled = (level.toFloat() / 15 * max).toInt().coerceIn(0, max)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, scaled, 0)
                Log.i(TAG, "볼륨 설정: $level/15 → $scaled/$max")
            },
            onPrepareReboot = {
                // ADB reboot 전에 루프 중단 + 검은 화면 → 일그러짐 방지
                loopJob?.cancel()
                scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    renderer.showBlack()
                }
            },
            onRestartApp = {
                Log.i(TAG, "원격 앱 재시작 소켓 수신 ➔ 자가 재시작 실행")
                performSelfRestart()
            },
            onRebootDevice = {
                Log.i(TAG, "물리 기기 재부팅 수행")
                
                // 물리 재부팅이 실패(권한 부족 등)하여 프로세스가 계속 살아있는 경우를 대비한 Fail-Safe 비상 복구 타이머 가동
                // 4초 뒤에도 앱이 죽지 않고 살아있다면 자가 앱 재시작을 강제로 구동해 검은 화면 굳어짐을 예방합니다.
                scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    kotlinx.coroutines.delay(4000)
                    Log.w(TAG, "물리 재부팅 실패 감지 (4초 경과) ➔ 자가 앱 재시작으로 비상 복구!")
                    performSelfRestart()
                }

                // 1순위: Device Owner DevicePolicyManager 활용 (API 24+)
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val adminComponent = ComponentName(context, SignageDeviceAdmin::class.java)
                if (Build.VERSION.SDK_INT >= 24 && dpm.isDeviceOwnerApp(context.packageName)) {
                    try {
                        Log.i(TAG, "Device Owner 권한으로 reboot() API 호출")
                        dpm.reboot(adminComponent)
                        return@ControlChannel
                    } catch (e: Exception) {
                        Log.e(TAG, "DPM.reboot() API 실패, su reboot 시도: ${e.message}")
                    }
                }
                
                // 2순위: su -c reboot (Rooted 기기 폴백)
                try {
                    Log.i(TAG, "su -c reboot 쉘 명령어 호출")
                    Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot"))
                } catch (e: Exception) {
                    Log.e(TAG, "su reboot 쉘 실행 실패: ${e.message}")
                }
            }
        ).also { it.start() }
    }

    private fun performSelfRestart() {
        Log.i(TAG, "자가 복구 앱 자체 재시작 수행 (AlarmManager)")
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            if (intent != null) {
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    9999,
                    intent,
                    if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    else PendingIntent.FLAG_CANCEL_CURRENT
                )
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                val triggerTime = System.currentTimeMillis() + 800
                if (Build.VERSION.SDK_INT >= 19) {
                    alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
                Log.i(TAG, "자가 재시작 알람 등록 완료 (800ms 후 실행 예약)")
            }
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(0)
        } catch (e: Exception) {
            Log.e(TAG, "AlarmManager 자가 앱 재시작 예약 실패: ${e.message}")
        }
    }

    fun stop() {
        loopJob?.cancel()
        heartbeat?.stop()
        control?.stop()
        scheduleManager.stop()
        renderer.release()
        visualizer?.release()
        visualizer = null
        scope.coroutineContext[Job]?.cancel()
    }

    companion object {
        private const val TAG = "PlayerCoordinator"
        private const val DEBUG_OVERLAY = true
    }
}
