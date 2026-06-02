package com.signagepro.player.engine

import android.content.Context
import com.signagepro.player.api.ScheduleDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

/**
 * 마지막으로 서버에서 수신한 스케줄 목록을 로컬 파일로 보존.
 * 서버 연결 실패 시에도 캐시된 스케줄로 화면 ON/OFF를 실행할 수 있게 함.
 */
class ScheduleStore(context: Context) {

    private val file = File(context.filesDir, "schedules.json")

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter<List<ScheduleDto>>(
        Types.newParameterizedType(List::class.java, ScheduleDto::class.java)
    ).serializeNulls()

    fun save(schedules: List<ScheduleDto>) {
        runCatching { file.writeText(adapter.toJson(schedules)) }
    }

    fun load(): List<ScheduleDto> {
        if (!file.exists()) return emptyList()
        return runCatching { adapter.fromJson(file.readText()) }.getOrNull() ?: emptyList()
    }
}
