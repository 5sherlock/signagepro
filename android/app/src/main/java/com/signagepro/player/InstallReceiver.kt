package com.signagepro.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import kotlinx.coroutines.CompletableDeferred

class InstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE
        )
        val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: ""

        // downloadAndInstallApk 에서 대기 중인 deferred 에 결과 전달
        pending?.complete(status == PackageInstaller.STATUS_SUCCESS)

        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                Log.i(TAG, "OTA 설치 완료 — 프로세스 종료 후 재시작")
                android.os.Process.killProcess(android.os.Process.myPid())
            }
            else -> {
                Log.e(TAG, "OTA 설치 실패: status=$status msg=$msg")
            }
        }
    }

    companion object {
        private const val TAG = "InstallReceiver"
        /** downloadAndInstallApk 이 설치 결과를 기다리는 Deferred */
        var pending: CompletableDeferred<Boolean>? = null
    }
}
