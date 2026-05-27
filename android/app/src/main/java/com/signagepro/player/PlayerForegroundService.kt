package com.signagepro.player

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 앱 프로세스 보호용 포그라운드 서비스.
 *
 * 배경:
 * - MainActivity가 PlayerCoordinator/HeartbeatService를 소유 → Activity 죽으면 같이 사망
 * - 시스템 OOM, 백그라운드 회수, 서드파티 메모리 정리 등으로 앱 프로세스가 죽으면 부활 안 됨
 *
 * 동작:
 * - START_STICKY: 시스템이 죽여도 자동 재시작
 * - onStartCommand / onTaskRemoved에서 MainActivity 살아있는지 확인 후 없으면 재실행
 * - 알림은 IMPORTANCE_MIN + 무음 → 사용자에게 거의 안 보임
 */
class PlayerForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watchdogJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification())
        Log.i(TAG, "PlayerForegroundService 시작 (프로세스 보호)")
        startWatchdog()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureActivityAlive()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        watchdogJob?.cancel()
        super.onDestroy()
    }

    /** 사용자가 최근 앱에서 swipe 제거 → 서비스 유지 + Activity 재실행 */
    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i(TAG, "Task 제거됨 — MainActivity 재시작")
        ensureActivityAlive()
        super.onTaskRemoved(rootIntent)
    }

    /**
     * 1분마다 MainActivity가 포그라운드에서 실행 중인지 확인.
     * 죽어있으면 자동으로 재시작.
     */
    private fun startWatchdog() {
        watchdogJob = scope.launch {
            delay(WATCHDOG_INITIAL_DELAY_MS)  // 첫 실행 30초 후부터 감시 시작
            while (isActive) {
                if (!isMainActivityRunning()) {
                    Log.w(TAG, "워치독: MainActivity 미실행 감지 → 재시작")
                    ensureActivityAlive()
                }
                delay(WATCHDOG_INTERVAL_MS)
            }
        }
    }

    private fun isMainActivityRunning(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.getRunningTasks(10)?.any { task ->
            task.topActivity?.className == MainActivity::class.java.name
        } ?: false
    }

    private fun ensureActivityAlive() {
        try {
            val launch = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(launch)
        } catch (e: Exception) {
            Log.w(TAG, "MainActivity 재시작 실패: ${e.message}")
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "SignagePro Keepalive",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "사이니지 플레이어 백그라운드 보호"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else
            PendingIntent.FLAG_UPDATE_CURRENT
        val pending = PendingIntent.getActivity(this, 0, launchIntent, pendingFlags)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SignagePro")
            .setContentText("디지털 사이니지 실행 중")
            .setSmallIcon(R.drawable.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pending)
            .build()
    }

    companion object {
        private const val TAG = "PlayerFGService"
        private const val CHANNEL_ID = "signagepro_keepalive"
        private const val NOTIF_ID = 4242
        private const val WATCHDOG_INITIAL_DELAY_MS = 30_000L   // 30초 후 감시 시작
        private const val WATCHDOG_INTERVAL_MS = 60_000L        // 1분마다 체크

        fun start(context: Context) {
            val intent = Intent(context, PlayerForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "포그라운드 서비스 시작 실패: ${e.message}")
            }
        }
    }
}
