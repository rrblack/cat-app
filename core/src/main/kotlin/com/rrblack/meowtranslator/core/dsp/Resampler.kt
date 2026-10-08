package com.rrblack.meowtranslator.core.dsp

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/**
 * Band-limited sample-rate conversion (windowed-sinc interpolation).
 *
 * Phones record at 16/44.1/48 kHz while the translation model was trained on 8 kHz audio,
 * so everything is converted to [com.rrblack.meowtranslator.core.analysis.AcousticAnalyzer.SAMPLE_RATE]
 * before analysis. Quality matters here: aliasing would smear pitch and spectral features.
 */
object Resampler {
    fun resample(input: FloatArray, fromRate: Int, toRate: Int, halfTaps: Int = 16): FloatArray {
        if (fromRate == toRate || input.isEmpty()) return input.copyOf()
        val ratio = toRate.toDouble() / fromRate
        // Cut-off just under the lower of the two Nyquist frequencies, expressed relative to the input rate.
        val cutoff = 0.95 * minOf(1.0, ratio)
        val outLen = floor(input.size * ratio).toInt()
        val out = FloatArray(outLen)
        val support = halfTaps / cutoff
        for (i in 0 until outLen) {
            val t = i / ratio
            val center = floor(t).toInt()
            val lo = (center - support).toInt().coerceAtLeast(0)
            val hi = (center + support).toInt().coerceAtMost(input.size - 1)
            var acc = 0.0
            var norm = 0.0
            for (k in lo..hi) {
                val x = (t - k) * cutoff
                if (kotlin.math.abs(x) > halfTaps) continue
                val w = sinc(x) * blackman(x / halfTaps)
                acc += input[k] * w
                norm += w
            }
            out[i] = if (norm > 1e-9) (acc / norm).toFloat() else 0f
        }
        return out
    }

    private fun sinc(x: Double): Double = if (kotlin.math.abs(x) < 1e-9) 1.0 else sin(PI * x) / (PI * x)

    /** Blackman window over u in [-1, 1]. */
    private fun blackman(u: Double): Double {
        val a = (u + 1.0) / 2.0
        return 0.42 - 0.5 * kotlin.math.cos(2 * PI * a) + 0.08 * kotlin.math.cos(4 * PI * a)
    }
}
