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
 * 정책 (Track A-1):
 * - 전체 prefetch: 현재 playlist의 모든 미디어를 미리 받음 → 서버 오프라인에도 재생 유지
 * - 캐시 키: 서버가 내려준 hash (SHA-256). 같은 hash면 재다운로드 안 함.
 * - 스토리지 쿼터 [quotaBytes] 초과 시, playlist에 포함되지 않은 가장 오래된 파일부터 삭제.
 *
 * 8GB Flash 환경 고려: 기본 quota 2GB.
 */
class MediaCacheRepo(
    private val context: Context,
    private val quotaBytes: Long = DEFAULT_QUOTA
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

            val url = serverUrl.trimEnd('/') + media.path
            val request = Request.Builder().url(url).build()
            try {
                ApiClient.http().newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw DownloadException("HTTP ${resp.code} for $url")
                    }
                    val body = resp.body ?: throw DownloadException("Empty body for $url")
                    val contentLength = body.contentLength() // -1이면 알 수 없음

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
     * 현재 playlist에 포함된 미디어의 hash 집합을 받아, 미포함 + 오래된 파일부터 삭제.
     */
    suspend fun trim(activeHashes: Set<String>) = withContext(Dispatchers.IO) {
        val files = baseDir.listFiles()?.toMutableList() ?: return@withContext
        // 활성 hash가 들어있지 않은 파일은 우선 삭제 대상
        val inactive = files.filter { f -> activeHashes.none { f.name.startsWith(it) } }
            .sortedBy { it.lastModified() }
        for (f in inactive) {
            if (currentSize() <= quotaBytes) break
            f.delete()
        }
        // 그래도 초과면 활성 파일 중에서도 오래된 것부터 삭제 (최후의 수단)
        if (currentSize() > quotaBytes) {
            files.filter { it.exists() }.sortedBy { it.lastModified() }.forEach { f ->
                if (currentSize() <= quotaBytes) return@forEach
                f.delete()
            }
        }
    }

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
        private const val DEFAULT_QUOTA: Long = 2L * 1024 * 1024 * 1024  // 2GB
    }
}
