package com.signagepro.player.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.signagepro.player.api.PlaylistItemDto
import com.signagepro.player.sync.VideoWallSync
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
    private var player: ExoPlayer = buildPlayer()

    private fun buildPlayer(): ExoPlayer = ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_ONE  // playlist duration이 비디오 길이보다 길 때 루프
        volume = 1f                          // 플레이어는 풀볼륨, 실제 출력은 OS 미디어 볼륨(STREAM_MUSIC)으로 제어.
                                             // OS 볼륨은 기본 음소거(최초 1회)라 의도치 않은 사운드 없음 + 관제에서 올리면 재생/이퀄라이저 동작.
        addListener(object : Player.Listener {
            override fun onRenderedFirstFrame() { firstFrameRendered = true }
        })
    }

    // 앱 재시작과 동일하게 ExoPlayer를 새로 만든다. 비디오→비디오 교체(배포) 시 기존 디코더가
    // 스톨해 "배포 후 화면 멈춤, 앱 재시작해야 풀림"이 발생 → 재시작 없이 플레이어 재생성으로 해소.
    private fun recreatePlayer() {
        try { player.release() } catch (e: Exception) {}
        player = buildPlayer()
    }

    private var active: FrameLayout = layerA
    private var standby: FrameLayout = layerB
    private var currentItemId: String? = null
    private var currentItem: PlaylistItemDto? = null  // 현재 표시 중(전환의 출발) 슬라이드

    // 이미지 사전 디코딩 캐시 — IO 스레드에서 decodeFile 후 Main 스레드 전달
    @Volatile private var preloadedBitmap: Bitmap? = null
    @Volatile private var preloadedItemId: String? = null

    // 멈춤(스톨) 워치독 상태 — 현재 재생 중인 비디오 파일과 위치 정지 감지용
    private var currentVideoFile: File? = null
    private var currentVideoView: PlayerView? = null   // 검은화면 복구 시 표면 재부착 대상
    private var lastStallPos: Long = -1L
    private var stallTicks: Int = 0
    // 검은-첫프레임(표면 미렌더) 감지용 — onRenderedFirstFrame 콜백으로 확정
    @Volatile private var firstFrameRendered: Boolean = false
    private var videoLoadElapsed: Long = 0L
    private var blackRecoveryTried: Boolean = false

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
        // 첫 프레임 렌더 리스너는 buildPlayer()에서 부착됨(플레이어 재생성 시에도 유지).
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
        recreatePlayer()  // 앱 재시작과 동일하게 새 ExoPlayer — 비디오 교체(배포) 시 디코더 스톨(멈춤) 방지
        val playerView = videoOf(layer)
        val imageView = imageOf(layer)
        imageView.visibility = View.GONE
        playerView.visibility = View.VISIBLE
        playerView.player = player

        // 이전 player가 다른 layer에 attach돼 있어도 setPlayer로 자동 이동
        videoOf(otherLayer(layer)).player = null

        // (새 ExoPlayer라 속도 1.0x·미디어 없음 — 별도 리셋 불필요)
        player.setMediaItem(MediaItem.fromUri(file.toURI().toString()))
        player.prepare()
        player.playWhenReady = true

        // 스톨/검은화면 워치독 대상 갱신
        currentVideoFile = file
        currentVideoView = playerView
        lastStallPos = -1L
        stallTicks = 0
        firstFrameRendered = false
        blackRecoveryTried = false
        videoLoadElapsed = SystemClock.elapsedRealtime()

        // 일부 RK STB는 첫 프레임을 렌더링하지 않고 검은 화면으로 멈추는 사례가 있다.
        // prepare 직후 짧게 지연 후 같은 위치로 seek를 한 번 줘서 디코더 표면 렌더를 강제 kick.
        playerView.postDelayed({
            if (player.playbackState == Player.STATE_READY) {
                player.seekTo(player.currentPosition)
            }
        }, FIRST_FRAME_KICK_MS)
    }

    private fun loadImage(layer: FrameLayout, file: File, itemId: String) {
        // 비디오 → 이미지 전환 시 ExoPlayer 완전 정지.
        // 그렇지 않으면 비활성 레이어에서 비디오가 계속 디코딩되며 GPU/메모리 점유 →
        // 일부 STB에서 이미지 레이어가 갱신되지 않거나 블랙으로 표시되는 문제 발생.
        if (player.playWhenReady || player.mediaItemCount > 0) {
            player.stop()
            player.clearMediaItems()
        }
        // 이미지 슬롯 — 비디오 스톨/검은화면 워치독 비활성화
        currentVideoFile = null
        currentVideoView = null
        lastStallPos = -1L
        stallTicks = 0
        firstFrameRendered = false
        blackRecoveryTried = false

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

    // ── 멀티스크린 비디오월 동기 ───────────────────────────────────────────────
    // VideoWallSync(순수계산)가 산출한 보정을 ExoPlayer에 적용한다.
    // 모든 호출은 Main 스레드에서 이뤄져야 한다(ExoPlayer 제약) — PlayerCoordinator 루프가 Main.

    /** 현재 재생 위치(ms). 미준비/이미지 슬롯이면 0. */
    fun videoPositionMs(): Long {
        val p = player.currentPosition
        return if (p < 0) 0L else p
    }

    /** 현재 비디오 길이(ms). 아직 prepare 전이라 미상이면 0(보정 보류 신호). */
    fun videoDurationMs(): Long {
        val d = player.duration
        return if (d == C.TIME_UNSET || d < 0) 0L else d
    }

    /**
     * VideoWallSync.evaluate() 결과를 적용.
     *   HARD_SEEK   → 속도 1.0 복귀 + 목표 위치로 즉시 seek
     *   SPEED_NUDGE → 재생속도 ±NUDGE 로 부드럽게 수렴
     *   NONE        → 속도 1.0 복귀(잔존 nudge 제거)
     */
    fun applyWallCorrection(c: VideoWallSync.Correction) {
        when (c.action) {
            VideoWallSync.Action.HARD_SEEK -> {
                if (player.playbackParameters.speed != 1f) player.playbackParameters = PlaybackParameters(1f)
                player.seekTo(c.targetPosMs)
            }
            VideoWallSync.Action.SPEED_NUDGE -> {
                if (player.playbackParameters.speed != c.speed) player.playbackParameters = PlaybackParameters(c.speed)
            }
            VideoWallSync.Action.NONE -> {
                if (player.playbackParameters.speed != 1f) player.playbackParameters = PlaybackParameters(1f)
            }
        }
    }

    /**
     * 비디오 멈춤(스톨) 자가 복구. PlayerCoordinator 워치독이 주기적으로 Main 스레드에서 호출.
     * 재생해야 하는 비디오인데 재생위치가 STALL_TICKS회 연속 안 늘면 멈춤으로 판단하고
     * 현재 파일을 재prepare(같은 위치 복귀)해 앱 재시작 없이 되살린다.
     * 일부 RK STB에서 영상 시작/도중 검은 화면으로 굳는 현상 대응.
     */
    fun recoverIfStalled(hdmiConnected: Boolean) {
        val file = currentVideoFile ?: return
        // HDMI 미연결(헤드리스)이면 복구 무의미 + 유효 표면이 없어 재로드 churn만 발생 → 건너뜀.
        if (!hdmiConnected) { stallTicks = 0; lastStallPos = -1L; return }
        if (player.mediaItemCount == 0 || !player.playWhenReady) { stallTicks = 0; lastStallPos = -1L; return }
        when (player.playbackState) {
            Player.STATE_BUFFERING -> { /* 버퍼링 중 — 정상 대기 */ }
            Player.STATE_READY -> {
                val pos = player.currentPosition
                if (pos == lastStallPos) {
                    // (a) 재생위치 정지 = 멈춤 → 재prepare
                    if (++stallTicks >= STALL_TICKS) reloadCurrentVideo(file, pos)
                } else {
                    stallTicks = 0
                    lastStallPos = pos
                    // (b) 위치는 흐르는데 첫 프레임이 안 떠 검은 화면(일부 RK STB 표면 미바인딩).
                    //     1차: 표면 재부착(가벼움), 그래도 안 뜨면 2차: 전체 재로드.
                    if (!firstFrameRendered && player.playWhenReady &&
                        SystemClock.elapsedRealtime() - videoLoadElapsed > FIRST_FRAME_TIMEOUT_MS) {
                        if (!blackRecoveryTried) {
                            blackRecoveryTried = true
                            currentVideoView?.let { it.player = null; it.player = player }  // 표면 재부착
                            videoLoadElapsed = SystemClock.elapsedRealtime()                // 재시도 유예 리셋
                        } else {
                            reloadCurrentVideo(file, pos)
                        }
                    }
                }
            }
            Player.STATE_IDLE, Player.STATE_ENDED -> {
                // 재생해야 하는데 IDLE/ENDED로 멈춤 → 재prepare
                if (++stallTicks >= STALL_TICKS) reloadCurrentVideo(file, player.currentPosition.coerceAtLeast(0L))
            }
        }
    }

    /**
     * HDMI 재연결(헤드리스→연결) 시 호출. 새 디스플레이 표면에 영상이 다시 그려지도록
     * PlayerView 표면을 재부착하고 현재 영상을 재로드한다.
     * (RK STB는 HDMI 핫플러그 후 기존 surface가 죽은 채 자동 재렌더되지 않는 경우가 있음)
     */
    fun onHdmiReconnected() {
        val file = currentVideoFile ?: return
        val pos = player.currentPosition.coerceAtLeast(0L)
        currentVideoView?.let { it.player = null; it.player = player }  // 새 표면 바인딩
        reloadCurrentVideo(file, pos)
    }

    private fun reloadCurrentVideo(file: File, resumePos: Long) {
        if (player.playbackParameters.speed != 1f) player.playbackParameters = PlaybackParameters(1f)
        player.setMediaItem(MediaItem.fromUri(file.toURI().toString()))
        player.prepare()
        if (resumePos > 0L) player.seekTo(resumePos)
        player.playWhenReady = true
        stallTicks = 0
        lastStallPos = -1L
        firstFrameRendered = false
        blackRecoveryTried = false
        videoLoadElapsed = SystemClock.elapsedRealtime()
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

    companion object {
        // prepare 직후 첫 프레임 렌더 강제 kick 지연(ms)
        private const val FIRST_FRAME_KICK_MS = 400L
        // 재생위치가 이 횟수만큼 연속 정지하면 멈춤으로 판단(워치독 주기 × STALL_TICKS)
        private const val STALL_TICKS = 2
        // 로드 후 이 시간까지 첫 프레임이 안 뜨면 검은화면으로 판단(표면 재부착 트리거)
        private const val FIRST_FRAME_TIMEOUT_MS = 1500L
    }
}
