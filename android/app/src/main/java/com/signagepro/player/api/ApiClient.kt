package com.signagepro.player.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 서버와의 REST 통신 진입점.
 * baseUrl이 바뀌면 [rebuild] 호출하여 재생성.
 */
object ApiClient {

    @Volatile
    private var api: SignageApi? = null

    @Volatile
    private var currentBaseUrl: String? = null

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun get(baseUrl: String): SignageApi {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cached = api
        if (cached != null && currentBaseUrl == normalized) return cached
        return rebuild(normalized)
    }

    @Synchronized
    private fun rebuild(baseUrl: String): SignageApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        val instance = retrofit.create(SignageApi::class.java)
        api = instance
        currentBaseUrl = baseUrl
        return instance
    }

    /**
     * 미디어 다운로드용 OkHttpClient — 큰 파일도 받을 수 있게 동일 인스턴스 재사용.
     */
    fun http(): OkHttpClient = okHttp
}
