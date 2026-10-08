package com.rrblack.meowtranslator.core.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/** Mel-frequency cepstral coefficients computed from a power spectrum. */
class Mfcc(
    sampleRate: Int,
    fftSize: Int,
    private val numFilters: Int = 26,
    private val numCoefficients: Int = 13,
    minFreq: Double = 100.0,
    maxFreq: Double = sampleRate / 2.0,
) {
    private val filters: Array<DoubleArray>
    private val filterStart: IntArray

    init {
        val bins = fftSize / 2 + 1
        val melMin = hzToMel(minFreq)
        val melMax = hzToMel(maxFreq)
        val points = DoubleArray(numFilters + 2) { melToHz(melMin + (melMax - melMin) * it / (numFilters + 1)) }
        val binOf = points.map { it * fftSize / sampleRate }
        filterStart = IntArray(numFilters)
        filters = Array(numFilters) { m ->
            val left = binOf[m]
            val center = binOf[m + 1]
            val right = binOf[m + 2]
            val start = kotlin.math.floor(left).toInt().coerceAtLeast(0)
            val end = kotlin.math.ceil(right).toInt().coerceAtMost(bins - 1)
            filterStart[m] = start
            DoubleArray(end - start + 1) { i ->
                val k = (start + i).toDouble()
                when {
                    k <= left || k >= right -> 0.0
                    k <= center -> (k - left) / (center - left)
                    else -> (right - k) / (right - center)
                }
            }
        }
    }

    fun compute(power: DoubleArray): DoubleArray {
        val logMel = DoubleArray(numFilters)
        for (m in 0 until numFilters) {
            var e = 0.0
            val f = filters[m]
            for (i in f.indices) e += f[i] * power[filterStart[m] + i]
            logMel[m] = ln(e + 1e-10)
        }
        val scale = sqrt(2.0 / numFilters)
        return DoubleArray(numCoefficients) { c ->
            var s = 0.0
            for (m in 0 until numFilters) s += logMel[m] * cos(PI * c * (m + 0.5) / numFilters)
            s * scale
        }
    }

    companion object {
        fun hzToMel(hz: Double) = 2595.0 * log10(1.0 + hz / 700.0)
        fun melToHz(mel: Double) = 700.0 * (10.0.pow(mel / 2595.0) - 1.0)
    }
}
