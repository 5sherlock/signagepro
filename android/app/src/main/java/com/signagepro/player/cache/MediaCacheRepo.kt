package com.signagepro.player.cache

import android.content.Context
import com.signagepro.player.api.ApiClient
import com.signagepro.player.api.MediaDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * 미디어 파일 다운로드 + SHA-256 검증 + LRU 정리.
 *
 * 정책:
 * - 전체 prefetch: 현재 playlist의 모든 미디어를 미리 받음 → 서버 오프라인에도 재생 유지
 * - 캐시 키: 서버가 내려준 hash (SHA-256). 같은 hash면 재다운로드 안 함.
 * - 정리 기준은 "고정 쿼터"가 아니라 **실제 여유공간**:
 *     · 캐시 폴더가 [maxCacheBytes] 를 넘거나
 *     · 파티션 여유공간이 [minFreeBytes] 아래로 떨어지면
 *   playlist에 없는(inactive) 파일부터 오래된 순으로 삭제.
 * - 다운로드는 정리(evict) **후**에 수행 → 작은 파티션 보드의 ENOSPC 방지.
 *
 * 작은 플래시(예: 2.9GB) 보드에서도 디스크를 꽉 채우지 않도록 여유공간을 항상 확보한다.
 */
class MediaCacheRepo(
    private val context: Context,
    private val maxCacheBytes: Long = DEFAULT_MAX_CACHE,
    private val minFreeBytes: Long = DEFAULT_MIN_FREE
) {

    private val baseDir: File by lazy {
        File(context.filesDir, "media_cache").also { it.mkdirs() }
    }

    // hash별 다운로드 직렬화 — 같은 파일을 동시에 받아 .part가 손상되는 race 방지
    private val downloadLocks = ConcurrentHashMap<String, Mutex>()

    /**
     * 캐시된 파일 경로 반환. hash가 없거나 검증 실패 시 null.
     */
    fun cachedFile(media: MediaDto): File? {
        val hash = media.hash ?: return null
        val file = File(baseDir, fileNameFor(media, hash))
        return if (file.exists() && file.length() > 0) file else null
    }

    /**
     * 필요 시 다운로드 후 파일 반환. 이미 같은 hash면 즉시 반환.
     * [onProgress]: 다운로드 진행률 (0~100). Content-Length 미제공 시 호출 안 됨.
     * 다운로드 실패 시 [DownloadException].
     */
    suspend fun ensure(
        serverUrl: String,
        media: MediaDto,
        onProgress: (suspend (pct: Int) -> Unit)? = null
    ): File = withContext(Dispatchers.IO) {
        cachedFile(media)?.let { return@withContext it }

        val hash = media.hash
            ?: throw DownloadException("Media ${media.id} has no hash — 서버 업그레이드 필요")

        val lock = downloadLocks.getOrPut(hash) { Mutex() }
        lock.withLock {
            // 락 획득 후 재확인 — 다른 코루틴이 이미 받았으면 재다운로드 불필요
            cachedFile(media)?.let { return@withLock it }

            val target = File(baseDir, fileNameFor(media, hash))
            val tmp = File(baseDir, "${target.name}.${System.nanoTime()}.part")

            val url = if (media.path.startsWith("http")) media.path
                      else serverUrl.trimEnd('/') + media.path
            val request = Request.Builder().url(url).build()
            try {
                ApiClient.http().newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw DownloadException("HTTP ${resp.code} for $url")
                    }
                    val body = resp.body ?: throw DownloadException("Empty body for $url")
                    val contentLength = body.contentLength() // -1이면 알 수 없음

                    // 다운로드 직전 여유공간 확인 — 부족하면 디스크를 채우지 않고 즉시 실패(명확한 사유)
                    val free = baseDir.usableSpace
                    val needed = (if (contentLength > 0) contentLength else 0L) + SAFETY_MARGIN
                    if (free < needed) {
                        throw DownloadException(
                            "저장공간 부족: ${media.filename} 필요 ${needed / 1_048_576}MB / 여유 ${free / 1_048_576}MB"
                        )
                    }

                    val buf = ByteArray(32 * 1024) // 32KB 버퍼
                    var downloaded = 0L
                    var lastReportedPct = -1
                    var lastReportMs = 0L

                    tmp.outputStream().use { out ->
                        body.byteStream().use { input ->
                            while (true) {
                                val n = input.read(buf)
                                if (n <= 0) break
                                out.write(buf, 0, n)
                                downloaded += n

                                // 진행률 보고: 5% 단위 또는 500ms 간격으로 throttle
                                if (onProgress != null && contentLength > 0) {
                                    val pct = (downloaded * 100L / contentLength).toInt()
                                    val now = System.currentTimeMillis()
                                    if (pct != lastReportedPct &&
                                        (pct - lastReportedPct >= 5 || now - lastReportMs >= 500)) {
                                        lastReportedPct = pct
                                        lastReportMs = now
                                        withContext(Dispatchers.Main) {
                                            onProgress(pct)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // 완료 시 100% 보고
                    if (onProgress != null && contentLength > 0) {
                        withContext(Dispatchers.Main) { onProgress(100) }
                    }
                }

                val actualHash = sha256(tmp)
                if (!actualHash.equals(hash, ignoreCase = true)) {
                    throw DownloadException("Hash mismatch for ${media.filename}: expected=$hash actual=$actualHash")
                }
                if (!tmp.renameTo(target)) {
                    throw DownloadException("Failed to finalize cache file for ${media.filename}")
                }
                target
            } finally {
                if (tmp.exists()) tmp.delete()
            }
        }
    }

    /**
     * 다운로드 *전에* 호출 — 새 playlist에 없는(inactive) 파일을 오래된 순으로 삭제해
     * 여유공간을 미리 확보한다. 활성 파일은 건드리지 않으므로 재사용 파일 재다운로드가 없다.
     */
    suspend fun evictInactive(activeHashes: Set<String>) = withContext(Dispatchers.IO) {
        val inactive = baseDir.listFiles()
            ?.filter { f -> activeHashes.none { f.name.startsWith(it) } }
            ?.sortedBy { it.lastModified() }
            ?: return@withContext
        for (f in inactive) {
            if (!needsEviction()) break
            f.delete()
        }
    }

    /**
     * 다운로드 *후* 정리 — inactive부터 비우고, 그래도 부족하면 활성 파일까지(최후의 수단).
     */
    suspend fun trim(activeHashes: Set<String>) = withContext(Dispatchers.IO) {
        evictInactive(activeHashes)
        if (needsEviction()) {
            baseDir.listFiles()
                ?.filter { it.exists() }
                ?.sortedBy { it.lastModified() }
                ?.forEach { f ->
                    if (!needsEviction()) return@forEach
                    f.delete()
                }
        }
    }

    /** 항상 남겨둘 여유공간 = 고정값과 파티션의 20% 중 큰 값.
     *  작은 보드(2.9GB)에서 디스크가 과도하게 차는 걸 막는다(고정 600MB는 ~79%까지 채워 너무 높았음). */
    private fun effectiveMinFree(): Long =
        maxOf(minFreeBytes, baseDir.totalSpace / 5)   // /5 = 20%

    /** 캐시가 상한을 넘었거나 파티션 여유공간이 최소치 아래면 정리 필요. */
    private fun needsEviction(): Boolean =
        currentSize() > maxCacheBytes || baseDir.usableSpace < effectiveMinFree()

    fun currentSize(): Long =
        baseDir.listFiles()?.sumOf { it.length() } ?: 0L

    private fun fileNameFor(media: MediaDto, hash: String): String {
        val ext = media.filename.substringAfterLast('.', "bin")
        return "${hash}.${ext}"
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    class DownloadException(message: String) : RuntimeException(message)

    companion object {
        /** 캐시 폴더 상한 (큰 디스크에서도 무한정 쌓지 않도록). */
        private const val DEFAULT_MAX_CACHE: Long = 2L * 1024 * 1024 * 1024   // 2GB
        /** 파티션에 항상 남겨둘 최소 여유공간 하한 (실제로는 effectiveMinFree에서 파티션 20%와 비교해 더 큰 값 사용). */
        private const val DEFAULT_MIN_FREE: Long = 1024L * 1024 * 1024        // 1GB
        /** 다운로드 직전 추가로 요구하는 여유 마진. */
        private const val SAFETY_MARGIN: Long = 64L * 1024 * 1024             // 64MB
    }
}
