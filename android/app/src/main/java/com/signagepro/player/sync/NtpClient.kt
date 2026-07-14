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

    // Tailscale 등 지터 큰 경로 대비 — 하트비트 ACK 오프셋을 슬라이딩 윈도로 모아
    // "최소 RTT 근방 샘플들의 중앙값"으로 커밋한다. 한 번 튄 RTT가 시계를 흔들지 않도록.
    // offset = epoch - elapsedRealtime (실시간이 양쪽 동일 속도라 ~일정한 값).
    //
    // src: 이 오프셋이 서버에서 왔는지 외부 NTP에서 왔는지. 두 출처의 시계가 어긋나 있으면
    //      섞을 때 기기마다 혼합 비율이 달라져 오히려 기기 간 스큐가 생긴다 → 한 번에 한 출처만 쓴다.
    // atElapsed: 샘플 수집 시각. 개수로만 만료시키면 서버가 죽어도 낡은 SERVER 샘플이
    //      윈도에 영원히 남아 NTP 폴백으로 전환되지 않는다 → 나이로도 만료시킨다.
    private data class AckSample(val offset: Long, val rtt: Long, val src: Source, val atElapsed: Long)
    private val ackSamples = ArrayDeque<AckSample>()

    /** NTP 폴백 호출 간격 제한용 (pool.ntp.org 과다 호출 방지) */
    @Volatile private var lastNtpAtElapsed: Long = 0L

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

    /**
     * 외부 NTP 1왕복. 서버 burst와 동일하게 RTT/2 편도 보정 후 같은 필터에 주입한다.
     * (보정 없이 commit하면 출처가 바뀔 때 수십 ms 점프가 생겨 비디오월이 튄다)
     */
    private fun syncFromNtp(): Boolean {
        return try {
            val t0 = SystemClock.elapsedRealtime()
            val epochMs = requestTime(host, timeoutMs)
            val t1 = SystemClock.elapsedRealtime()
            val rtt = t1 - t0
            if (rtt !in 0..MAX_PLAUSIBLE_RTT_MS) return false
            addSampleAndCommit(epochMs + rtt / 2 - t1, rtt, Source.NTP)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * **서버 시각 동기 실패 시 폴백.** 서버가 끊기면 하트비트 ACK도 burst도 멈춰 시각 동기가
     * 완전히 정지하고, 보드마다 크리스탈 오차로 자유주행(free-run) 하다 서로 벌어진다
     * → 롤링/비디오월이 따로 논다. 외부 NTP로 계속 물려 기기 간 시계를 붙들어 둔다.
     *
     * 인터넷까지 끊긴 경우엔 이것도 실패하므로 드리프트를 막을 수 없다(그때는 네트워크를 고쳐야 함).
     */
    suspend fun burstSyncNtp(rounds: Int = NTP_BURST_ROUNDS): Boolean = withContext(Dispatchers.IO) {
        val now = SystemClock.elapsedRealtime()
        if (lastNtpAtElapsed != 0L && now - lastNtpAtElapsed < NTP_MIN_INTERVAL_MS) return@withContext false
        lastNtpAtElapsed = now
        var ok = false
        repeat(rounds) { if (syncFromNtp()) ok = true }
        ok
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
     * 하트비트 ACK에서 받은 서버 epoch으로 동기화.
     * RTT/2 편도 보정 후, 최근 샘플들 중 "최소 RTT 근방"만 추려 중앙값을 커밋한다.
     * → Tailscale/WiFi 지터로 한 번 RTT가 튀어도 시계가 흔들리지 않음(NTP best-sample + median).
     */
    fun syncFromHeartbeatAck(serverEpochMs: Long, sentAtElapsed: Long) {
        val recvElapsed = SystemClock.elapsedRealtime()
        val rtt = recvElapsed - sentAtElapsed
        if (rtt < 0 || rtt > MAX_PLAUSIBLE_RTT_MS) return   // 비정상/지연 샘플 폐기
        // offset = epoch - elapsed. RTT/2로 편도 보정한 현재 epoch에서 elapsed를 뺌.
        val offset = serverEpochMs + rtt / 2 - recvElapsed
        addSampleAndCommit(offset, rtt, Source.SERVER)
    }

    /**
     * /api/time 으로 빠른 왕복을 rounds회 수행해 시계 오프셋을 빠르게 '정밀 락' 한다.
     * 하트비트는 10초당 1샘플이라 수렴이 느리고 한 번 튄 RTT가 윈도에 오래 남는다.
     * burst는 1~2초 안에 여러 샘플을 모아 '최소 RTT 근방'(가장 대칭·정확)만 필터에 주입 →
     * Tailscale/WAN 비대칭 경로에서도 기기 간 스큐를 빠르게 줄인다(멀티스크린 비디오월 동기 핵심).
     *
     * 부팅 직후와 주기적으로 호출. 개별 라운드 실패는 무시하고 한 샘플이라도 성공하면 true.
     */
    suspend fun burstSync(serverUrl: String, rounds: Int = BURST_ROUNDS): Boolean = withContext(Dispatchers.IO) {
        val url = serverUrl.trimEnd('/') + "/api/time"
        var ok = false
        repeat(rounds) {
            try {
                val t0 = SystemClock.elapsedRealtime()
                val req = Request.Builder().url(url).build()
                val epochMs = ApiClient.http().newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@repeat
                    JSONObject(resp.body?.string() ?: return@repeat).getLong("epochMs")
                }
                val t1 = SystemClock.elapsedRealtime()
                val rtt = t1 - t0
                if (rtt in 0..MAX_PLAUSIBLE_RTT_MS) {
                    // offset = epoch - elapsed (RTT/2 편도보정한 recv 시점 epoch 기준)
                    addSampleAndCommit(epochMs + rtt / 2 - t1, rtt, Source.SERVER)
                    ok = true
                }
            } catch (e: Exception) { /* 개별 라운드 실패 무시 */ }
        }
        ok
    }

    /**
     * 오프셋 샘플을 슬라이딩 윈도에 넣고 '최소 RTT 근방' 샘플들의 중앙값으로 커밋.
     * 하트비트 ACK · 서버 burst · NTP 폴백이 모두 공유 — 지연 적은(정확한) 샘플이 우세해 지터 억제.
     *
     * 출처 선택: **서버 샘플이 하나라도 살아있으면 서버만** 쓰고 NTP 샘플은 무시한다.
     * 서버 시계와 NTP가 조금이라도 다를 때 둘을 섞으면 기기마다 혼합 비율이 달라져
     * 기기 간 스큐가 생기기 때문 — 그건 이 폴백이 막으려는 문제 그 자체다.
     * 서버가 죽어 SERVER 샘플이 전부 만료되면(SAMPLE_MAX_AGE_MS) 자연히 NTP로 넘어간다.
     */
    private fun addSampleAndCommit(offset: Long, rtt: Long, src: Source) {
        val filtered: Long
        val chosen: Source
        synchronized(ackSamples) {
            val nowElapsed = SystemClock.elapsedRealtime()
            ackSamples.addLast(AckSample(offset, rtt, src, nowElapsed))
            // 나이 만료 먼저 — 서버가 끊기면 SERVER 샘플이 빠져나가야 NTP로 전환된다.
            while (ackSamples.isNotEmpty() &&
                   nowElapsed - ackSamples.first().atElapsed > SAMPLE_MAX_AGE_MS) {
                ackSamples.removeFirst()
            }
            while (ackSamples.size > ACK_WINDOW) ackSamples.removeFirst()

            chosen = if (ackSamples.any { it.src == Source.SERVER }) Source.SERVER else Source.NTP
            val pool = ackSamples.filter { it.src == chosen }
            val minRtt = pool.minOf { it.rtt }
            val good = pool.filter { it.rtt <= minRtt + RTT_TOLERANCE_MS }
                .map { it.offset }.sorted()
            filtered = good[good.size / 2]
        }
        // now() == filtered + elapsedRealtime() 이 되도록 anchor 커밋.
        commit(filtered + SystemClock.elapsedRealtime(), chosen)
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
        // 하트비트 ACK 오프셋 필터(약 8개 × 10초 = 최근 ~80초 윈도)
        private const val ACK_WINDOW = 12
        private const val RTT_TOLERANCE_MS = 40L    // 최소 RTT + 이만큼 이내 샘플만 채택
        private const val MAX_PLAUSIBLE_RTT_MS = 5000L  // 이보다 큰 RTT는 폐기
        private const val BURST_ROUNDS = 12         // burstSync 1회당 빠른 왕복 횟수
        // 샘플 나이 만료. 하트비트 10초 / burst 20초 주기라 서버가 살아있으면 항상 갱신된다.
        // 서버가 끊기면 이 시간 뒤 SERVER 샘플이 비고 NTP 폴백이 인계받는다.
        private const val SAMPLE_MAX_AGE_MS = 120_000L
        private const val NTP_BURST_ROUNDS = 3      // NTP 폴백 1회당 왕복 횟수
        private const val NTP_MIN_INTERVAL_MS = 60_000L  // pool.ntp.org 과다 호출 방지
    }
}
