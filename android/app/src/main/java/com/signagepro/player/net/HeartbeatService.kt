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
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/**
 * 서버 TCP 10080에 연결하여 인증 → 주기적 하트비트 전송.
 * 끊기면 지수 백오프로 재연결 (1s → 60s).
 *
 * 프로토콜 (server/index.js와 동일):
 *   1. 클라이언트 → auth:<deviceId>:<secret>\n
 *   2. 서버    → auth:ok\n
 *   3. 클라이언트 → status:<deviceId>/cpu:<n>/mem:<n>\n  (주기)
 *   4. 서버    → ok:\n
 */
class HeartbeatService(
    private val serverHost: String,
    private val serverPort: Int = 10080,
    private val deviceId: String,
    private val deviceSecret: String,
    private val metrics: SystemMetrics,
    private val intervalMs: Long = 10_000L,
    private val connectTimeoutMs: Int = 5_000
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
        var backoff = 1_000L
        while (currentCoroutineContext().isActive) {
            try {
                runSession()
                backoff = 1_000L
            } catch (e: Exception) {
                Log.w(TAG, "TCP 세션 종료: ${e.message}")
            }
            if (!currentCoroutineContext().isActive) break
            delay(backoff)
            backoff = (backoff * 2).coerceAtMost(60_000L)
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

            // 2. 하트비트 루프
            while (currentCoroutineContext().isActive) {
                val cpu = "%.1f".format(metrics.cpuUsage())
                val mem = "%.1f".format(metrics.memUsage())
                out.write("status:$deviceId/cpu:$cpu/mem:$mem\n"); out.flush()
                val ack = input.readLine() ?: throw IOException("EOF on heartbeat")
                if (!ack.startsWith("ok")) throw IOException("ACK 오류: $ack")
                delay(intervalMs)
            }
        }
    }

    companion object {
        private const val TAG = "HeartbeatService"
    }
}
