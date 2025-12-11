package com.example.andopsi.network

import com.example.andopsi.model.TranscriptResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TranscriptApiService {
    // This points to YOUR backend, e.g., "http://10.0.2.2:5000/get_transcript"
    // 10.0.2.2 is the localhost alias for Android Emulator
    @GET("get_transcript")
    suspend fun getTranscript(
        @Query("video_id") videoId: String
    ): Response<TranscriptResponse>
}