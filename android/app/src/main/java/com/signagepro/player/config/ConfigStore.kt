package com.signagepro.player.config

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences 래퍼.
 * 부팅 시 필요한 deviceId, serverUrl, 마지막 playlist 해시 등을 보관.
 */
class ConfigStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var deviceId: String?
        get() = prefs.getString(KEY_DEVICE_ID, null)
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    var serverUrl: String?
        get() = prefs.getString(KEY_SERVER_URL, null)
        // 원격 server_url 변경 직후 자가 재시작(killProcess)이 따라오므로 apply(비동기)는
        // 디스크 반영 전에 프로세스가 죽어 유실될 수 있다 → commit(동기)으로 저장 보장.
        set(value) { prefs.edit().putString(KEY_SERVER_URL, value).commit() }

    var deviceSecret: String?
        get() = prefs.getString(KEY_DEVICE_SECRET, null)
        set(value) = prefs.edit().putString(KEY_DEVICE_SECRET, value).apply()

    var lastPlaylistHash: String?
        get() = prefs.getString(KEY_PLAYLIST_HASH, null)
        set(value) = prefs.edit().putString(KEY_PLAYLIST_HASH, value).apply()

    fun isConfigured(): Boolean =
        !deviceId.isNullOrBlank() && !serverUrl.isNullOrBlank()

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "signagepro_player"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_DEVICE_SECRET = "device_secret"
        private const val KEY_PLAYLIST_HASH = "playlist_hash"
    }
}
