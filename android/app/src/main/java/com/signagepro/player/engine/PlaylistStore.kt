package com.signagepro.player.engine

import android.content.Context
import com.signagepro.player.api.PlaylistDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

/**
 * 마지막으로 받은 playlist를 로컬 파일로 보존.
 * 서버 다운 상태로 부팅해도 캐시된 playlist로 재생을 이어가게 함.
 */
class PlaylistStore(context: Context) {

    private val file: File = File(context.filesDir, "playlist.json")

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(PlaylistDto::class.java).serializeNulls()

    fun save(playlist: PlaylistDto) {
        runCatching { file.writeText(adapter.toJson(playlist)) }
    }

    fun load(): PlaylistDto? {
        if (!file.exists()) return null
        return runCatching { adapter.fromJson(file.readText()) }.getOrNull()
    }

    fun clear() { file.delete() }
}
