package com.signagepro.player.render

import android.content.Context
import android.graphics.BitmapFactory
import android.view.View
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
        volume = 1f
    }

    private var active: FrameLayout = layerA
    private var standby: FrameLayout = layerB
    private var currentItemId: String? = null

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
        currentItemId = item.id

        // 1. standby 레이어에 새 미디어 로드
        when (item.media.type.lowercase()) {
            "video" -> loadVideo(standby, file)
            "image" -> loadImage(standby, file)
            else -> return
        }

        val transitionMs = (item.transitionTime ?: 1000).coerceAtLeast(0).toLong()
        val type = item.transition?.lowercase() ?: "fade"

        // 2. 크로스페이드
        when (type) {
            "fade" -> crossfade(transitionMs)
            else -> instantSwap()  // "none" 등은 하드컷
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
        val playerView = videoOf(layer)
        val imageView = imageOf(layer)
        playerView.visibility = View.GONE
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

    private fun crossfade(durationMs: Long) {
        standby.alpha = 0f
        standby.animate().alpha(1f).setDuration(durationMs).start()
        active.animate().alpha(0f).setDuration(durationMs).withEndAction {
            // 이전 active 정리 — 메모리 절약
            val playerView = videoOf(active)
            if (playerView.player != null) {
                // 새 player가 standby로 옮겨졌으므로 여긴 이미 detach 상태일 것
            }
            imageOf(active).setImageBitmap(null)
        }.start()
    }

    private fun instantSwap() {
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
