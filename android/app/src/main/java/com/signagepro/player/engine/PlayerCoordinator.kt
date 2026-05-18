package com.signagepro.player.engine

import android.content.Context
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
import java.net.URI

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
 */
class PlayerCoordinator(
    private val context: Context,
    private val config: ConfigStore,
    private val renderer: MediaRenderer,
    private val onStatus: (String) -> Unit
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

        onStatus("NTP 동기 중…")
        ntp.sync()

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

        // 네트워크 모듈은 playlist 결과와 무관하게 시작
        startHeartbeat(serverUrl, deviceId, secret)
        startControlChannel(serverUrl, deviceId)
    }

    private suspend fun fetchPlaylistOrCached(serverUrl: String, deviceId: String): PlaylistDto? {
        return try {
            val api = ApiClient.get(serverUrl)
            val device = api.getDevice(deviceId)
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

    /** playlist_updated/group_assignment_changed 수신 시 재조회 + 재시작. */
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
            }
        }
    }

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
        heartbeat = HeartbeatService(
            serverHost = host,
            deviceId = deviceId,
            deviceSecret = secret,
            metrics = metrics
        ).also { it.start() }
    }

    private fun startControlChannel(serverUrl: String, deviceId: String) {
        control?.stop()
        control = ControlChannel(
            serverUrl = serverUrl,
            selfDeviceId = deviceId,
            onPlaylistUpdated = { refreshPlaylist() },
            onAssignmentChanged = { refreshPlaylist() }
        ).also { it.start() }
    }

    fun stop() {
        loopJob?.cancel()
        heartbeat?.stop()
        control?.stop()
        renderer.release()
        scope.coroutineContext[Job]?.cancel()
    }

    companion object {
        private const val TAG = "PlayerCoordinator"
    }
}
