package com.signagepro.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.IOException

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != "android.intent.action.REBOOT" &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        Log.i(TAG, "부팅 또는 패키지 변경 감지 (action=$action)")

        if (action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // 구버전 프로세스가 살아있는 경우 강제 종료 후 재시작.
            // su 루트 쉘 서브프로세스는 am force-stop 으로 죽지 않으므로
            // 3초 후 am start 가 안전하게 실행됨.
            scheduleRestartAndKillLegacy(context)
            return
        }

        enableAdbTcp()
        PlayerForegroundService.start(context)
        val launch = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        context.startActivity(launch)
    }

    /**
     * OTA 설치 완료(ACTION_MY_PACKAGE_REPLACED) 후 구 프로세스 종료 + 신버전 재시작.
     *
     * 일부 RK3229 ROM은 pm install -r 후 기존 앱 프로세스를 자동으로 종료하지 않는다.
     * su 없이 동작하는 방법:
     *  1. AlarmManager 로 2초 후 앱 재기동 예약 (RTC_WAKEUP — 프로세스 kill 후에도 유지됨)
     *     ※ am force-stop 은 AlarmManager 를 취소하지만 Process.killProcess 는 유지함
     *  2. Process.killProcess(myPid()) 로 현재 프로세스(구버전) 즉시 종료
     *  3. 2초 후 AlarmManager 발화 → 신버전 MainActivity 기동
     */
    private fun scheduleRestartAndKillLegacy(context: Context) {
        try {
            enableAdbTcp()

            val pkg = context.packageName
            val launch = context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            } ?: run {
                Log.w(TAG, "LaunchIntent 없음 — 폴백 시작")
                PlayerForegroundService.start(context)
                context.startActivity(Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                })
                return
            }

            val flags = if (android.os.Build.VERSION.SDK_INT >= 23)
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            else android.app.PendingIntent.FLAG_UPDATE_CURRENT

            val pi = android.app.PendingIntent.getActivity(context, 9997, launch, flags)
            val am = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            val triggerAt = System.currentTimeMillis() + 2000L

            if (android.os.Build.VERSION.SDK_INT >= 19) {
                am.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.set(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }

            Log.i(TAG, "OTA 설치 완료: AlarmManager 2초 후 신버전 기동 예약 완료 — 구 프로세스 종료")
            // Process.killProcess 는 RTC_WAKEUP AlarmManager 를 취소하지 않음
            // (am force-stop 과 달리 시스템 알람 서비스는 프로세스 kill 이후에도 유지됨)
            android.os.Process.killProcess(android.os.Process.myPid())

        } catch (e: Exception) {
            Log.w(TAG, "scheduleRestartAndKillLegacy 실패: ${e.message}")
            PlayerForegroundService.start(context)
            context.startActivity(Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
        }
    }

    private fun enableAdbTcp() {
        try {
            Runtime.getRuntime().exec(arrayOf("su", "-c",
                "setprop service.adb.tcp.port 5555 && stop adbd && start adbd"
            )).waitFor()
            Log.i(TAG, "ADB TCP 5555 활성화 완료")
        } catch (e: IOException) {
            Log.w(TAG, "ADB TCP 활성화 실패 (root 없음): ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "ADB TCP 활성화 오류: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
