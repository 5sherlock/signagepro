package com.signagepro.player.net

import android.util.Log
import com.signagepro.player.engine.SystemMetrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/**
 * 서버 TCP 10080에 연결하여 인증 → 주기적 하트비트 전송.
 * 끊기면 지수 백오프로 재연결 (1s → 60s).
 *
 * 프로토콜:
 *   1. 클라이언트 → auth:<deviceId>:<secret>\n
 *   2. 서버    → auth:ok\n
 *   3. 클라이언트 → status:<deviceId>/cpu:<n>/mem:<n>/ver:<v>/vol:<n>\n  (10초)
 *   4. 서버    → ok:\n
 *   5. 클라이언트 → vu:<deviceId>/<level>\n  (300ms, ack 없음)
 */
class HeartbeatService(
    private val serverHost: String,
    private val serverPort: Int = 10080,
    private val deviceId: String,
    private val deviceSecret: String,
    private val metrics: SystemMetrics,
    private val appVersion: String = "unknown",
    private val intervalMs: Long = 10_000L,
    private val connectTimeoutMs: Int = 5_000,
    /** 다운로드 진행 중이면 "cur/total/pct" 반환, 아니면 null */
    private val dlStatusProvider: (() -> String?)? = null,
    /** 현재 볼륨 레벨 (0~15) 반환 */
    private val volumeProvider: (() -> Int?)? = null,
    /** 실제 오디오 출력 레벨 (0~100) — Visualizer 측정값 */
    private val vuProvider: (() -> Int)? = null,
    /**
     * 현재 재생 중인 슬라이드 정보. 형식: "<index>|<total>|<filename>"
     * index/total은 1-based. null이면 heartbeat에 포함하지 않음.
     */
    private val slideProvider: (() -> String?)? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    fun start() {
        job?.cancel()
        job = scope.launch { runForever() }
    }

    fun stop() {
        job?.cancel()
        scope.coroutineContext[Job]?.cancel()
    }

    private suspend fun runForever() {
        var backoff = 0L // 첫 재연결은 즉시 (0ms), 이후 지수 백오프
        while (currentCoroutineContext().isActive) {
            try {
                runSession()
                backoff = 0L // 정상 종료 시 즉시 재시도
            } catch (e: Exception) {
                Log.w(TAG, "TCP 세션 종료: ${e.message}")
            }
            if (!currentCoroutineContext().isActive) break
            if (backoff > 0L) delay(backoff)
            backoff = (backoff.coerceAtLeast(500L) * 2).coerceAtMost(60_000L)
        }
    }

    private suspend fun runSession() {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(serverHost, serverPort), connectTimeoutMs)
            socket.soTimeout = (intervalMs * 3).toInt().coerceAtMost(60_000)
            val out = socket.getOutputStream().bufferedWriter(Charsets.UTF_8)
            val input = socket.getInputStream().bufferedReader(Charsets.UTF_8)

            // 1. 인증
            out.write("auth:$deviceId:$deviceSecret\n"); out.flush()
            val authResp = input.readLine() ?: throw IOException("EOF on auth")
            if (authResp != "auth:ok") throw IOException("인증 실패: $authResp")
            Log.i(TAG, "TCP 인증 성공")

            // 소켓 쓰기 뮤텍스 — 하트비트 루프 + VU 루프 동시 쓰기 방지
            val writeMutex = Mutex()

            // 2. VU 빠른 전송 루프 (300ms, ack 없음)
            val vuJob = if (vuProvider != null) {
                scope.launch {
                    while (currentCoroutineContext().isActive) {
                        val level = vuProvider.invoke()
                        writeMutex.withLock {
                            out.write("vu:$deviceId/$level\n")
                            out.flush()
                        }
                        delay(300L)
                    }
                }
            } else null

            try {
                // 3. 하트비트 루프 (10초, ack 필요)
                while (currentCoroutineContext().isActive) {
                    val cpu = "%.1f".format(metrics.cpuUsage())
                    val mem = "%.1f".format(metrics.memUsage())
                    val dlPart = dlStatusProvider?.invoke()?.let { "/dl:$it" } ?: ""
                    val volPart = volumeProvider?.invoke()?.let { "/vol:$it" } ?: ""
                    val timePart = "/time:${System.currentTimeMillis()}"
                    // slide: "1|5|filename.jpg" 형식 — '|' 구분자로 '/' 충돌 방지
                    val slidePart = slideProvider?.invoke()?.let { "/slide:$it" } ?: ""
                    writeMutex.withLock {
                        out.write("status:$deviceId/cpu:$cpu/mem:$mem/ver:$appVersion$dlPart$volPart$timePart$slidePart\n")
                        out.flush()
                    }
                    val ack = input.readLine() ?: throw IOException("EOF on heartbeat")
                    if (!ack.startsWith("ok")) throw IOException("ACK 오류: $ack")
                    delay(intervalMs)
                }
            } finally {
                vuJob?.cancel()
            }
        }
    }

    companion object {
        private const val TAG = "HeartbeatService"
    }
}
