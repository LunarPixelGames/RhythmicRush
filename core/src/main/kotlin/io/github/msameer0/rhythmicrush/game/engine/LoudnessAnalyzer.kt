package io.github.msameer0.rhythmicrush.game.engine

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import kotlin.math.sqrt

/**
 * Analyzes audio files to extract loudness data used for reactive visual effects.
 */
class LoudnessAnalyzer {

    fun analyze(file: FileHandle, sampleRate: Int = 50): FloatArray {
        val bytes = file.readBytes()
        val frames = parseMp3Frames(bytes)

        if (frames.isEmpty()) {
            Gdx.app.error("LoudnessAnalyzer", "No MP3 frames found!")
            return FloatArray(0)
        }

        val firstFrame = frames[0]
        val msPerFrame = 1152f / firstFrame.sampleRate * 1000f
        val windowMs = 1000f / sampleRate

        Gdx.app.log("LoudnessAnalyzer", "Frames: ${frames.size}, Freq: ${firstFrame.sampleRate}, msPerFrame: $msPerFrame")

        val loudnessMap = mutableListOf<Float>()
        var maxLoudness = 0f
        var accumulatedMs = 0f
        var windowEnergy = 0.0
        var windowFrames = 0

        for (frame in frames) {
            val dataStart = frame.offset + 4
            val dataEnd = (frame.offset + frame.frameSize).coerceAtMost(bytes.size)

            if (dataEnd > dataStart) {
                var energy = 0.0
                val count = dataEnd - dataStart
                for (i in dataStart until dataEnd) {
                    val v = (bytes[i].toInt() and 0xFF) - 128
                    energy += v.toDouble() * v.toDouble()
                }
                windowEnergy += sqrt(energy / count)
                windowFrames++
            }

            accumulatedMs += msPerFrame

            if (accumulatedMs >= windowMs && windowFrames > 0) {
                val avg = (windowEnergy / windowFrames).toFloat()
                loudnessMap.add(avg)
                if (avg > maxLoudness) maxLoudness = avg
                windowEnergy = 0.0
                windowFrames = 0
                accumulatedMs -= windowMs
            }
        }

        if (windowFrames > 0) {
            val avg = (windowEnergy / windowFrames).toFloat()
            loudnessMap.add(avg)
            if (avg > maxLoudness) maxLoudness = avg
        }

        Gdx.app.log("LoudnessAnalyzer", "Analysis complete. Windows: ${loudnessMap.size}, Max energy: $maxLoudness")

        val result = FloatArray(loudnessMap.size)
        if (maxLoudness > 0) {
            for (i in loudnessMap.indices) {
                val rawNormalized = (loudnessMap[i] / maxLoudness).coerceIn(0f, 1f)
                result[i] = Math.pow(rawNormalized.toDouble(), 6.0).toFloat()
            }
        }

        return result
    }

    /**
     * Represents a single MP3 frame's metadata found during analysis.
     */
    private class Mp3Frame(
        val offset: Int,
        val frameSize: Int,
        val sampleRate: Int
    )

    private fun parseMp3Frames(bytes: ByteArray): List<Mp3Frame> {
        val frames = mutableListOf<Mp3Frame>()
        var pos = 0

        if (bytes.size >= 10
            && bytes[0] == 'I'.code.toByte()
            && bytes[1] == 'D'.code.toByte()
            && bytes[2] == '3'.code.toByte()
        ) {
            val size = ((bytes[6].toInt() and 0x7F) shl 21) or
                ((bytes[7].toInt() and 0x7F) shl 14) or
                ((bytes[8].toInt() and 0x7F) shl 7) or
                (bytes[9].toInt() and 0x7F)
            pos = size + 10
        }

        val bitratesV1L3 = intArrayOf(0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0)
        val sampleRates = intArrayOf(44100, 48000, 32000, 0)

        while (pos < bytes.size - 4) {
            val b0 = bytes[pos].toInt() and 0xFF
            val b1 = bytes[pos + 1].toInt() and 0xFF

            if (b0 == 0xFF && (b1 and 0xE0) == 0xE0) {
                val version = (b1 shr 3) and 0x03
                val layer = (b1 shr 1) and 0x03

                if (version == 3 && layer == 1) {
                    val b2 = bytes[pos + 2].toInt() and 0xFF
                    val bitrateIdx = (b2 shr 4) and 0x0F
                    val srIdx = (b2 shr 2) and 0x03
                    val padding = (b2 shr 1) and 0x01

                    if (bitrateIdx in 1..14 && srIdx in 0..2) {
                        val bitrate = bitratesV1L3[bitrateIdx] * 1000
                        val sr = sampleRates[srIdx]
                        val frameSize = (144 * bitrate / sr) + padding

                        if (frameSize > 0 && pos + frameSize <= bytes.size) {
                            frames.add(Mp3Frame(pos, frameSize, sr))
                            pos += frameSize
                            continue
                        }
                    }
                }
            }
            pos++
        }

        return frames
    }
}
