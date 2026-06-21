package com.signagepro.player.sync

import kotlin.math.abs

/**
 * 멀티스크린 비디오월 재생 위치 동기 계산기.
 *
 * 핵심 원리 (PlaylistEngine / TickerView 와 동일):
 *   목표 위치 = (now() - anchor) % durationMs
 *   동일 NTP 기준 시각을 보는 5대가 동일 시각에 동일 목표 위치를 계산 → 프레임 정렬.
 *
 * 이 클래스는 부수효과 없는 순수 계산.
 *   evaluate() 가 "지금 어디로 가야 하고(targetPosMs), 현재 위치와의 드리프트가
 *   얼마이며(driftMs), 어떻게 보정할지(action/speed)"만 결정한다.
 *   실제 ExoPlayer seekTo()/setPlaybackParameters() 는 호출 측(렌더러)에서 수행.
 *
 * 보정 전략 (서버 없이도 NTP 기준 유지):
 *   - |drift| ≤ SOFT  : NONE (1.0x)            — 충분히 맞음, 그대로
 *   - SOFT < |drift| ≤ HARD : 속도 미세조정      — 앞서면 느리게, 뒤지면 빠르게
 *       하드 seek 의 가시적 점프를 피하고 슬그머니 수렴. 음소거라 피치 변화 무해.
 *   - |drift| > HARD  : HARD_SEEK              — 시작 직후/큰 이탈 시 즉시 목표로 점프
 */
class VideoWallSync(private val ntp: NtpClient) {

    enum class Action { NONE, NUDGE_FASTER, NUDGE_SLOWER, HARD_SEEK }

    data class Correction(
        val action: Action,
        val targetPosMs: Long,
        val driftMs: Long,      // 현재 - 목표 (양수=기기가 앞섬, 음수=뒤짐). 루프 경계 보정됨.
        val speed: Float        // 적용할 재생속도 (NONE/HARD_SEEK = 1.0x)
    )

    /**
     * @param durationMs   슬라이스 영상 길이(ms). 0 이하면 NONE.
     * @param currentPosMs 현재 ExoPlayer 재생 위치(ms).
     * @param anchorMs     공통 시작 기준 시각(epoch ms). 0이면 순수 now()%duration.
     */
    fun evaluate(durationMs: Long, currentPosMs: Long, anchorMs: Long = 0L): Correction {
        if (durationMs <= 0L) return Correction(Action.NONE, 0L, 0L, 1f)

        var target = (ntp.now() - anchorMs) % durationMs
        if (target < 0L) target += durationMs

        // 루프 원형 타임라인상 최단 부호드리프트로 보정.
        // (예: 기기 pos=50, target=D-50 이면 raw=-(D-100) 이지만 실제론 +100ms 앞선 것)
        var drift = currentPosMs - target
        val half = durationMs / 2
        if (drift > half) drift -= durationMs
        if (drift < -half) drift += durationMs

        val a = abs(drift)
        return when {
            a > HARD_THRESHOLD_MS               -> Correction(Action.HARD_SEEK, target, drift, 1f)
            a > SOFT_THRESHOLD_MS && drift > 0  -> Correction(Action.NUDGE_SLOWER, target, drift, 1f - NUDGE)
            a > SOFT_THRESHOLD_MS && drift < 0  -> Correction(Action.NUDGE_FASTER, target, drift, 1f + NUDGE)
            else                                -> Correction(Action.NONE, target, drift, 1f)
        }
    }

    companion object {
        const val SOFT_THRESHOLD_MS = 40L    // 30fps 한 프레임(33ms) 근방 — 이내면 정렬로 간주
        const val HARD_THRESHOLD_MS = 400L   // 이 이상 어긋나면 속도보정으론 느려 즉시 seek
        const val NUDGE = 0.03f              // ±3% 속도 — 시각적으로 인지 불가, 음소거라 피치 무관
    }
}
