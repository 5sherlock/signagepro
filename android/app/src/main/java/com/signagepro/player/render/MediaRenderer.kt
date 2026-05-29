package com.signagepro.player.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.signagepro.player.api.PlaylistItemDto
import java.io.File

/**
 * 두 개의 레이어를 alpha 크로스페이드하여 슬라이드 전환을 구현.
 * - 비디오: 단일 ExoPlayer 인스턴스를 활성 레이어의 PlayerView에 재부착 (메모리 절약)
 * - 이미지: 활성 레이어의 ImageView에 setImageBitmap
 *
 * Android 5.1.1 + 2GB RAM 환경 가정 — Bitmap을 inSampleSize로 다운샘플링.
 */
class MediaRenderer(
    private val context: Context,
    private val layerA: FrameLayout,
    private val layerB: FrameLayout
) {
    private val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_ONE  // playlist duration이 비디오 길이보다 길 때 루프
        volume = 0f                          // 동영상 기본 음소거 — 사이니지에서 의도치 않은 사운드 방지
    }

    private var active: FrameLayout = layerA
    private var standby: FrameLayout = layerB
    private var currentItemId: String? = null
    private var currentItem: PlaylistItemDto? = null  // 현재 표시 중(전환의 출발) 슬라이드

    // 이미지 사전 디코딩 캐시 — IO 스레드에서 decodeFile 후 Main 스레드 전달
    @Volatile private var preloadedBitmap: Bitmap? = null
    @Volatile private var preloadedItemId: String? = null

    /**
     * IO 스레드에서 호출: 다음 슬라이드의 이미지를 미리 디코딩해 캐시에 저장.
     * 비디오 슬롯은 no-op.
     */
    fun preloadImage(item: PlaylistItemDto, file: File) {
        if (item.media.type.lowercase() != "image") return
        if (item.id == preloadedItemId && preloadedBitmap != null) return  // 이미 로드됨

        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        val maxDim = 1920
        var sample = 1
        while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) sample *= 2
        val loadOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = BitmapFactory.decodeFile(file.absolutePath, loadOpts) ?: return
        preloadedBitmap = bmp
        preloadedItemId = item.id
    }

    init {
        // 초기에는 양쪽 레이어 모두 숨김
        active.alpha = 1f
        standby.alpha = 0f
        videoOf(active).visibility = View.GONE
        imageOf(active).visibility = View.GONE
        videoOf(standby).visibility = View.GONE
        imageOf(standby).visibility = View.GONE
    }

    /**
     * 새 슬라이드 표시. 동일 itemId면 무시.
     * @return 실제로 실행된 전환 애니메이션 지속 시간(ms). 즉시 전환이면 0.
     *         호출 측은 이 값 + 버퍼를 최소 대기 시간으로 사용해야 한다.
     */
    fun show(item: PlaylistItemDto, file: File): Long {
        if (item.id == currentItemId) return 0L

        val isFirst = currentItem == null
        val prevItem = currentItem  // 나가는 슬라이드 — 대시보드와 동일하게 outgoing 기준으로 전환 효과 결정
        currentItemId = item.id
        currentItem = item

        // 1. standby 레이어에 새 미디어 로드
        when (item.media.type.lowercase()) {
            "video" -> loadVideo(standby, file)
            "image" -> loadImage(standby, file, item.id)
            else -> return 0L
        }

        // 2. 전환 효과 — 나가는 슬라이드 기준 (최초 슬라이드는 전환 없이 즉시 표시)
        //    반환값: 실제 애니메이션 지속 시간(ms)
        val animDurationMs: Long = if (isFirst) {
            instantSwap()
            0L
        } else {
            val transitionMs = (prevItem?.transitionTime ?: 1000).coerceAtLeast(0).toLong()
            val dir = prevItem?.slideDirection?.lowercase() ?: "right"
            when (prevItem?.transition?.lowercase() ?: "fade") {
                "fade"     -> { fadeToBlack(transitionMs);          transitionMs }
                "dissolve" -> { crossfade(transitionMs);            transitionMs }
                "slide"    -> { slideTransition(transitionMs, dir); transitionMs }
                else       -> { instantSwap();                      0L           }
            }
        }

        // 3. 역할 스왑
        val tmp = active
        active = standby
        standby = tmp

        return animDurationMs
    }

    private fun loadVideo(layer: FrameLayout, file: File) {
        val playerView = videoOf(layer)
        val imageView = imageOf(layer)
        imageView.visibility = View.GONE
        playerView.visibility = View.VISIBLE
        playerView.player = player

        // 이전 player가 다른 layer에 attach돼 있어도 setPlayer로 자동 이동
        videoOf(otherLayer(layer)).player = null

        player.setMediaItem(MediaItem.fromUri(file.toURI().toString()))
        player.prepare()
        player.playWhenReady = true
    }

    private fun loadImage(layer: FrameLayout, file: File, itemId: String) {
        // 비디오 → 이미지 전환 시 ExoPlayer 완전 정지.
        // 그렇지 않으면 비활성 레이어에서 비디오가 계속 디코딩되며 GPU/메모리 점유 →
        // 일부 STB에서 이미지 레이어가 갱신되지 않거나 블랙으로 표시되는 문제 발생.
        if (player.playWhenReady || player.mediaItemCount > 0) {
            player.stop()
            player.clearMediaItems()
        }

        val playerView = videoOf(layer)
        val imageView = imageOf(layer)
        playerView.visibility = View.GONE
        playerView.player = null

        // 반대편 레이어의 PlayerView도 정리 — 잔존 표면이 이미지를 덮는 현상 방지
        val otherPV = videoOf(otherLayer(layer))
        otherPV.visibility = View.GONE
        otherPV.player = null

        imageView.visibility = View.VISIBLE

        // preloadImage()로 IO 스레드에서 미리 디코딩된 비트맵 사용 (있으면).
        // 없으면 Main 스레드에서 동기 디코딩 (폴백).
        val bmp: Bitmap? = if (itemId == preloadedItemId) {
            val b = preloadedBitmap
            preloadedBitmap = null
            preloadedItemId = null
            b
        } else {
            // 폴백: Main 스레드 동기 디코딩 (느린 기기에서 타이밍 drift 유발 가능)
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, opts)
            val maxDim = 1920
            var sample = 1
            while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) sample *= 2
            val loadOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(file.absolutePath, loadOpts)
        }
        imageView.setImageBitmap(bmp)
    }

    /**
     * DISSOLVE — 두 레이어를 동시에 크로스페이드.
     */
    private fun crossfade(durationMs: Long) {
        val prevActive = active
        standby.translationX = 0f
        standby.translationY = 0f
        standby.alpha = 0f
        standby.animate().alpha(1f).setDuration(durationMs).start()
        prevActive.animate().alpha(0f).setDuration(durationMs).withEndAction {
            imageOf(prevActive).setImageBitmap(null)
        }.start()
    }

    /**
     * FADE — 검정으로 사라졌다 새 슬라이드가 나타남 (2단계 순차 페이드).
     * durationMs 절반: 페이드아웃 / 나머지 절반: 페이드인
     *
     * ⚠️ 주의: withEndAction 람다는 fadeToBlack() 반환 후 halfMs 뒤에 실행된다.
     *   그 사이 show()에서 active/standby 가 스왑되므로, 람다 안에서 클래스 멤버
     *   'standby' 를 참조하면 스왑 후의 구(舊) 레이어를 가리키게 된다.
     *   → nextActive 로 신(新) 레이어 객체 참조를 미리 캡처해 사용해야 한다.
     */
    private fun fadeToBlack(durationMs: Long) {
        val prevActive = active
        val nextActive = standby          // 스왑 전에 신 레이어 참조를 val 로 캡처
        val halfMs = (durationMs / 2).coerceAtLeast(100L)

        nextActive.translationX = 0f
        nextActive.translationY = 0f
        nextActive.alpha = 0f

        // 1단계: 현재 → 검정
        prevActive.animate()
            .alpha(0f)
            .setDuration(halfMs)
            .withEndAction {
                // 2단계: 검정 → 새 슬라이드
                // standby 대신 캡처된 nextActive 사용 — 스왑 후에도 신 레이어를 정확히 참조
                nextActive.animate()
                    .alpha(1f)
                    .setDuration(halfMs)
                    .withEndAction { imageOf(prevActive).setImageBitmap(null) }
                    .start()
            }
            .start()
    }

    /**
     * SLIDE — 방향에 따라 새 슬라이드가 밀고 들어오고 이전 슬라이드는 반대 방향으로 빠져나감.
     * dir: "right"(기본) | "left" | "up" | "down"
     *   right → 다음이 오른쪽에서 진입, 이전은 왼쪽으로 퇴장
     *   left  → 다음이 왼쪽에서 진입, 이전은 오른쪽으로 퇴장
     *   up    → 다음이 아래에서 진입, 이전은 위로 퇴장
     *   down  → 다음이 위에서 진입, 이전은 아래로 퇴장
     */
    private fun slideTransition(durationMs: Long, dir: String) {
        val dm = context.resources.displayMetrics
        val screenW = dm.widthPixels.toFloat()
        val screenH = dm.heightPixels.toFloat()
        val prevActive = active
        val interp = DecelerateInterpolator()

        // 진입 시작 위치 (standby) / 퇴장 끝 위치 (prevActive)
        val (inX, inY, outX, outY) = when (dir) {
            "left"  -> arrayOf( -screenW, 0f,  screenW, 0f)
            "up"    -> arrayOf( 0f,  screenH, 0f, -screenH)
            "down"  -> arrayOf( 0f, -screenH, 0f,  screenH)
            else    -> arrayOf(  screenW, 0f, -screenW, 0f)  // "right" 기본
        }

        standby.alpha = 1f
        standby.translationX = inX
        standby.translationY = inY
        standby.animate()
            .translationX(0f).translationY(0f)
            .setDuration(durationMs)
            .setInterpolator(interp)
            .start()

        prevActive.animate()
            .translationX(outX).translationY(outY)
            .setDuration(durationMs)
            .setInterpolator(interp)
            .withEndAction {
                imageOf(prevActive).setImageBitmap(null)
                prevActive.translationX = 0f
                prevActive.translationY = 0f
            }
            .start()
    }

    private fun instantSwap() {
        standby.translationX = 0f
        standby.translationY = 0f
        standby.alpha = 1f
        active.alpha = 0f
        imageOf(active).setImageBitmap(null)
    }

    /** 재부팅/종료 전 호출: 두 레이어를 즉시 숨기고 검은 화면으로 전환 */
    fun showBlack() {
        active.animate().cancel()
        standby.animate().cancel()
        // SurfaceView는 View 알파 컴포지터를 우회하므로 alpha 애니메이션만으로는
        // 비디오 표면이 화면에서 사라지지 않음 → player 분리 + visibility=GONE 으로
        // SurfaceView 홀을 즉시 닫아야 한다 (특히 Android 5.1.1에서 필수)
        player.stop()
        player.clearMediaItems()
        videoOf(active).apply { this.player = null; visibility = View.GONE }
        videoOf(standby).apply { this.player = null; visibility = View.GONE }
        imageOf(active).setImageBitmap(null)
        imageOf(standby).setImageBitmap(null)
        active.alpha = 0f
        standby.alpha = 0f
    }

    private fun videoOf(layer: FrameLayout): PlayerView =
        layer.findViewWithTag("video") as PlayerView

    private fun imageOf(layer: FrameLayout): ImageView =
        layer.findViewWithTag("image") as ImageView

    private fun otherLayer(layer: FrameLayout): FrameLayout =
        if (layer === layerA) layerB else layerA

    fun release() {
        player.release()
    }
}
