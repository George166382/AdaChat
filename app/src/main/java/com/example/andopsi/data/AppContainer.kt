// kotlin
package com.example.andopsi.data

import com.example.andopsi.network.YouTubeApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import okhttp3.MediaType.Companion.toMediaType
import com.example.andopsi.BuildConfig

interface AppContainer {
    val videoRepository: VideoRepository
}

class DefaultAppContainer : AppContainer {
    private val baseUrl = "https://www.googleapis.com/youtube/v3/"

    // Configure Json to ignore unknown keys in responses
    private val json = Json { ignoreUnknownKeys = true }

    private val retrofit: Retrofit = Retrofit.Builder()
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .baseUrl(baseUrl)
        .build()

    private val retrofitService: YouTubeApiService by lazy {
        retrofit.create(YouTubeApiService::class.java)
    }

    override val videoRepository: VideoRepository by lazy {
        NetworkVideoRepository(retrofitService, BuildConfig.YOUTUBE_API_KEY)
    }
}