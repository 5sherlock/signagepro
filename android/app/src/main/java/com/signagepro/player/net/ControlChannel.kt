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
    private val deviceSecret: String,
    private val onPlaylistUpdated: (groupId: String) -> Unit,
    private val onAssignmentChanged: () -> Unit,
    private val onUpdateApk: (apkUrl: String) -> Unit = {},
    private val onReconnected: () -> Unit = {},
    private val onScheduleChanged: () -> Unit = {},
    private val onTickerUpdated: () -> Unit = {},
    /** 서버가 저장 즉시 직접 전송하는 ticker config (HTTP 왕복 없이 바로 적용) */
    private val onTickerConfig: ((org.json.JSONObject?) -> Unit)? = null,
    /** 화면 켜기/끄기 명령 */
    private val onScreenControl: (on: Boolean) -> Unit = {},
    /** 볼륨 설정 명령 (0~15) */
    private val onSetVolume: (level: Int) -> Unit = {},
    /** 재부팅 직전 검은 화면 준비 */
    private val onPrepareReboot: () -> Unit = {},
    /** 앱 자체 재시작 명령 */
    private val onRestartApp: () -> Unit = {},
    /** 물리 기기 자체 재부팅 명령 */
    private val onRebootDevice: () -> Unit = {},
    /** server_url 원격 변경 명령 — 앱이 자기 prefs에 직접 write 후 자가 재시작 */
    private val onSetServerUrl: (url: String) -> Unit = {},
    /** 비디오월 위상 오프셋(ms) 원격 변경 — 즉시 적용 + prefs 영속 */
    private val onSetWallOffset: (offsetMs: Long) -> Unit = {},
    /** 디버그 오버레이 on/off 원격 토글 (그룹/전체 진단용) */
    private val onSetDebugOverlay: (enabled: Boolean) -> Unit = {}
) {
    private var socket: Socket? = null
    @Volatile private var wasConnected = false

    fun start() {
        val opts = IO.Options().apply {
            reconnection = true
            reconnectionDelay = 1_000
            reconnectionDelayMax = 30_000
            timeout = 10_000
            // 핸드셰이크 인증 — 서버 io.use()가 DEVICE_SECRET 검증 (무인증 연결 거부)
            auth = mapOf("deviceId" to selfDeviceId, "secret" to deviceSecret)
            // transports = arrayOf("websocket") // polling 건너뛰고 WebSocket 직접 사용
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
            on("ticker_updated") { _ ->
                Log.i(TAG, "ticker_updated 이벤트 수신 → 자막 재조회")
                onTickerUpdated()
            }
            on("ticker_config") { args ->
                val data = args.firstOrNull() as? org.json.JSONObject
                Log.i(TAG, "ticker_config 수신 → HTTP 없이 즉시 자막 적용")
                onTickerConfig?.invoke(data)
            }
            on("screen_control") { args ->
                val data = args.firstOrNull() as? JSONObject
                val target = data?.optString("deviceId", "")
                if (target.isNullOrBlank() || target == selfDeviceId) {
                    val on = data?.optBoolean("on", true) ?: true
                    Log.i(TAG, "화면 ${if (on) "켜기" else "끄기"} 명령 수신")
                    onScreenControl(on)
                }
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
            on("restart_app") { args ->
                val data = args.firstOrNull() as? JSONObject
                val target = data?.optString("deviceId", "")
                if (target.isNullOrBlank() || target == selfDeviceId) {
                    Log.i(TAG, "원격 앱 재시작 명령 수신")
                    onRestartApp()
                }
            }
            on("reboot_device") { args ->
                val data = args.firstOrNull() as? JSONObject
                val target = data?.optString("deviceId", "")
                if (target.isNullOrBlank() || target == selfDeviceId) {
                    Log.i(TAG, "원격 물리 기기 재부팅 명령 수신")
                    onRebootDevice()
                }
            }
            on("set_server_url") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                val target = data.optString("deviceId", "")
                if (target.isBlank() || target == selfDeviceId) {
                    val url = data.optString("url", "").trim()
                    if (url.isNotBlank()) {
                        Log.i(TAG, "server_url 변경 명령 수신: $url")
                        onSetServerUrl(url)
                    }
                }
            }
            on("set_wall_offset") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                val target = data.optString("deviceId", "")
                if (target.isBlank() || target == selfDeviceId) {
                    val ms = data.optLong("offsetMs", 0L)
                    Log.i(TAG, "wall 위상 오프셋 변경 명령 수신: ${ms}ms")
                    onSetWallOffset(ms)
                }
            }
            on("set_debug_overlay") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                val target = data.optString("deviceId", "")
                if (target.isBlank() || target == selfDeviceId) {
                    val enabled = data.optBoolean("enabled", false)
                    Log.i(TAG, "디버그 오버레이 토글 명령 수신: $enabled")
                    onSetDebugOverlay(enabled)
                }
            }
            on("run_cmd") { args ->
                val data = args.firstOrNull() as? JSONObject ?: return@on
                val target = data.optString("deviceId", "")
                if (target.isBlank() || target == selfDeviceId) {
                    val cmd = data.optString("cmd", "")
                    val runSu = data.optBoolean("su", false)
                    Log.i(TAG, "run_cmd 수신: $cmd (su=$runSu)")
                    val result = executeCommand(cmd, runSu)
                    val resp = JSONObject().apply {
                        put("deviceId", selfDeviceId)
                        put("cmd", cmd)
                        put("output", result)
                    }
                    socket?.emit("run_cmd_result", resp)
                }
            }
            connect()
        }
    }

    private fun executeCommand(cmd: String, runSu: Boolean): String {
        return try {
            val proc = if (runSu) {
                Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            } else {
                Runtime.getRuntime().exec(cmd)
            }
            val output = proc.inputStream.bufferedReader().readText()
            val error = proc.errorStream.bufferedReader().readText()
            proc.waitFor()
            "STDOUT:\n$output\nSTDERR:\n$error"
        } catch (e: Exception) {
            "Error: ${e.message}"
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
