package com.example.andopsi.data

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit

class AndroidAudioExtractor : AudioExtractor {

    override fun detectAudioCodec(inputPath: String): String? {
        val session = FFprobeKit.execute(
            "-v error -select_streams a:0 -show_entries stream=codec_name -of default=nw=1 $inputPath"
        )

        val output = session.output ?: return null
        return output.lineSequence()
            .firstOrNull { it.startsWith("codec_name=") }
            ?.substringAfter("=")
    }

    override fun extractAudio(inputPath: String, outputPath: String): Boolean {
        val codec = detectAudioCodec(inputPath)

        val command = if (codec == "aac") {
            "-i $inputPath -vn -acodec copy $outputPath"
        } else {
            "-i $inputPath -vn -c:a aac -b:a 192k $outputPath"
        }

        val session = FFmpegKit.execute(command)
        return session.returnCode.isValueSuccess
    }
}