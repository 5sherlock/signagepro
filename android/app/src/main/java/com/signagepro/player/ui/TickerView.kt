package com.signagepro.player.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.signagepro.player.api.TickerConfigDto
import com.signagepro.player.sync.NtpClient

/**
 * NTP 기반 멀티스크린 연속 자막 뷰.
 *
 * group 모드:
 *   가상 캔버스 너비 = screenW × totalDevices
 *   텍스트 가상 X     = (totalW + textW) × posInCycle / cycleLen  (rtl 기준)
 *   로컬 X            = 가상X - deviceIndex × screenW
 *
 * individual 모드:
 *   일반 단일화면 스크롤 (deviceIndex=0, totalDevices=1 과 동일)
 *
 * postInvalidateOnAnimation() 으로 매 vsync 마다 재계산 → 서버 없이도 NTP 기준 유지.
 */
class TickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var config: TickerConfigDto? = null
    private var ntp: NtpClient? = null

    /** 대시보드 동기화용: 마지막으로 계산된 사이클 길이(ms). 0이면 미초기화. */
    @Volatile var lastCycleMs: Long = 0L
        private set

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.LEFT }
    private val bgPaint   = Paint(Paint.ANTI_ALIAS_FLAG)

    // 폰트 캐시: assets/fonts/ 에서 한 번만 로드
    private val typefaceCache = mutableMapOf<String, Typeface>()

    fun configure(cfg: TickerConfigDto, ntpClient: NtpClient) {
        config = cfg
        ntp    = ntpClient

        // 폰트 설정
        val tf = loadTypeface(cfg.fontFamily)
        val style = when {
            cfg.fontBold && cfg.fontItalic -> Typeface.BOLD_ITALIC
            cfg.fontBold                   -> Typeface.BOLD
            cfg.fontItalic                 -> Typeface.ITALIC
            else                           -> Typeface.NORMAL
        }
        textPaint.typeface = Typeface.create(tf, style)
        textPaint.textSize = cfg.fontSize.toFloat()
        textPaint.color    = parseColor(cfg.textColor, Color.WHITE)

        val bgAlpha = (cfg.bgOpacity.coerceIn(0, 100) * 255 / 100)
        bgPaint.color = parseColor(cfg.bgColor, Color.BLACK)
        bgPaint.alpha = bgAlpha

        visibility = VISIBLE
        invalidate()
    }

    fun clear() {
        config = null
        visibility = GONE
    }

    override fun onDraw(canvas: Canvas) {
        val cfg = config ?: return
        val ntpClient = ntp ?: return
        if (cfg.text.isBlank()) return

        val screenW  = width.toFloat()
        val barH     = cfg.fontSize * 2.0f
        val barTop   = when (cfg.position) {
            "top"    -> 0f
            "center" -> (height - barH) / 2f
            else     -> height - barH          // bottom
        }

        // 배경 바
        bgPaint.alpha = (cfg.bgOpacity.coerceIn(0, 100) * 255 / 100)
        canvas.drawRect(0f, barTop, screenW, barTop + barH, bgPaint)

        // 텍스트 X 계산
        val textW     = textPaint.measureText(cfg.text)
        val totalDevices = if (cfg.mode == "group") cfg.totalDevices.coerceAtLeast(1) else 1
        val totalW    = screenW * totalDevices
        val cyclePx   = totalW + textW
        val cycleMs   = (cyclePx / cfg.speed.coerceAtLeast(1) * 1000L).toLong()
        lastCycleMs   = cycleMs

        val nowMs     = ntpClient.now()
        val posInCycle = (nowMs % cycleMs).toFloat() / 1000f * cfg.speed  // px

        val virtualX = if (cfg.direction == "ltr") {
            -textW + posInCycle                        // 좌→우
        } else {
            totalW - posInCycle                        // 우→좌 (기본)
        }

        val deviceIdx = if (cfg.mode == "group") cfg.deviceIndex else 0
        val localX    = virtualX - deviceIdx * screenW

        // 화면 범위 밖이면 그리기 생략 (성능 최적화)
        if (localX > screenW || localX + textW < 0f) {
            postInvalidateOnAnimation()
            return
        }

        val textBaseline = barTop + barH * 0.68f
        canvas.drawText(cfg.text, localX, textBaseline, textPaint)

        postInvalidateOnAnimation()
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────────────────

    private fun loadTypeface(family: String): Typeface {
        return typefaceCache.getOrPut(family) {
            try {
                Typeface.createFromAsset(context.assets, "fonts/$family.ttf")
            } catch (e: Exception) {
                Typeface.SANS_SERIF
            }
        }
    }

    private fun parseColor(hex: String, fallback: Int): Int {
        return try { Color.parseColor(hex) } catch (e: Exception) { fallback }
    }
}
