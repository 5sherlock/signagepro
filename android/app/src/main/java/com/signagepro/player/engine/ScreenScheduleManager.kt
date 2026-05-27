package com.signagepro.player.engine

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.PowerManager
import android.util.Log
import com.signagepro.player.SignageDeviceAdmin
import com.signagepro.player.api.ScheduleDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

class ScreenScheduleManager(private val context: Context) {

    @Volatile private var schedules: List<ScheduleDto> = emptyList()
    private var job: Job? = null

    /**
     * 화면 ON/OFF 시 호출되는 콜백.
     * true = 화면 켜짐, false = 화면 꺼짐.
     * HeartbeatService에 상태를 전달하기 위해 PlayerCoordinator가 설정.
     */
    var onScreenStateChange: ((Boolean) -> Unit)? = null

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(context, SignageDeviceAdmin::class.java)

    fun update(newSchedules: List<ScheduleDto>) {
        schedules = newSchedules
        Log.i(TAG, "스케줄 업데이트: ${schedules.size}개")
    }

    fun start(scope: CoroutineScope) {
        job?.cancel()
        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                checkAndExecute()
                // 다음 분 시작 직후까지 대기 (drift 방지)
                val now = System.currentTimeMillis()
                val nextMinute = (now / 60_000 + 1) * 60_000
                delay(nextMinute - now + 500L)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun checkAndExecute() {
        // 스케줄 시각은 KST(한국 표준시) 기준 — 기기 시스템 시간대와 무관하게 항상 Asia/Seoul 사용
        val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Seoul"))
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        // Calendar: SUNDAY=1 → 0, MONDAY=2 → 1 ... SATURDAY=7 → 6
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
        val currentTime = "%02d:%02d".format(hour, minute)

        for (s in schedules) {
            if (!s.enabled) continue
            val days = s.days.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (dayOfWeek !in days) continue

            if (s.onTime != null && s.onTime == currentTime) {
                Log.i(TAG, "화면 켜기 실행: $currentTime")
                turnScreenOn()
            }
            if (s.offTime != null && s.offTime == currentTime) {
                Log.i(TAG, "화면 끄기 실행: $currentTime")
                turnScreenOff()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun turnScreenOn() {
        try {
            // SCREEN_BRIGHT_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP: API 1+ 지원, API 22에서 정상 동작
            val wl = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "SignagePro:ScreenScheduleOn"
            )
            wl.acquire(3_000L)
            Log.i(TAG, "화면 켜기 완료")
            onScreenStateChange?.invoke(true)
        } catch (e: Exception) {
            Log.e(TAG, "화면 켜기 실패", e)
        }
    }

    private fun turnScreenOff() {
        try {
            if (dpm.isAdminActive(adminComponent)) {
                dpm.lockNow()
                Log.i(TAG, "화면 끄기 완료 (lockNow)")
                onScreenStateChange?.invoke(false)
            } else {
                Log.w(TAG, "Device Admin 미활성 — 화면 끄기 불가. 관리자 권한을 부여하세요.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "화면 끄기 실패", e)
        }
    }

    companion object {
        private const val TAG = "ScreenScheduleManager"
    }
}
