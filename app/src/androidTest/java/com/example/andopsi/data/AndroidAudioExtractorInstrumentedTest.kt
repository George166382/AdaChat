package com.example.andopsi.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class AndroidAudioExtractorInstrumentedTest {

    private lateinit var testVideoFile: File
    private lateinit var outputAudioFile: File
    private val extractor = AndroidAudioExtractor()

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Copy test video from assets to cache directory
        testVideoFile = File(context.cacheDir, "test_video.mp4")
        context.assets.open("video.mp4").use { input ->
            FileOutputStream(testVideoFile).use { output ->
                input.copyTo(output)
            }
        }

        // Prepare output file path
        outputAudioFile = File(context.cacheDir, "output_audio.aac")

        // ADD THIS: Force delete the old output file if it exists
        /*if (outputAudioFile.exists()) {
            outputAudioFile.delete()
        }*/
    }

    @After
    fun cleanup() {
        // Clean up test files
        if (testVideoFile.exists()) testVideoFile.delete()
        //if (outputAudioFile.exists()) outputAudioFile.delete()

        println("Audio file saved at: ${outputAudioFile.absolutePath}")
    }

    @Test
    fun testDetectAudioCodec_withRealVideo() {
        val codec = extractor.detectAudioCodec(testVideoFile.absolutePath)

        assertNotNull("Codec should be detected from video file", codec)
        println("Detected codec: $codec")

        // Common audio codecs in MP4 files
        assertTrue(
            "Codec should be a valid audio codec",
            codec in listOf("aac", "mp3", "opus", "vorbis", "ac3")
        )
    }

    @Test
    fun testDetectAudioCodec_nonExistentFile() {
        val codec = extractor.detectAudioCodec("/path/to/nonexistent/file.mp4")

        // Should return null for non-existent files
        assertNull("Non-existent file should return null codec", codec)
    }

    @Test
    fun testExtractAudio_withRealVideo() {
        val success = extractor.extractAudio(
            testVideoFile.absolutePath,
            outputAudioFile.absolutePath
        )

        assertTrue("Audio extraction should succeed", success)
        assertTrue("Output audio file should exist", outputAudioFile.exists())
        assertTrue("Output audio file should not be empty", outputAudioFile.length() > 0)

        println("Output file size: ${outputAudioFile.length()} bytes")
    }

    @Test
    fun testExtractAudio_aacCodec_shouldCopy() {
        // First detect the codec
        val codec = extractor.detectAudioCodec(testVideoFile.absolutePath)

        val success = extractor.extractAudio(
            testVideoFile.absolutePath,
            outputAudioFile.absolutePath
        )

        assertTrue("Audio extraction should succeed", success)
        assertTrue("Output file should exist", outputAudioFile.exists())

        if (codec == "aac") {
            println("AAC codec detected - audio was copied without re-encoding")
        } else {
            println("$codec codec detected - audio was re-encoded to AAC")
        }
    }

    @Test
    fun testExtractAudio_nonExistentFile_shouldFail() {
        val success = extractor.extractAudio(
            "/path/to/nonexistent/file.mp4",
            outputAudioFile.absolutePath
        )

        assertFalse("Extraction from non-existent file should fail", success)
    }

    @Test
    fun testExtractAudio_invalidOutputPath_shouldFail() {
        // Try to write to a path that doesn't exist and can't be created
        val success = extractor.extractAudio(
            testVideoFile.absolutePath,
            "/invalid/path/that/does/not/exist/output.aac"
        )

        assertFalse("Extraction to invalid path should fail", success)
    }
}