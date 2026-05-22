package com.signagepro.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.signagepro.player.config.ConfigStore
import org.json.JSONObject
import java.io.File

/**
 * USB 마운트 시 signagepro.json 을 읽어 자동 실행.
 *
 * USB 루트에 signagepro.json 을 넣고 꽂으면:
 * - 파일/폴더 삭제   (delete_dir)
 * - 설정 변경        (update_config)
 * - APK 설치         (install_apk)
 * - ADB TCP 활성화   (enable_adb_tcp) ← 원격 배포를 위한 1회 설정
 *
 * 예시 signagepro.json:
 * {
 *   "actions": [
 *     { "type": "install_apk",   "filename": "app-debug.apk" },
 *     { "type": "enable_adb_tcp" }
 *   ]
 * }
 */
class UsbReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MEDIA_MOUNTED) return

        // intent.data 로 마운트 경로 획득, 없으면 알려진 경로 순서대로 탐색
        val mountPath = intent.data?.path
        val candidates = buildList {
            if (mountPath != null) add(mountPath)
            addAll(KNOWN_USB_PATHS)
        }

        val usbRoot = candidates.map { File(it) }.firstOrNull { it.exists() && it.isDirectory }
        if (usbRoot == null) {
            Log.w(TAG, "USB 마운트 경로를 찾을 수 없습니다")
            return
        }
        Log.i(TAG, "USB 마운트 감지: $usbRoot")

        val actionFile = File(usbRoot, ACTION_FILENAME)
        if (!actionFile.exists()) {
            Log.i(TAG, "$ACTION_FILENAME 없음 — 자동 실행 건너뜀")
            return
        }

        Log.i(TAG, "$ACTION_FILENAME 발견 — 실행 시작")
        toast(context, "USB 액션 실행 중...")

        try {
            val json = JSONObject(actionFile.readText())
            val actions = json.getJSONArray("actions")
            val results = mutableListOf<String>()

            for (i in 0 until actions.length()) {
                val result = executeAction(context, actions.getJSONObject(i), usbRoot)
                results.add(result)
                Log.i(TAG, "액션 결과: $result")
            }

            toast(context, "완료: ${results.joinToString(" / ")}")
        } catch (e: Exception) {
            Log.e(TAG, "액션 파일 처리 실패", e)
            toast(context, "오류: ${e.message}")
        }
    }

    private fun executeAction(context: Context, action: JSONObject, usbRoot: File): String {
        return when (val type = action.getString("type")) {

            // ── 파일/폴더 삭제 ──────────────────────────────────────────────
            "delete_dir" -> {
                val path = action.getString("path")
                val target = File(path)
                return if (!target.exists()) {
                    "skip(없음): $path"
                } else if (target.deleteRecursively()) {
                    "삭제: $path"
                } else {
                    "삭제실패(권한부족): $path"
                }
            }

            // ── 설정 변경 (deviceId / serverUrl / deviceSecret) ─────────────
            "update_config" -> {
                val config = ConfigStore(context)
                val changed = mutableListOf<String>()
                action.optString("deviceId").takeIf { it.isNotBlank() }?.let {
                    config.deviceId = it; changed.add("deviceId=$it")
                }
                action.optString("serverUrl").takeIf { it.isNotBlank() }?.let {
                    config.serverUrl = it; changed.add("serverUrl=$it")
                }
                action.optString("deviceSecret").takeIf { it.isNotBlank() }?.let {
                    config.deviceSecret = it; changed.add("secret=***")
                }
                if (changed.isNotEmpty()) {
                    // 설정 변경 후 앱 재시작 (새 설정 적용)
                    val restart = context.packageManager
                        .getLaunchIntentForPackage(context.packageName)
                        ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }
                    if (restart != null) context.startActivity(restart)
                    "설정변경: ${changed.joinToString()}"
                } else {
                    "설정변경: 변경 항목 없음"
                }
            }

            // ── APK 설치 ────────────────────────────────────────────────────
            // 1순위: pm install -r (자동, UI 없음)
            // 2순위: ACTION_VIEW (설치 다이얼로그)
            "install_apk" -> {
                val filename = action.getString("filename")
                val apk = File(usbRoot, filename)
                if (!apk.exists()) return "APK없음: $filename"

                apk.setReadable(true, false) // 패키지 인스톨러 접근 허용
                val silentOk = trySilentInstall(apk)
                if (silentOk) return "APK자동설치: $filename"

                // 자동 설치 실패 → 다이얼로그
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(installIntent)
                "APK설치요청: $filename"
            }

            // ── ADB TCP 모드 활성화 (포트 5555) ─────────────────────────────
            // 이후 PC에서 "adb connect <기기IP>:5555" 로 USB 없이 원격 접속 가능.
            // RK3229 Android 5.1.1 에서는 setprop + stop/start adbd 조합으로 활성화.
            "enable_adb_tcp" -> {
                return try {
                    Runtime.getRuntime().exec("setprop service.adb.tcp.port 5555").waitFor()
                    Runtime.getRuntime().exec("stop adbd").waitFor()
                    Thread.sleep(500)
                    Runtime.getRuntime().exec("start adbd").waitFor()
                    Log.i(TAG, "ADB TCP 5555 활성화 완료")
                    "ADB TCP 활성화(5555)"
                } catch (e: Exception) {
                    Log.w(TAG, "ADB TCP 활성화 실패: ${e.message}")
                    "ADB TCP 활성화 실패: ${e.message}"
                }
            }

            else -> "알수없는액션: $type"
        }
    }

    private fun trySilentInstall(apkFile: File): Boolean {
        return try {
            val process = Runtime.getRuntime()
                .exec(arrayOf("pm", "install", "-r", apkFile.absolutePath))
            val exitCode = process.waitFor()
            val output = process.inputStream.bufferedReader().readText()
            Log.i(TAG, "pm install: exitCode=$exitCode out=$output")
            exitCode == 0 && output.contains("Success", ignoreCase = true)
        } catch (e: Exception) {
            Log.w(TAG, "pm install 불가: ${e.message}")
            false
        }
    }

    private fun toast(context: Context, msg: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(context, "[SignagePro USB] $msg", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val TAG = "UsbReceiver"
        const val ACTION_FILENAME = "signagepro.json"

        // RK3229 Android 5.1.1 에서 알려진 USB 마운트 경로
        private val KNOWN_USB_PATHS = listOf(
            "/storage/udisk0",
            "/storage/usb_storage",
            "/mnt/usb_storage",
            "/storage/external_storage/sda1",
            "/mnt/media_rw/udisk0",
        )
    }
}
