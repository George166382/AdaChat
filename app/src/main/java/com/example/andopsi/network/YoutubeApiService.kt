package com.example.andopsi.network

import com.example.andopsi.model.Video
import retrofit2.Response

import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {
    @GET("videos")
    suspend fun getVideo(
        @Query("part") part: String = "snippet,contentDetails,statistics",
        @Query("id") videoId: String,
        @Query("key") apiKey: String
    ): Response<Video>
}

