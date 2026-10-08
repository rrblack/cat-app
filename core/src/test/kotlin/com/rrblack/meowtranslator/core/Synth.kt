package com.rrblack.meowtranslator.core

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Synthetic cat-like sounds with known ground truth for testing the analysis pipeline. */
object Synth {
    const val SR = 8000

    /** Harmonic tone whose F0 follows [f0] (Hz as a function of 0..1 progress), with a smooth envelope. */
    fun tone(durationSec: Double, amp: Double = 0.3, harmonics: Int = 6, noise: Double = 0.003, f0: (Double) -> Double): FloatArray {
        val n = (durationSec * SR).toInt()
        val rnd = Random(1)
        var phase = 0.0
        return FloatArray(n) { i ->
            val t = i.toDouble() / n
            phase += 2 * PI * f0(t) / SR
            val env = sin(PI * t).let { it * it }.coerceAtLeast(0.0)
            var s = 0.0
            for (h in 1..harmonics) {
                if (f0(t) * h < SR / 2 - 200) s += sin(phase * h) / h
            }
            (amp * env * s + noise * (rnd.nextDouble() * 2 - 1)).toFloat()
        }
    }

    fun silence(durationSec: Double, noise: Double = 0.003, seed: Int = 2): FloatArray {
        val rnd = Random(seed)
        return FloatArray((durationSec * SR).toInt()) { (noise * (rnd.nextDouble() * 2 - 1)).toFloat() }
    }

    fun noise(durationSec: Double, amp: Double = 0.3, seed: Int = 3): FloatArray {
        val n = (durationSec * SR).toInt()
        val rnd = Random(seed)
        // High-passed white noise with a soft attack/decay, like a hiss.
        var prev = 0.0
        return FloatArray(n) { i ->
            val t = i.toDouble() / n
            val env = minOf(1.0, t * 10, (1 - t) * 10)
            val w = rnd.nextDouble() * 2 - 1
            val hp = w - 0.6 * prev
            prev = w
            (amp * env * hp).toFloat()
        }
    }

    /** Purr: a train of decaying low-frequency pulses at [rateHz]. */
    fun purr(durationSec: Double, rateHz: Double = 25.0, amp: Double = 0.4): FloatArray {
        val n = (durationSec * SR).toInt()
        val rnd = Random(4)
        val period = SR / rateHz
        return FloatArray(n) { i ->
            val inPeriod = (i % period) / SR
            val pulse = kotlin.math.exp(-inPeriod * 180) * sin(2 * PI * 140 * inPeriod)
            val t = i.toDouble() / n
            val env = minOf(1.0, t * 8, (1 - t) * 8)
            (amp * env * pulse + 0.003 * (rnd.nextDouble() * 2 - 1)).toFloat()
        }
    }

    fun concat(vararg parts: FloatArray): FloatArray {
        val out = FloatArray(parts.sumOf { it.size })
        var pos = 0
        for (p in parts) {
            p.copyInto(out, pos)
            pos += p.size
        }
        return out
    }

    fun padded(sound: FloatArray) = concat(silence(0.4), sound, silence(0.4, seed = 9))
}
