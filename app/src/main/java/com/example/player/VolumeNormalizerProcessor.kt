package com.example.player

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Custom AudioProcessor for real-time volume normalization (AGC).
 * It calculates the RMS level and applies a compensatory gain with smoothing.
 */
@UnstableApi
class VolumeNormalizerProcessor : BaseAudioProcessor() {

    private var isEnabled = false
    private var currentGain = 1.0f
    private val targetRms = 0.2f // Target RMS level (approx -14 LUFS)
    private val maxGain = 3.0f
    private val minGain = 0.5f
    
    // Smoothing constants
    private val attackCoeff = 0.999f
    private val releaseCoeff = 0.9999f

    fun setEnabled(enabled: Boolean) {
        if (isEnabled != enabled) {
            isEnabled = enabled
            if (!enabled) currentGain = 1.0f
            flush()
        }
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != androidx.media3.common.C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining <= 0) return

        val outputBuffer = replaceOutputBuffer(remaining)

        if (!isEnabled) {
            outputBuffer.put(inputBuffer)
        } else {
            // Process samples to calculate RMS and apply gain
            val limit = inputBuffer.limit()
            var sumSquare = 0.0
            val count = remaining / 2

            // First pass: Calculate RMS for this buffer
            val initialPosition = inputBuffer.position()
            while (inputBuffer.hasRemaining()) {
                val sample = inputBuffer.short.toFloat() / Short.MAX_VALUE
                sumSquare += (sample * sample).toDouble()
            }
            inputBuffer.position(initialPosition)

            val rms = sqrt(sumSquare / count).toFloat()
            
            // Calculate required gain
            val requiredGain = if (rms > 0.001f) (targetRms / rms).coerceIn(minGain, maxGain) else 1.0f

            // Second pass: Apply gain with smoothing (simple envelope follower logic)
            while (inputBuffer.hasRemaining()) {
                val coeff = if (requiredGain < currentGain) attackCoeff else releaseCoeff
                currentGain = coeff * currentGain + (1f - coeff) * requiredGain
                
                val sample = inputBuffer.short.toFloat() * currentGain
                val clampedSample = sample.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat()).toInt().toShort()
                outputBuffer.putShort(clampedSample)
            }
        }

        outputBuffer.flip()
    }
}
