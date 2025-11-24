package com.example.andopsi.model

import android.provider.MediaStore
import kotlinx.serialization.Serializable

@Serializable
data class YouTubeVideosResponse(val items: List<VideoItem>)

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

// your clean domain model
@Serializable
data class Video(
    val id: String,
    val title: String,
    val description: String,
    val publishedAt: String,
    val thumbnailUrl: String
)

// mapper
fun VideoItem.toVideo(): Video = Video(
    id = id,
    title = snippet.title,
    description = snippet.description,
    publishedAt = snippet.publishedAt,
    thumbnailUrl = snippet.thumbnails.high.url
)
