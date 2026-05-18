package com.signagepro.player.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.signagepro.player.BootActivity

/**
 * 단말 부팅 완료 시 BootActivity를 자동 실행.
 * (자가 부활 1단계 — 추후 WatchdogService와 결합)
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val launch = Intent(context, BootActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(launch)
        }
    }
}
