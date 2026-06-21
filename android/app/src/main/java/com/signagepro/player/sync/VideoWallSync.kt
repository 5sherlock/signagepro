package com.signagepro.player.sync

import kotlin.math.abs

/**
 * 멀티스크린 비디오월 재생 위치 동기 계산기.
 *
 * 핵심 원리 (PlaylistEngine / TickerView 와 동일):
 *   목표 위치 = (now() - anchor) % durationMs
 *   동일 NTP 기준 시각을 보는 기기들이 동일 목표 위치로 수렴 → 프레임 정렬.
 *
 * 보정 전략 (2단계):
 *   - |drift| 큼(> SEEK_THRESHOLD)   → HARD_SEEK: 목표로 즉시 점프(부팅·스톨복구·큰 이탈).
 *   - |drift| 작음(데드밴드~임계 사이) → SPEED_NUDGE: 재생속도 ±NUDGE 로 부드럽게 수렴.
 *       (점프 없이 흘러서 같은 오차라도 훨씬 동기처럼 보임 — 실측상 seek-only보다 우수)
 *   - 데드밴드 이내                    → NONE: 속도 1.0 복귀.
 *
 * ⚠️ 속도값이 ExoPlayer 인스턴스에 남으므로, NONE/HARD_SEEK 시 렌더러가 반드시 1.0으로
 *    되돌려야 한다(잔존 속도로 영상이 멈춘 채 유지되는 문제 방지). 멈춤 자체는 별도
 *    워치독(MediaRenderer.recoverIfStalled)이 재prepare로 복구.
 *
 * 이 클래스는 부수효과 없는 순수 계산. 실제 seek/속도 적용은 호출 측(렌더러)이 수행.
 */
class VideoWallSync(private val ntp: NtpClient) {

    enum class Action { NONE, SPEED_NUDGE, HARD_SEEK }

    data class Correction(
        val action: Action,
        val targetPosMs: Long,
        val driftMs: Long,      // 현재 - 목표 (양수=앞섬, 음수=뒤짐). 루프 경계 보정됨.
        val speed: Float = 1f   // SPEED_NUDGE 시 적용할 재생속도
    )

    /**
     * @param durationMs   슬라이스 영상 길이(ms). 0 이하면 NONE.
     * @param currentPosMs 현재 ExoPlayer 재생 위치(ms).
     * @param anchorMs     공통 시작 기준 시각(epoch ms). 0이면 순수 now()%duration.
     */
    fun evaluate(durationMs: Long, currentPosMs: Long, anchorMs: Long = 0L): Correction {
        if (durationMs <= 0L) return Correction(Action.NONE, 0L, 0L)

        var target = (ntp.now() - anchorMs) % durationMs
        if (target < 0L) target += durationMs

        // 루프 원형 타임라인상 최단 부호드리프트로 보정.
        var drift = currentPosMs - target
        val half = durationMs / 2
        if (drift > half) drift -= durationMs
        if (drift < -half) drift += durationMs

        val a = abs(drift)
        return when {
            a > SEEK_THRESHOLD_MS -> Correction(Action.HARD_SEEK, target, drift, 1f)
            a > NUDGE_DEADBAND_MS -> {
                // 뒤지면(drift<0) 빠르게, 앞서면(drift>0) 느리게.
                val speed = if (drift < 0) 1f + NUDGE else 1f - NUDGE
                Correction(Action.SPEED_NUDGE, target, drift, speed)
            }
            else -> Correction(Action.NONE, target, drift, 1f)
        }
    }

    companion object {
        // 이 이상 어긋나면 즉시 seek(점프). 그 이하는 속도 nudge로 부드럽게 수렴.
        const val SEEK_THRESHOLD_MS = 1000L
        // 이 이내면 보정 안 함(속도 1.0). 잦은 미세 nudge 방지.
        const val NUDGE_DEADBAND_MS = 20L
        // 재생속도 미세조정 폭(±3%). 임베디드 디코더 호환 위해 과하지 않게.
        const val NUDGE = 0.03f
    }
}
