package com.signagepro.player.api

import retrofit2.http.GET
import retrofit2.http.Path

interface SignageApi {

    @GET("api/devices/{id}")
    suspend fun getDevice(@Path("id") deviceId: String): DeviceDto

    @GET("api/groups/{groupId}/playlist")
    suspend fun getPlaylist(@Path("groupId") groupId: String): PlaylistDto
}
