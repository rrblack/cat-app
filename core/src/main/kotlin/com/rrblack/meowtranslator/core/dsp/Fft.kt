package com.rrblack.meowtranslator.core.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** In-place iterative radix-2 complex FFT. [re] and [im] must have the same power-of-two length. */
object Fft {
    fun transform(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        require(n == im.size && n > 0 && (n and (n - 1)) == 0) { "FFT size must be a power of two, got $n" }

        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                var t = re[i]; re[i] = re[j]; re[j] = t
                t = im[i]; im[i] = im[j]; im[j] = t
            }
        }

        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wRe = cos(ang)
            val wIm = sin(ang)
            var start = 0
            while (start < n) {
                var curRe = 1.0
                var curIm = 0.0
                for (k in 0 until len / 2) {
                    val a = start + k
                    val b = a + len / 2
                    val vRe = re[b] * curRe - im[b] * curIm
                    val vIm = re[b] * curIm + im[b] * curRe
                    re[b] = re[a] - vRe
                    im[b] = im[a] - vIm
                    re[a] += vRe
                    im[a] += vIm
                    val nextRe = curRe * wRe - curIm * wIm
                    curIm = curRe * wIm + curIm * wRe
                    curRe = nextRe
                }
                start += len
            }
            len = len shl 1
        }
    }

    /** Power spectrum (|X|^2) of a real signal, bins 0..n/2 inclusive. Input is zero-padded to [n]. */
    fun powerSpectrum(frame: DoubleArray, n: Int): DoubleArray {
        val re = DoubleArray(n)
        val im = DoubleArray(n)
        frame.copyInto(re, 0, 0, minOf(frame.size, n))
        transform(re, im)
        return DoubleArray(n / 2 + 1) { re[it] * re[it] + im[it] * im[it] }
    }

    fun nextPowerOfTwo(x: Int): Int {
        var n = 1
        while (n < x) n = n shl 1
        return n
    }

    fun hann(size: Int): DoubleArray =
        DoubleArray(size) { 0.5 - 0.5 * cos(2.0 * PI * it / (size - 1).coerceAtLeast(1)) }
}
