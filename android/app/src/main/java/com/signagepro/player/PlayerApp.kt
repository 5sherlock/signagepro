package com.signagepro.player

import android.app.Application
import com.signagepro.player.config.ConfigStore

class PlayerApp : Application() {
    lateinit var config: ConfigStore
        private set

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
