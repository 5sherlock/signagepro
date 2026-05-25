package com.signagepro.player.render

import android.content.Context
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
     */
    fun show(item: PlaylistItemDto, file: File) {
        if (item.id == currentItemId) return

        val isFirst = currentItem == null
        val prevItem = currentItem  // 나가는 슬라이드 — 대시보드와 동일하게 outgoing 기준으로 전환 효과 결정
        currentItemId = item.id
        currentItem = item

        // 1. standby 레이어에 새 미디어 로드
        when (item.media.type.lowercase()) {
            "video" -> loadVideo(standby, file)
            "image" -> loadImage(standby, file)
            else -> return
        }

        // 2. 전환 효과 — 나가는 슬라이드 기준 (최초 슬라이드는 전환 없이 즉시 표시)
        if (isFirst) {
            instantSwap()
        } else {
            val transitionMs = (prevItem?.transitionTime ?: 1000).coerceAtLeast(0).toLong()
            val dir = prevItem?.slideDirection?.lowercase() ?: "right"
            when (prevItem?.transition?.lowercase() ?: "fade") {
                "fade"    -> fadeToBlack(transitionMs)
                "dissolve"-> crossfade(transitionMs)
                "slide"   -> slideTransition(transitionMs, dir)
                else      -> instantSwap()
            }
        }

        // 3. 역할 스왑
        val tmp = active
        active = standby
        standby = tmp
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

    private fun loadImage(layer: FrameLayout, file: File) {
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

        // 다운샘플링하여 OOM 방지
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        val maxDim = 1920
        var sample = 1
        while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) sample *= 2
        val loadOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = BitmapFactory.decodeFile(file.absolutePath, loadOpts)
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
     */
    private fun fadeToBlack(durationMs: Long) {
        val prevActive = active
        val halfMs = (durationMs / 2).coerceAtLeast(100L)

        standby.translationX = 0f
        standby.translationY = 0f
        standby.alpha = 0f

        // 1단계: 현재 → 검정
        prevActive.animate()
            .alpha(0f)
            .setDuration(halfMs)
            .withEndAction {
                // 2단계: 검정 → 새 슬라이드
                standby.animate()
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
