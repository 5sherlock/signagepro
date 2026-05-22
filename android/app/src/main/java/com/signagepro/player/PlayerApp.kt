package com.signagepro.player

import android.content.Context
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
        enableAdbTcp()
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
    }
}
