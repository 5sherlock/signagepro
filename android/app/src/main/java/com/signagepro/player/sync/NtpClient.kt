package com.signagepro.player.sync

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
 * 외부 NTP 서버에서 현재 epoch ms를 받아오고, 이후에는 SystemClock.elapsedRealtime()
 * 기반으로 보정 — 매번 NTP 호출하지 않고 부팅 시 한 번 + 주기적 갱신.
 *
 * 5대 보드가 모두 같은 NTP를 보면, 동일 epoch ms에서 동일 슬라이드 인덱스를 계산해
 * 서버 없이도 동기 재생이 유지됨.
 */
class NtpClient(
    private val host: String = "pool.ntp.org",
    private val timeoutMs: Int = 3000
) {
    @Volatile private var lastSyncedEpochMs: Long = 0L
    @Volatile private var lastSyncedElapsed: Long = 0L
    @Volatile var source: Source = Source.NONE
        private set

    enum class Source { NONE, NTP, SERVER }

    /** 동기화된 적이 있는지 */
    val isSynced: Boolean get() = lastSyncedEpochMs > 0L

    /** 디버그 오버레이용 라벨 */
    val sourceLabel: String
        get() = when (source) {
            Source.NTP -> "SYNCED(NTP)"
            Source.SERVER -> "SYNCED(SVR)"
            Source.NONE -> "LOCAL(미동기)"
        }

    /**
     * 시각 동기. 외부 NTP 우선, 실패 시 서버 시각으로 fallback.
     * 둘 다 실패하면 false (now()는 로컬 시각 사용).
     *
     * 5대 보드가 같은 NTP 또는 같은 서버 시각을 받으면 동일 슬롯 인덱스를 계산해 동기 재생.
     */
    suspend fun sync(serverUrl: String? = null): Boolean = withContext(Dispatchers.IO) {
        if (syncFromNtp()) return@withContext true
        if (serverUrl != null && syncFromServer(serverUrl)) return@withContext true
        false
    }

    private fun syncFromNtp(): Boolean {
        return try {
            val epochMs = requestTime(host, timeoutMs)
            lastSyncedEpochMs = epochMs
            lastSyncedElapsed = SystemClock.elapsedRealtime()
            source = Source.NTP
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
                lastSyncedEpochMs = epochMs
                lastSyncedElapsed = SystemClock.elapsedRealtime()
                source = Source.SERVER
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 현재 시각 (epoch ms). 동기화된 적 없으면 System.currentTimeMillis() fallback.
     */
    fun now(): Long {
        if (!isSynced) return System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime() - lastSyncedElapsed
        return lastSyncedEpochMs + elapsed
    }

    private fun requestTime(host: String, timeoutMs: Int): Long {
        DatagramSocket().use { socket ->
            socket.soTimeout = timeoutMs
            val address = InetAddress.getByName(host)
            val buf = ByteArray(NTP_PACKET_SIZE)
            // LI=0, VN=3, Mode=3 (client) = 0b00_011_011 = 0x1B
            buf[0] = 0x1B
            val request = DatagramPacket(buf, buf.size, address, NTP_PORT)
            socket.send(request)

            val response = DatagramPacket(buf, buf.size)
            socket.receive(response)

            // Transmit Timestamp (서버가 응답을 보낸 시각) = offset 40, 8바이트
            // seconds(4) + fraction(4), 1900-01-01 기준
            val seconds = readUInt32(buf, 40)
            val fraction = readUInt32(buf, 44)
            val ntpSeconds = seconds - NTP_EPOCH_OFFSET_SECONDS
            val ms = ntpSeconds * 1000L + (fraction * 1000L) / 0x100000000L
            return ms
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
        // NTP epoch (1900-01-01) → Unix epoch (1970-01-01) 차이 (초)
        private const val NTP_EPOCH_OFFSET_SECONDS = 2208988800L
    }
}
