package com.signagepro.player

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.signagepro.player.databinding.ActivitySetupBinding

/**
 * 첫 부팅 시 deviceId, 서버 URL, 디바이스 시크릿 입력.
 * 설정 완료 후 KioskActivity로 진입.
 */
class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

            startActivity(Intent(this, KioskActivity::class.java))
            finish()
        }
    }
}
