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
    private val onReconnected: () -> Unit = {},
    private val onScheduleChanged: () -> Unit = {},
    /** 볼륨 설정 명령 (0~15) */
    private val onSetVolume: (level: Int) -> Unit = {},
    /** 재부팅 직전 검은 화면 준비 */
    private val onPrepareReboot: () -> Unit = {}
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
                // 서버에 기기 ID 등록 → 개별 명령(볼륨 등) 수신용 룸 입장
                val reg = JSONObject().put("deviceId", selfDeviceId)
                socket?.emit("register_device", reg)
                if (wasConnected) {
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
            on("set_volume") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                val target = data.optString("deviceId", "")
                if (target.isBlank() || target == selfDeviceId) {
                    val level = data.optInt("level", -1)
                    if (level in 0..15) {
                        Log.i(TAG, "볼륨 설정 수신: $level")
                        onSetVolume(level)
                    }
                }
            }
            on("prepare_reboot") { args ->
                val data = args.firstOrNull() as? JSONObject
                val target = data?.optString("deviceId", "")
                if (target.isNullOrBlank() || target == selfDeviceId) {
                    Log.i(TAG, "재부팅 준비 명령 수신 → 검은 화면")
                    onPrepareReboot()
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
