package com.example.andopsi.data

interface AudioExtractor {
    fun detectAudioCodec(inputPath: String): String?
    fun extractAudio(inputPath: String, outputPath: String): Boolean
}