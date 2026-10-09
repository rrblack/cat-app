package com.rrblack.meowtranslator.trainer

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Makes clean, close-miked dataset recordings sound like a phone in a real home: room reverberation,
 * background noise at a given SNR, a quieter overall level, and optionally the thump of a finger on the phone.
 */
object Augment {
    data class Condition(val name: String, val snrDb: Double?, val rt60: Double?, val gainDb: Double = 0.0, val tap: Boolean = false)

    val CONDITIONS = listOf(
        Condition("clean", null, null),
        Condition("room-echo", null, 0.45),
        Condition("noise-20dB", 20.0, null),
        Condition("noise-10dB", 10.0, null),
        Condition("home(echo+15dB noise+quiet)", 15.0, 0.4, gainDb = -18.0),
        Condition("home+tap", 15.0, 0.4, gainDb = -18.0, tap = true),
    )

    fun apply(x: FloatArray, sr: Int, c: Condition, rnd: Random, noiseBank: List<FloatArray> = emptyList()): FloatArray {
        // Pad with silence so the recording starts and ends quiet like a real capture.
        var y = FloatArray(x.size + sr) { i -> if (i >= sr / 2 && i - sr / 2 < x.size) x[i - sr / 2] else 0f }
        if (c.rt60 != null) y = reverb(y, sr, c.rt60, rnd)
        if (c.snrDb != null) {
            val noise = if (noiseBank.isNotEmpty() && rnd.nextBoolean()) loopTo(noiseBank.random(rnd), y.size, rnd) else pink(y.size, rnd)
            val sig = activeRms(y)
            val n = rms(noise)
            val scale = if (n > 0) sig / n / 10.0.pow(c.snrDb / 20) else 0.0
            for (i in y.indices) y[i] += (noise[i] * scale).toFloat()
        }
        if (c.tap) {
            val at = (0.08 * sr).toInt()
            val peak = y.maxOf { kotlin.math.abs(it) } * 3
            for (k in 0 until (0.03 * sr).toInt()) if (at + k < y.size) y[at + k] += (peak * exp(-k / (0.004 * sr)) * (rnd.nextDouble() * 2 - 1)).toFloat()
        }
        val g = 10.0.pow(c.gainDb / 20).toFloat()
        var max = 0f
        for (i in y.indices) {
            y[i] *= g
            max = maxOf(max, kotlin.math.abs(y[i]))
        }
        if (max > 0.99f) for (i in y.indices) y[i] *= 0.99f / max
        return y
    }

    /** Synthetic room impulse response: direct sound plus exponentially decaying diffuse tail. */
    private fun reverb(x: FloatArray, sr: Int, rt60: Double, rnd: Random): FloatArray {
        val len = (rt60 * sr).toInt()
        val decay = ln(1000.0) / (rt60 * sr)
        val ir = DoubleArray(len) { i -> if (i == 0) 1.0 else if (i < sr / 200) 0.0 else 0.35 * rnd.nextGaussian() * exp(-decay * i) }
        val out = FloatArray(x.size)
        for (i in x.indices) {
            val s = x[i]
            if (s == 0f) continue
            for (k in 0 until minOf(len, x.size - i)) out[i + k] += (s * ir[k]).toFloat()
        }
        return out
    }

    private fun Random.nextGaussian(): Double {
        val u = nextDouble().coerceAtLeast(1e-12)
        return sqrt(-2 * ln(u)) * kotlin.math.cos(2 * Math.PI * nextDouble())
    }

    /** Pink-ish noise (Paul Kellet's filter), typical of fans, fridges and room tone. */
    private fun pink(n: Int, rnd: Random): FloatArray {
        var b0 = 0.0; var b1 = 0.0; var b2 = 0.0
        return FloatArray(n) {
            val w = rnd.nextDouble() * 2 - 1
            b0 = 0.99765 * b0 + w * 0.0990460
            b1 = 0.96300 * b1 + w * 0.2965164
            b2 = 0.57000 * b2 + w * 1.0526913
            (b0 + b1 + b2 + w * 0.1848).toFloat()
        }
    }

    private fun loopTo(src: FloatArray, n: Int, rnd: Random): FloatArray {
        val start = rnd.nextInt(src.size)
        return FloatArray(n) { src[(start + it) % src.size] }
    }

    fun rms(x: FloatArray): Double = sqrt(x.sumOf { it.toDouble() * it } / x.size.coerceAtLeast(1))

    /** RMS of the loudest 30% of 20 ms blocks, i.e. of the call itself rather than the silence around it. */
    fun activeRms(x: FloatArray, block: Int = 160): Double {
        val blocks = (0 until x.size / block).map { b -> rms(x.copyOfRange(b * block, (b + 1) * block)) }.sortedDescending()
        val top = blocks.take((blocks.size * 0.3).toInt().coerceAtLeast(1))
        return sqrt(top.sumOf { it * it } / top.size)
    }
}
