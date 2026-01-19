package com.example.andopsi.whisper

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

class WhisperAudioProcessor(
    private val onPcm16k: (ShortArray) -> Unit
) : AudioProcessor {

    private var inputAudioFormat: AudioFormat = AudioFormat.NOT_SET
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        this.inputAudioFormat = inputAudioFormat
        return inputAudioFormat
    }

    override fun isActive(): Boolean = inputAudioFormat != AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val bufferCopy = inputBuffer.asReadOnlyBuffer()
        val shortCount = bufferCopy.remaining() / 2
        val stereo = ShortArray(shortCount)
        bufferCopy.order(ByteOrder.nativeOrder()).asShortBuffer().get(stereo)

        val channels = inputAudioFormat.channelCount
        val mono48k = if (channels == 2) {
            val out = ShortArray(shortCount / 2)
            var i = 0
            var j = 0
            while (i < shortCount) {
                val left = stereo[i].toInt()
                val right = stereo[i + 1].toInt()
                out[j] = ((left + right) / 2).toShort()
                i += 2
                j += 1
            }
            out
        } else {
            stereo
        }

        val sampleRate = inputAudioFormat.sampleRate
        if (sampleRate != 16000) {
            val ratio = sampleRate / 16000f
            val outLength = (mono48k.size / ratio).toInt()
            val mono16k = ShortArray(outLength)
            var srcIndex = 0f

            for (i in 0 until outLength) {
                val idx = srcIndex.toInt()
                val frac = srcIndex - idx
                val s1 = mono48k[idx].toInt()
                val s2 = mono48k[min(idx + 1, mono48k.size - 1)].toInt()
                mono16k[i] = (s1 + frac * (s2 - s1)).toInt().toShort()
                srcIndex += ratio
            }
            onPcm16k(mono16k)
        } else {
            onPcm16k(mono48k)
        }

        if (outputBuffer.capacity() < remaining) {
            outputBuffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
        } else {
            outputBuffer.clear()
        }
        outputBuffer.put(inputBuffer)
        outputBuffer.flip()
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean = false

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
    }

    override fun reset() {
        flush()
        inputAudioFormat = AudioFormat.NOT_SET
    }

    override fun queueEndOfStream() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
    }
}
