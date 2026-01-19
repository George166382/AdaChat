package com.example.andopsi.whisper

import java.util.concurrent.Executors

/**
 * The raw JNI interface.
 */
object WhisperBridge {
    init {
        System.loadLibrary("whisper")
    }

    external fun init(modelPath: String): Boolean
    external fun processAudio(pcm: ShortArray, length: Int): String
    external fun destroy()
}

/**
 * The high-level Engine that handles threading.
 */
class WhisperEngine(
    private val modelPath: String,
    private val onText: (String) -> Unit
) {
    private val executor = Executors.newSingleThreadExecutor()

    fun start() {
        executor.execute {
            val success = WhisperBridge.init(modelPath)
            if (success) {
                println("Whisper Engine Initialized")
            } else {
                println("Whisper Engine Failed to Initialize")
            }
        }
    }

    fun pushAudio(pcm: ShortArray) {
        executor.execute {
            // This is a blocking call, running on background thread
            val text = WhisperBridge.processAudio(pcm, pcm.size)
            if (text.isNotEmpty()) {
                onText(text)
            }
        }
    }

    fun stop() {
        executor.execute {
            WhisperBridge.destroy()
        }
        executor.shutdown()
    }
}