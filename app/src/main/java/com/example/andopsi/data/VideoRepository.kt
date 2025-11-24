package com.example.andopsi.data

import com.example.andopsi.model.Video
import com.example.andopsi.model.YouTubeVideosResponse
import com.example.andopsi.network.YouTubeApiService
import retrofit2.Response

interface VideoRepository {
    suspend fun getVideo(videoId: String): Response<YouTubeVideosResponse>
}




/**
 * Network Implementation of Repository
 */
class NetworkVideoRepository(
    private val videoApiService: YouTubeApiService,
    private val apiKey: String
) : VideoRepository {


    override suspend fun getVideo(videoId: String): Response<YouTubeVideosResponse> {
        val response = videoApiService.getVideo(videoId = videoId, apiKey = apiKey)
        return response
    }
}
