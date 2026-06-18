package com.example.studyplan.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the Retrofit-backed [AIApi].
 *
 * The backend is a Spring Boot app listening on port 8080. From the Android
 * emulator, the host machine's localhost is reachable at 10.0.2.2 — not
 * 127.0.0.1, which would point at the emulator itself. Change [BASE_URL] if you
 * run on a physical device (use the host's LAN IP) or deploy the backend.
 */
object BackendApi {

    private const val BASE_URL = "http://10.0.2.2:8080/"

    // AI generation can take a while, so be generous with the read timeout.
    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        )
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val aiApi: AIApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AIApi::class.java)
    }
}
