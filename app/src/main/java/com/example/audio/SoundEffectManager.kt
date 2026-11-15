package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.game.SoundEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundEffectManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 22050
    private val cache = mutableMapOf<SoundEvent, ShortArray>()

    @Volatile
    var enabled: Boolean = true

    init {
        try {
            SoundEvent.entries.forEach { event ->
                cache[event] = synthesize(event)
            }
        } catch (_: Throwable) {
            // Safe fallback in headless/unit test environments
        }
    }

    fun play(event: SoundEvent) {
        if (!enabled) return
        val pcm = cache[event] ?: return
        scope.launch {
            playPcm(pcm)
        }
    }

    fun release() {
        scope.cancel()
    }

    private fun playPcm(pcm: ShortArray) {
        try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(pcm.size * 2)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(maxOf(minBuf, pcm.size * 2))
                .build()

            track.write(pcm, 0, pcm.size)
            track.play()
            Thread.sleep((pcm.size * 1000L) / sampleRate + 40L)
            track.stop()
            track.release()
        } catch (_: Throwable) {
            // Ignore audio playback errors on headless environments
        }
    }

    private fun synthesize(event: SoundEvent): ShortArray {
        return when (event) {
            SoundEvent.LIGHT_ATTACK -> sweepTone(startHz = 780f, endHz = 280f, durationSec = 0.085f, volume = 0.28f)
            SoundEvent.HEAVY_ATTACK -> sweepTone(startHz = 440f, endHz = 110f, durationSec = 0.16f, volume = 0.42f, squareMix = 0.35f)
            SoundEvent.ENEMY_HIT -> sweepTone(startHz = 520f, endHz = 860f, durationSec = 0.075f, volume = 0.30f, squareMix = 0.25f)
            SoundEvent.DAMAGE_TAKEN -> sweepTone(startHz = 210f, endHz = 85f, durationSec = 0.18f, volume = 0.42f, squareMix = 0.5f)
            SoundEvent.SHIELD_BLOCK -> chordTone(freqs = floatArrayOf(660f, 990f), durationSec = 0.12f, volume = 0.30f)
            SoundEvent.ITEM_COLLECTED -> arpeggioTone(freqs = floatArrayOf(587.33f, 783.99f, 1046.5f), noteDurSec = 0.055f, volume = 0.32f)
            SoundEvent.DASH -> sweepTone(startHz = 320f, endHz = 960f, durationSec = 0.10f, volume = 0.24f)
        }
    }

    private fun sweepTone(
        startHz: Float,
        endHz: Float,
        durationSec: Float,
        volume: Float,
        squareMix: Float = 0f
    ): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val freq = startHz + (endHz - startHz) * t
            phase += 2.0 * PI * freq / sampleRate
            val sineVal = sin(phase)
            val squareVal = if (sineVal >= 0.0) 1.0 else -1.0
            val wave = sineVal * (1.0 - squareMix) + squareVal * squareMix
            val env = exp(-t * 4.5f) * (1f - t * 0.25f)
            out[i] = (wave * env * volume * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun chordTone(freqs: FloatArray, durationSec: Float, volume: Float): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toFloat() / count
            var sum = 0.0
            for (f in freqs) {
                sum += sin(2.0 * PI * f * i / sampleRate)
            }
            val env = exp(-t * 5.0f)
            out[i] = ((sum / freqs.size) * env * volume * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun arpeggioTone(freqs: FloatArray, noteDurSec: Float, volume: Float): ShortArray {
        val perNote = (sampleRate * noteDurSec).toInt().coerceAtLeast(1)
        val out = ShortArray(perNote * freqs.size)
        for (n in freqs.indices) {
            val freq = freqs[n]
            for (i in 0 until perNote) {
                val t = i.toFloat() / perNote
                val env = exp(-t * 3.5f)
                val wave = sin(2.0 * PI * freq * i / sampleRate)
                out[n * perNote + i] = (wave * env * volume * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        return out
    }
}
