package com.signagepro.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 부팅 완료 시 PlayerActivity를 자동 실행.
 *
 * 동작 조건:
 * 1. RECEIVE_BOOT_COMPLETED 권한 보유 (Manifest에 선언)
 * 2. 앱이 최소 1회 이상 수동으로 실행된 적 있어야 함 (Android 3.1+ 패키지 정지 정책)
 * 3. 설정이 완료된 경우(ConfigStore.isConfigured)에만 바로 키오스크 모드로 진입
 *    — 미설정 시에는 앱을 띄워 setup 화면 표시
 *
 * RK3229 / Android 5.1.1 확인 사항:
 * - 일부 STB ROM은 BOOT_COMPLETED가 늦게 오거나 안 오는 경우가 있음
 * - 그 경우 ROM 자체 "자동 시작" 설정(개발자 옵션 또는 런처 설정)을 병행 사용
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != "android.intent.action.REBOOT"
        ) return

        Log.i(TAG, "부팅 완료 감지 (action=$action) — MainActivity 시작")

        val launch = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // 이미 실행 중인 인스턴스가 있으면 재사용
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        context.startActivity(launch)
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
