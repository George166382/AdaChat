package com.example.andopsi.model

import android.provider.MediaStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Video(val items: List<VideoItem>)

@Serializable
data class VideoItem(
    val id: String,
    val snippet: Snippet
)

@Serializable
data class Snippet(
    val title: String,
    val description: String,
    val publishedAt: String,
    val thumbnails: Thumbnails
)

@Serializable
data class Thumbnails(val high: Thumbnail)
@Serializable
data class Thumbnail(val url: String)


@Serializable
data class TranscriptResponse(
    @SerialName("video_id") val videoId: String,
    @SerialName("full_text") val fullText: String,
    @SerialName("segments") val segments: List<TranscriptSegment>?
)

@Serializable
data class TranscriptSegment(
    val text: String,
    val start: Double,
    val duration: Double
)


