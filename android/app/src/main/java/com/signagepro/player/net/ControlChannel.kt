package com.signagepro.player.net

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

/**
 * Socket.io 클라이언트 — 서버의 실시간 이벤트 구독.
 *
 * 구독 이벤트:
 *   - playlist_updated {groupId}        → 해당 그룹의 player가 playlist 재조회
 *   - group_assignment_changed {deviceId, groupId, storeId}
 *                                       → 자기 기기에 해당하면 재조회
 *
 * 재접속/하트비트는 socket.io 클라이언트가 자체 관리.
 */
class ControlChannel(
    private val serverUrl: String,
    private val selfDeviceId: String,
    private val onPlaylistUpdated: (groupId: String) -> Unit,
    private val onAssignmentChanged: () -> Unit,
    private val onUpdateApk: (apkUrl: String) -> Unit = {},
    /**
     * 서버 재연결 시 호출 — 서버 다운 중 콘텐츠가 바뀌었을 수 있으므로
     * 재연결되면 최신 playlist를 즉시 재조회한다.
     */
    private val onReconnected: () -> Unit = {},
    /** 스케줄 변경 알림 — 기기 정보 재조회하여 최신 스케줄 적용 */
    private val onScheduleChanged: () -> Unit = {}
) {
    private var socket: Socket? = null
    @Volatile private var wasConnected = false

    fun start() {
        val opts = IO.Options().apply {
            reconnection = true
            reconnectionDelay = 1_000
            reconnectionDelayMax = 30_000
            timeout = 10_000
            transports = arrayOf("websocket") // polling 건너뛰고 WebSocket 직접 사용
        }
        socket = IO.socket(serverUrl, opts).apply {
            on(Socket.EVENT_CONNECT) {
                if (wasConnected) {
                    // 재연결 — 다운 중 변경된 콘텐츠 반영
                    Log.i(TAG, "Socket.io 재연결됨 → playlist 재조회")
                    onReconnected()
                } else {
                    Log.i(TAG, "Socket.io 최초 연결됨")
                }
                wasConnected = true
            }
            on(Socket.EVENT_DISCONNECT) {
                Log.w(TAG, "Socket.io 끊김")
            }
            on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.w(TAG, "Socket.io 연결 오류: ${args.firstOrNull()}")
            }
            on("playlist_updated") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                val groupId = data.optString("groupId", "")
                if (groupId.isNotBlank()) {
                    Log.i(TAG, "playlist_updated: $groupId")
                    onPlaylistUpdated(groupId)
                }
            }
            on("group_assignment_changed") { args ->
                val data = args.firstOrNull() as? JSONObject
                val changedId = data?.optString("deviceId", "")
                if (changedId.isNullOrBlank() || changedId == selfDeviceId) {
                    Log.i(TAG, "group_assignment_changed")
                    onAssignmentChanged()
                }
            }
            on("update_apk") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                // targetDeviceId 없으면 전체 배포, 있으면 해당 기기만
                val target = data.optString("deviceId", "")
                if (target.isBlank() || target == selfDeviceId) {
                    val apkUrl = data.optString("url", "")
                    if (apkUrl.isNotBlank()) {
                        Log.i(TAG, "OTA 업데이트 수신: $apkUrl")
                        onUpdateApk(apkUrl)
                    }
                }
            }
            on("screen_schedule") { _ ->
                Log.i(TAG, "screen_schedule 이벤트 수신 → 스케줄 재조회")
                onScheduleChanged()
            }
            connect()
        }
    }

    fun stop() {
        socket?.disconnect()
        socket?.off()
        socket?.close()
        socket = null
    }

    companion object {
        private const val TAG = "ControlChannel"
    }
}
