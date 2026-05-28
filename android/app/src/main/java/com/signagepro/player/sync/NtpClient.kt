package com.signagepro.player.sync

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import com.signagepro.player.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * SNTP 클라이언트 (RFC 4330 단순 구현).
 *
 * 동기화 우선순위:
 *   1. 하트비트 ACK (10초마다, RTT 보정)  — 가장 빈번하고 정확
 *   2. 서버 /api/time (부팅 시 1회)
 *   3. pool.ntp.org (서버 불가 시 fallback)
 *   4. SharedPreferences 복원 (재부팅 후 서버 미응답 구간 커버)
 *   5. System.currentTimeMillis() (최후 수단)
 *
 * elapsedRealtime()은 재부팅 시 0으로 리셋되므로 SharedPreferences에는
 * epoch + 저장 당시 wallClock 을 함께 저장해 재부팅 후 RTC 기반 추정치 복원.
 */
class NtpClient(
    context: Context,
    private val host: String = "pool.ntp.org",
    private val timeoutMs: Int = 3000
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ntp_sync", Context.MODE_PRIVATE)

    @Volatile private var lastSyncedEpochMs: Long = 0L
    @Volatile private var lastSyncedElapsed: Long = 0L
    @Volatile var source: Source = Source.NONE
        private set

    enum class Source { NONE, NTP, SERVER, RESTORED }

    init {
        // 재부팅 후에도 마지막 동기값을 복원해 RTC 어긋남 구간을 최소화.
        // elapsedRealtime이 리셋되므로 저장 당시 wallClock과의 delta로 보정.
        val savedEpoch = prefs.getLong(KEY_EPOCH, 0L)
        val savedWall  = prefs.getLong(KEY_WALL, 0L)
        if (savedEpoch > 0L && savedWall > 0L) {
            val wallDelta = System.currentTimeMillis() - savedWall
            lastSyncedEpochMs = savedEpoch + wallDelta
            lastSyncedElapsed = SystemClock.elapsedRealtime()
            source = Source.RESTORED
        }
    }

    /** 동기화된 적이 있는지 */
    val isSynced: Boolean get() = lastSyncedEpochMs > 0L

    /** 디버그 오버레이용 라벨 */
    val sourceLabel: String
        get() = when (source) {
            Source.NTP      -> "SYNCED(NTP)"
            Source.SERVER   -> "SYNCED(SVR)"
            Source.RESTORED -> "RESTORED"
            Source.NONE     -> "LOCAL(미동기)"
        }

    /**
     * 시각 동기. 서버 시각 우선, 실패 시 외부 NTP로 fallback.
     * 둘 다 실패하면 false (now()는 복원값 또는 로컬 시각 사용).
     */
    suspend fun sync(serverUrl: String? = null): Boolean = withContext(Dispatchers.IO) {
        if (serverUrl != null && syncFromServer(serverUrl)) return@withContext true
        if (syncFromNtp()) return@withContext true
        false
    }

    private fun syncFromNtp(): Boolean {
        return try {
            val epochMs = requestTime(host, timeoutMs)
            commit(epochMs, Source.NTP)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun syncFromServer(serverUrl: String): Boolean {
        return try {
            val url = serverUrl.trimEnd('/') + "/api/time"
            val req = Request.Builder().url(url).build()
            ApiClient.http().newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return false
                val body = resp.body?.string() ?: return false
                val epochMs = JSONObject(body).getLong("epochMs")
                commit(epochMs, Source.SERVER)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 하트비트 ACK에서 받은 서버 epoch으로 직접 동기화.
     * RTT/2 보정: heartbeat 전송 시각과 ACK 수신 시각의 중간값을 사용.
     */
    fun syncFromHeartbeatAck(serverEpochMs: Long, sentAtElapsed: Long) {
        val rttHalf = (SystemClock.elapsedRealtime() - sentAtElapsed) / 2
        commit(serverEpochMs + rttHalf, Source.SERVER)
    }

    /**
     * 현재 시각 (epoch ms). 동기화된 적 없으면 System.currentTimeMillis() fallback.
     */
    fun now(): Long {
        if (!isSynced) return System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime() - lastSyncedElapsed
        return lastSyncedEpochMs + elapsed
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────────────────

    private fun commit(epochMs: Long, src: Source) {
        lastSyncedEpochMs = epochMs
        lastSyncedElapsed = SystemClock.elapsedRealtime()
        source = src
        prefs.edit()
            .putLong(KEY_EPOCH, epochMs)
            .putLong(KEY_WALL, System.currentTimeMillis())
            .apply()
    }

    private fun requestTime(host: String, timeoutMs: Int): Long {
        DatagramSocket().use { socket ->
            socket.soTimeout = timeoutMs
            val address = InetAddress.getByName(host)
            val buf = ByteArray(NTP_PACKET_SIZE)
            buf[0] = 0x1B
            val request = DatagramPacket(buf, buf.size, address, NTP_PORT)
            socket.send(request)
            val response = DatagramPacket(buf, buf.size)
            socket.receive(response)
            val seconds  = readUInt32(buf, 40)
            val fraction = readUInt32(buf, 44)
            val ntpSeconds = seconds - NTP_EPOCH_OFFSET_SECONDS
            return ntpSeconds * 1000L + (fraction * 1000L) / 0x100000000L
        }
    }

    private fun readUInt32(buf: ByteArray, offset: Int): Long {
        val b0 = (buf[offset].toInt() and 0xFF).toLong()
        val b1 = (buf[offset + 1].toInt() and 0xFF).toLong()
        val b2 = (buf[offset + 2].toInt() and 0xFF).toLong()
        val b3 = (buf[offset + 3].toInt() and 0xFF).toLong()
        return (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
    }

    companion object {
        private const val NTP_PORT = 123
        private const val NTP_PACKET_SIZE = 48
        private const val NTP_EPOCH_OFFSET_SECONDS = 2208988800L
        private const val KEY_EPOCH = "last_epoch_ms"
        private const val KEY_WALL  = "last_wall_ms"
    }
}
