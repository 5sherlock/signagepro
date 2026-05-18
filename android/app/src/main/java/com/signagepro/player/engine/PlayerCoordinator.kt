package com.signagepro.player.engine

import android.content.Context
import android.util.Log
import com.signagepro.player.api.ApiClient
import com.signagepro.player.api.PlaylistDto
import com.signagepro.player.cache.MediaCacheRepo
import com.signagepro.player.config.ConfigStore
import com.signagepro.player.render.MediaRenderer
import com.signagepro.player.sync.NtpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 부팅 → NTP → device/playlist fetch → prefetch → 슬롯 기반 재생 루프 오케스트레이션.
 *
 * 흐름:
 *   1. NTP 동기 (best-effort, 실패해도 진행)
 *   2. /api/devices/:id → groupId 확인
 *   3. /api/groups/:gid/playlist 받아 PlaylistStore 저장 (실패 시 캐시 사용)
 *   4. 모든 media prefetch (best-effort)
 *   5. 슬롯 루프 — PlaylistEngine.current() → renderer.show() → 다음 슬롯까지 delay
 *
 * 모든 외부 호출은 best-effort. 실패해도 캐시 + NTP fallback으로 재생을 이어감.
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
    private var engine: PlaylistEngine? = null
    private var loopJob: Job? = null

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
        val deviceId = config.deviceId ?: run {
            onStatus("디바이스 ID 미설정")
            return
        }
        val serverUrl = config.serverUrl ?: run {
            onStatus("서버 URL 미설정")
            return
        }

        onStatus("NTP 동기 중…")
        ntp.sync()  // 실패해도 fallback으로 진행

        engine = PlaylistEngine(ntp, deviceId)

        onStatus("playlist 조회 중…")
        val playlist = fetchPlaylistOrCached(serverUrl, deviceId)
        if (playlist == null || playlist.medias.isEmpty()) {
            onStatus("재생 가능한 미디어가 없습니다.")
            return
        }
        engine!!.setPlaylist(playlist.medias)

        onStatus("미디어 다운로드 중…")
        prefetchAll(serverUrl, playlist)

        onStatus("")
        startLoop()
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
            val hash = item.media.hash
            if (hash != null) activeHashes.add(hash)
            try {
                cache.ensure(serverUrl, item.media)
            } catch (e: Exception) {
                Log.w(TAG, "미디어 다운로드 실패: ${item.media.filename}", e)
            }
        }
        cache.trim(activeHashes)
    }

    private fun startLoop() {
        loopJob?.cancel()
        loopJob = scope.launch {
            while (isActive) {
                val eng = engine
                if (eng == null) { delay(1000); continue }
                val slot = eng.current()
                if (slot == null) { delay(1000); continue }

                val file = cache.cachedFile(slot.item.media)
                if (file != null) {
                    renderer.show(slot.item, file)
                } else {
                    // 캐시에 없으면 즉시 다음 슬롯으로 넘김
                    Log.w(TAG, "캐시에 없음: ${slot.item.media.filename}")
                }

                val waitMs = (slot.nextSlotEpochMs - ntp.now()).coerceAtLeast(200L)
                delay(waitMs)
            }
        }
    }

    fun stop() {
        loopJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
        renderer.release()
    }

    companion object {
        private const val TAG = "PlayerCoordinator"
    }
}
