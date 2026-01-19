package com.example.andopsi.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*


class AudioExtractorTest {

    @Test
    fun testCodecDetection_nonExistentFile() {
        val extractor = FakeAudioExtractor()
        val codec = extractor.detectAudioCodec("/sdcard/non_existent_file.mp4")
        assertNull(codec)
    }

    @Test
    fun testCodecDetection_validFile() {
        val extractor = FakeAudioExtractor()
        val codec = extractor.detectAudioCodec("D:\\projs\\Portofoliu_Proiecte\\app\\sampledata\\video.mp4")
        assertEquals("aac", codec)
    }

    @Test
    fun testExtractAudio_success() {
        val extractor = FakeAudioExtractor()
        extractor.shouldSucceed = true
        val result = extractor.extractAudio("/input.mp4", "/output.aac")
        assertTrue(result)
    }

    @Test
    fun testExtractAudio_failure() {
        val extractor = FakeAudioExtractor()
        extractor.shouldSucceed = false
        val result = extractor.extractAudio("/input.mp4", "/output.aac")
        assertFalse(result)
    }
}