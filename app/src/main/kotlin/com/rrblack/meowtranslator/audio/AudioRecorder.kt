package com.rrblack.meowtranslator.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.rrblack.meowtranslator.core.audio.PcmAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext
import kotlin.math.log10
import kotlin.math.max

class MicrophoneUnavailableException : IllegalStateException("Microphone unavailable")

/**
 * Microphone capture that stops by itself once the cat has gone quiet again.
 *
 * Uses the VOICE_RECOGNITION source, which on most devices disables automatic gain control and noise
 * suppression — those "speech enhancements" would distort exactly the pitch and spectral cues we measure.
 */
class AudioRecorder {
    /** Live progress, reported ~20 times per second. */
    data class Progress(val levelDb: Float, val elapsedSec: Float, val heardSound: Boolean)

    private val stopRequested = AtomicBoolean(false)

    fun requestStop() = stopRequested.set(true)

    @SuppressLint("MissingPermission") // Caller checks RECORD_AUDIO before starting.
    suspend fun record(onProgress: (Progress) -> Unit): PcmAudio = withContext(Dispatchers.IO) {
        stopRequested.set(false)
        val (record, rate) = open() ?: throw MicrophoneUnavailableException()
        val chunk = rate / 20
        val buffer = ShortArray(chunk)
        val samples = FloatArrayBuilder(rate * 4)
        var noiseFloor = Float.MAX_VALUE
        var heard = false
        var quietSec = 0f
        try {
            record.startRecording()
            while (coroutineContext.isActive && !stopRequested.get()) {
                val n = record.read(buffer, 0, buffer.size)
                if (n <= 0) continue
                var energy = 0.0
                for (i in 0 until n) {
                    val s = buffer[i] / 32768f
                    samples.add(s)
                    energy += s * s
                }
                val db = (10 * log10(energy / n + 1e-12)).toFloat()
                val elapsed = samples.size.toFloat() / rate
                // Skip the first 150 ms (button click, mic settling) when learning the room's noise level.
                if (elapsed > 0.15f) noiseFloor = minOf(noiseFloor, db)
                val floor = if (noiseFloor == Float.MAX_VALUE) db else noiseFloor
                if (!heard && elapsed > 0.3f && db > floor + START_MARGIN_DB && db > MIN_SOUND_DB) heard = true
                if (heard) quietSec = if (db < floor + QUIET_MARGIN_DB) quietSec + n.toFloat() / rate else 0f
                onProgress(Progress(db, elapsed, heard))
                if (heard && quietSec >= AUTO_STOP_QUIET_SEC && elapsed >= MIN_SEC) break
                if (elapsed >= MAX_SEC) break
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }
        PcmAudio(samples.toArray(), rate)
    }

    @SuppressLint("MissingPermission")
    private fun open(): Pair<AudioRecord, Int>? {
        for (rate in intArrayOf(16000, 44100, 48000, 22050, 8000)) {
            val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (min <= 0) continue
            val record = runCatching {
                AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, max(min, rate / 5) * 2)
            }.getOrNull() ?: continue
            if (record.state == AudioRecord.STATE_INITIALIZED) return record to rate
            record.release()
        }
        return null
    }

    private class FloatArrayBuilder(initial: Int) {
        private var data = FloatArray(initial)
        var size = 0
            private set

        fun add(v: Float) {
            if (size == data.size) data = data.copyOf(data.size * 2)
            data[size++] = v
        }

        fun toArray(): FloatArray = data.copyOf(size)
    }

    companion object {
        const val MAX_SEC = 15f
        private const val MIN_SEC = 1f
        private const val AUTO_STOP_QUIET_SEC = 1.6f
        private const val START_MARGIN_DB = 15f
        private const val QUIET_MARGIN_DB = 8f
        private const val MIN_SOUND_DB = -60f

        /** Maps a dBFS level onto 0..1 for meters. */
        fun normalizedLevel(db: Float): Float = ((db + 70f) / 60f).coerceIn(0f, 1f)
    }
}
