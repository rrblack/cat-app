package com.rrblack.meowtranslator.core

import com.rrblack.meowtranslator.core.audio.PcmAudio
import com.rrblack.meowtranslator.core.audio.WavIO
import com.rrblack.meowtranslator.core.dsp.Fft
import com.rrblack.meowtranslator.core.dsp.FrameAnalyzer
import com.rrblack.meowtranslator.core.dsp.Resampler
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class DspTest {
    @Test
    fun fftFindsSinusoidBin() {
        val n = 256
        val x = DoubleArray(n) { sin(2 * PI * 16 * it / n) }
        val p = Fft.powerSpectrum(x, n)
        assertEquals(16, p.indices.maxBy { p[it] })
    }

    @Test
    fun pitchTrackerFollowsSteadyTone() {
        val tone = Synth.tone(0.6) { 550.0 }
        val track = FrameAnalyzer(Synth.SR).analyze(tone)
        val mid = track.size / 2
        assertTrue(track.voiced[mid])
        assertEquals(550.0, track.f0[mid], 8.0)
    }

    @Test
    fun pitchTrackerAvoidsOctaveErrorsOnStrongHarmonics() {
        // Weak fundamental, strong 2nd/3rd harmonics — the case where plain YIN jumps octaves.
        val n = Synth.SR
        val x = FloatArray(n) {
            val t = it.toDouble() / Synth.SR
            (0.05 * sin(2 * PI * 400 * t) + 0.3 * sin(2 * PI * 800 * t) + 0.25 * sin(2 * PI * 1200 * t)).toFloat()
        }
        val track = FrameAnalyzer(Synth.SR).analyze(x)
        val voiced = track.f0.filterIndexed { i, _ -> track.voiced[i] }
        val consistent = voiced.count { abs(it - voiced[voiced.size / 2]) < 20 }
        assertTrue("pitch should be stable, got ${voiced.take(20)}", consistent > voiced.size * 0.9)
    }

    @Test
    fun resamplerPreservesToneFrequency() {
        val sr = 44100
        val x = FloatArray(sr) { sin(2 * PI * 700 * it / sr).toFloat() * 0.5f }
        val y = Resampler.resample(x, sr, 8000)
        assertEquals(8000, y.size)
        val track = FrameAnalyzer(8000).analyze(y)
        assertEquals(700.0, track.f0[track.size / 2], 6.0)
    }

    @Test
    fun resamplerRejectsAliasedContent() {
        // 6 kHz is above the 4 kHz Nyquist of the target rate and must be removed, not folded down to 2 kHz.
        val sr = 48000
        val x = FloatArray(sr) { sin(2 * PI * 6000 * it / sr).toFloat() * 0.5f }
        val y = Resampler.resample(x, sr, 8000)
        val rms = FrameAnalyzer.rms(y, 1000, 7000)
        assertTrue("aliased energy rms=$rms", rms < 0.02)
    }

    @Test
    fun wavRoundTrip() {
        val samples = FloatArray(800) { (sin(it * 0.1) * 0.5).toFloat() }
        val out = ByteArrayOutputStream()
        WavIO.write(PcmAudio(samples, 8000), out)
        val back = WavIO.read(ByteArrayInputStream(out.toByteArray()))
        assertEquals(8000, back.sampleRate)
        assertArrayEquals(samples, back.samples, 1e-4f)
    }
}
