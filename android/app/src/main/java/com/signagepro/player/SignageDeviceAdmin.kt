package com.signagepro.player

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class SignageDeviceAdmin : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Log.i(TAG, "Device Admin 활성화됨 — 화면 끄기 스케줄 사용 가능")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Log.i(TAG, "Device Admin 비활성화됨")
    }

    companion object {
        private const val TAG = "SignageDeviceAdmin"
    }
}
