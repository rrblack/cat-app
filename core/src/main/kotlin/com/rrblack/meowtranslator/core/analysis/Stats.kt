package com.rrblack.meowtranslator.core.analysis

import kotlin.math.sqrt

object Stats {
    fun mean(x: DoubleArray): Double = if (x.isEmpty()) 0.0 else x.sum() / x.size

    fun std(x: DoubleArray): Double {
        if (x.size < 2) return 0.0
        val m = mean(x)
        var s = 0.0
        for (v in x) s += (v - m) * (v - m)
        return sqrt(s / (x.size - 1))
    }

    fun percentile(x: DoubleArray, p: Double): Double {
        if (x.isEmpty()) return 0.0
        val sorted = x.sortedArray()
        val pos = (p / 100.0) * (sorted.size - 1)
        val lo = pos.toInt()
        val hi = minOf(lo + 1, sorted.size - 1)
        return sorted[lo] + (sorted[hi] - sorted[lo]) * (pos - lo)
    }

    fun median(x: DoubleArray): Double = percentile(x, 50.0)

    /** Least-squares slope of y against x. */
    fun slope(x: DoubleArray, y: DoubleArray): Double {
        if (x.size < 2) return 0.0
        val mx = mean(x)
        val my = mean(y)
        var num = 0.0
        var den = 0.0
        for (i in x.indices) {
            num += (x[i] - mx) * (y[i] - my)
            den += (x[i] - mx) * (x[i] - mx)
        }
        return if (den > 1e-12) num / den else 0.0
    }

    fun medianFilter(x: DoubleArray, radius: Int): DoubleArray = DoubleArray(x.size) { i ->
        val lo = (i - radius).coerceAtLeast(0)
        val hi = (i + radius).coerceAtMost(x.size - 1)
        median(x.copyOfRange(lo, hi + 1))
    }
}
