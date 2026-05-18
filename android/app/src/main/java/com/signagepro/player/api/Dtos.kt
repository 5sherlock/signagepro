package com.signagepro.player.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** GET /api/devices/:id */
@JsonClass(generateAdapter = true)
data class DeviceDto(
    val id: String,
    val name: String,
    val groupId: String?,
    val storeId: String?,
    val group: GroupDto?,
    val store: StoreDto?
)

@JsonClass(generateAdapter = true)
data class GroupDto(val id: String, val name: String)

@JsonClass(generateAdapter = true)
data class StoreDto(val id: String, val name: String)

/** GET /api/groups/:groupId/playlist */
@JsonClass(generateAdapter = true)
data class PlaylistDto(
    val id: String?,
    val name: String?,
    val medias: List<PlaylistItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PlaylistItemDto(
    val id: String,
    val mediaId: String,
    val order: Int,
    val duration: Int?,
    val targetDeviceId: String?,
    val transition: String?,
    val transitionTime: Int?,
    val media: MediaDto
)

@JsonClass(generateAdapter = true)
data class MediaDto(
    val id: String,
    val filename: String,
    val path: String,           // 예: "/uploads/xxx.mp4"
    val type: String,           // "video" | "image"
    val size: Int?,
    val hash: String?           // SHA-256
)
