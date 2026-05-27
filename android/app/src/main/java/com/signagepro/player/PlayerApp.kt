package com.signagepro.player

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.signagepro.player.config.ConfigStore

class PlayerApp : MultiDexApplication() {
    lateinit var config: ConfigStore
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        config = ConfigStore(this)
        installCrashHandler()
        enableAdbTcp()
    }

    /**
     * 전역 크래시 핸들러.
     * 앱이 크래시되면 "앱이 중지되었습니다" 다이얼로그 없이 3초 후 자동 재시작.
     */
    private fun installCrashHandler() {
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            try {
                Log.e(TAG, "크래시 감지 → 3초 후 자동 재시작", throwable)
                val restartIntent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                else
                    PendingIntent.FLAG_ONE_SHOT
                val pending = PendingIntent.getActivity(this, 9999, restartIntent, flags)
                val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarm.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 3_000L, pending)
            } catch (e: Exception) {
                Log.e(TAG, "재시작 예약 실패", e)
            } finally {
                Process.killProcess(Process.myPid())
            }
        }
    }

    // ADB TCP 포트 5555 활성화 — 서버에서 adb install 로 무인 OTA 설치 가능하게
    private fun enableAdbTcp() {
        Thread {
            try {
                Runtime.getRuntime().exec("setprop service.adb.tcp.port 5555").waitFor()
                Runtime.getRuntime().exec("stop adbd").waitFor()
                Thread.sleep(400)
                Runtime.getRuntime().exec("start adbd").waitFor()
                android.util.Log.i("AdbTcp", "ADB TCP :5555 활성화")
            } catch (e: Exception) {
                android.util.Log.w("AdbTcp", "ADB TCP 활성화 실패: ${e.message}")
            }
        }.start()
    }

    companion object {
        lateinit var instance: PlayerApp
            private set
        private const val TAG = "PlayerApp"
    }
}
