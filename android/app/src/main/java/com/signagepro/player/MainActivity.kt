package com.signagepro.player

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.signagepro.player.databinding.ActivityMainBinding
import com.signagepro.player.engine.PlayerCoordinator
import com.signagepro.player.render.MediaRenderer

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var coordinator: PlayerCoordinator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val config = PlayerApp.instance.config
        if (config.isConfigured()) startKiosk() else showSetup()
    }

    private fun showSetup() {
        binding.setupContainer.visibility = View.VISIBLE
        binding.kioskContainer.visibility = View.GONE

        val config = PlayerApp.instance.config
        binding.deviceIdInput.setText(config.deviceId ?: "")
        binding.serverUrlInput.setText(config.serverUrl ?: "http://192.168.0.10:3000")
        binding.deviceSecretInput.setText(config.deviceSecret ?: "")

        binding.saveButton.setOnClickListener {
            val deviceId = binding.deviceIdInput.text.toString().trim()
            val serverUrl = binding.serverUrlInput.text.toString().trim().trimEnd('/')
            val secret = binding.deviceSecretInput.text.toString().trim()

            if (deviceId.isEmpty() || serverUrl.isEmpty() || secret.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            config.deviceId = deviceId
            config.serverUrl = serverUrl
            config.deviceSecret = secret

            startKiosk()
        }
    }

    private fun startKiosk() {
        binding.setupContainer.visibility = View.GONE
        binding.kioskContainer.visibility = View.VISIBLE
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
        coordinator!!.start()
    }

    override fun onDestroy() {
        coordinator?.stop()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && coordinator != null) applyImmersiveMode()
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
            @Suppress("DEPRECATION")
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
