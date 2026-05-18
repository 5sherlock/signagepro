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
    }

    companion object {
        lateinit var instance: PlayerApp
            private set
    }
}
