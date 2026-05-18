package com.signagepro.player

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * 부팅 후 진입점.
 * - 설정이 없으면 SetupActivity로 보냄
 * - 설정이 있으면 바로 KioskActivity로 진입
 */
class BootActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = PlayerApp.instance.config
        val next = if (config.isConfigured()) KioskActivity::class.java
                   else SetupActivity::class.java
        startActivity(Intent(this, next))
        finish()
    }
}
