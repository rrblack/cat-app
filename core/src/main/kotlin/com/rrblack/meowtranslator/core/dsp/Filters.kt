package com.rrblack.meowtranslator.core.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object Filters {
    /** Zero-phase-delay-compensated windowed-sinc low-pass FIR; [cutoff] is a fraction of the sample rate. */
    fun lowPass(x: FloatArray, cutoff: Double, taps: Int = 41): FloatArray {
        val m = taps - 1
        val h = DoubleArray(taps) { i ->
            val k = i - m / 2.0
            val sinc = if (k == 0.0) 2 * cutoff else sin(2 * PI * cutoff * k) / (PI * k)
            sinc * (0.54 - 0.46 * cos(2 * PI * i / m))
        }
        val norm = h.sum()
        val half = m / 2
        return FloatArray(x.size) { n ->
            var acc = 0.0
            for (i in 0 until taps) {
                val idx = n + half - i
                if (idx in x.indices) acc += h[i] * x[idx]
            }
            (acc / norm).toFloat()
        }
    }
}
