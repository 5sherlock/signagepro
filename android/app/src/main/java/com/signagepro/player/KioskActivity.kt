package com.signagepro.player

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.signagepro.player.databinding.ActivityKioskBinding
import com.signagepro.player.engine.PlayerCoordinator
import com.signagepro.player.render.MediaRenderer

/**
 * 풀스크린 키오스크 — PlayerCoordinator를 통해 실제 재생을 구동.
 */
class KioskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKioskBinding
    private lateinit var coordinator: PlayerCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKioskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        applyImmersiveMode()

        val renderer = MediaRenderer(this, binding.layerA, binding.layerB)
        coordinator = PlayerCoordinator(
            context = applicationContext,
            config = PlayerApp.instance.config,
            renderer = renderer,
            onStatus = { msg ->
                runOnUiThread {
                    binding.statusText.text = msg
                    binding.statusText.visibility = if (msg.isBlank()) View.GONE else View.VISIBLE
                }
            }
        )
        coordinator.start()
    }

    override fun onDestroy() {
        coordinator.stop()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveMode()
    }

    @Suppress("DEPRECATION")
    private fun applyImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(android.view.WindowInsets.Type.systemBars())
                it.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }
}
