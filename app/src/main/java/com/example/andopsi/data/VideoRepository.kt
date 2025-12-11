package com.example.andopsi.data

import com.example.andopsi.model.TranscriptResponse
import com.example.andopsi.model.Video
import com.example.andopsi.network.TranscriptApiService
import com.example.andopsi.network.YouTubeApiService
import retrofit2.Response

interface VideoRepository {
    suspend fun getVideo(videoId: String): Response<Video>
    // ADD THIS:
   // suspend fun getTranscript(videoId: String): Response<TranscriptResponse>
}




/**
 * Network Implementation of Repository
 */
class NetworkVideoRepository(
    private val videoApiService: YouTubeApiService,
    //private val transcriptApiService: TranscriptApiService, // Inject the new service
    private val apiKey: String
) : VideoRepository {


    override suspend fun getVideo(videoId: String): Response<Video> {
        val response = videoApiService.getVideo(videoId = videoId, apiKey = apiKey)
        return response
    }
   /* override suspend fun getTranscript(videoId: String): Response<TranscriptResponse> {
        return transcriptApiService.getTranscript(videoId)
    }*/
}
