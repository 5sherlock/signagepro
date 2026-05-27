package com.signagepro.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log

/**
 * PackageInstaller(Device Owner) 설치 완료 브로드캐스트 수신.
 * 설치 성공 시 MainActivity를 자동으로 재시작한다.
 *
 * 앱 프로세스가 죽어도 Manifest에 등록된 리시버는 시스템이 직접 호출하므로
 * 신버전 앱이 설치된 직후 이 리시버가 실행된다.
 */
class InstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE
        )
        val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: ""

        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                Log.i(TAG, "OTA 설치 완료 — MainActivity 재시작")
                val launch = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                context.startActivity(launch)
            }
            else -> {
                Log.e(TAG, "OTA 설치 실패: status=$status msg=$msg")
            }
        }
    }

    companion object {
        private const val TAG = "InstallReceiver"
    }
}
