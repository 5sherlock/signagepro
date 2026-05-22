package com.signagepro.player.engine

import com.signagepro.player.api.PlaylistItemDto
import com.signagepro.player.sync.NtpClient

/**
 * NTP 시각 기반 슬라이드 인덱스 계산기.
 *
 * 핵심 원리:
 *   누적 cycle 시간 = sum(duration_i)
 *   현재 cycle 내 진행 시간 t = (now_ms % cycle) / 1000
 *   t를 슬라이드 duration 으로 누적해서 어디에 위치하는지 찾음
 *
 * 5대 보드가 동일한 NTP 기준 시각을 보면, 동일 시각에 동일 인덱스를 반환.
 * 서버 다운 여부와 무관하게 동기 재생 유지.
 *
 * 이 클래스는 부수효과 없는 순수 계산. 재생/전환 로직은 KioskActivity 측에서 처리.
 */
class PlaylistEngine(
    private val ntp: NtpClient,
    private val selfDeviceId: String
) {

    @Volatile
    private var items: List<PlaylistItemDto> = emptyList()

    /**
     * 새 playlist 적용 — 자기 deviceId에 해당하는 항목만 필터링하고 order 정렬.
     */
    fun setPlaylist(all: List<PlaylistItemDto>) {
        items = all
            .filter { it.targetDeviceId == null || it.targetDeviceId == selfDeviceId }
            .sortedBy { it.order }
    }

    fun current(): SlotState? {
        val list = items
        if (list.isEmpty()) return null

        // 각 슬라이드의 duration (초). 없으면 fallback 10초.
        val durations = list.map { (it.duration ?: 10).coerceAtLeast(1) }
        val cycleSec = durations.sum().toLong()
        if (cycleSec <= 0) return null

        val nowMs = ntp.now()
        val nowSec = nowMs / 1000L
        var posInCycle = (nowSec % cycleSec).toInt()
        if (posInCycle < 0) posInCycle += cycleSec.toInt()

        var idx = 0
        var acc = 0
        for ((i, d) in durations.withIndex()) {
            if (posInCycle < acc + d) {
                idx = i
                break
            }
            acc += d
        }
        val elapsedInSlide = posInCycle - acc
        val remaining = durations[idx] - elapsedInSlide
        val nextSlotEpochMs = (nowSec + remaining) * 1000L
        return SlotState(
            item = list[idx],
            index = idx,
            total = list.size,
            elapsedSec = elapsedInSlide,
            remainingSec = remaining,
            nextSlotEpochMs = nextSlotEpochMs
        )
    }

    data class SlotState(
        val item: PlaylistItemDto,
        val index: Int,
        val total: Int,
        val elapsedSec: Int,
        val remainingSec: Int,
        val nextSlotEpochMs: Long
    )
}
