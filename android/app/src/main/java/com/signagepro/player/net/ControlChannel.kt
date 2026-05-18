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
    private val onAssignmentChanged: () -> Unit
) {
    private var socket: Socket? = null

    fun start() {
        val opts = IO.Options().apply {
            reconnection = true
            reconnectionDelay = 1_000
            reconnectionDelayMax = 30_000
            timeout = 10_000
        }
        socket = IO.socket(serverUrl, opts).apply {
            on(Socket.EVENT_CONNECT) {
                Log.i(TAG, "Socket.io 연결됨")
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
                // 자기 기기 변경이거나 broadcast(데이터 없음)이면 재조회
                if (changedId.isNullOrBlank() || changedId == selfDeviceId) {
                    Log.i(TAG, "group_assignment_changed")
                    onAssignmentChanged()
                }
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
