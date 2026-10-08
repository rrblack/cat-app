package com.rrblack.meowtranslator.core.classify

import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Multinomial logistic regression on standardised features.
 * Small, interpretable and hard to over-fit — appropriate for a dataset of a few hundred calls.
 */
class SoftmaxRegression(
    val classes: List<String>,
    val mean: DoubleArray,
    val scale: DoubleArray,
    /** weights[class][feature]; bias is stored separately. */
    val weights: Array<DoubleArray>,
    val bias: DoubleArray,
) {
    fun predict(x: DoubleArray): DoubleArray {
        val logits = DoubleArray(classes.size) { c ->
            var z = bias[c]
            val w = weights[c]
            for (i in x.indices) z += w[i] * (x[i] - mean[i]) / scale[i]
            z
        }
        return softmax(logits)
    }

    companion object {
        fun softmax(z: DoubleArray): DoubleArray {
            val m = z.max()
            val e = DoubleArray(z.size) { exp(z[it] - m) }
            val s = e.sum()
            return DoubleArray(z.size) { e[it] / s }
        }

        /**
         * Full-batch gradient descent (with Nesterov momentum) on class-weighted cross-entropy + L2.
         * Classes are re-weighted to equal total weight so the majority context does not dominate.
         */
        fun train(
            x: List<DoubleArray>,
            y: IntArray,
            classes: List<String>,
            l2: Double,
            epochs: Int = 600,
            learningRate: Double = 0.1,
        ): SoftmaxRegression {
            val d = x[0].size
            val k = classes.size
            val n = x.size
            val mean = DoubleArray(d) { j -> x.sumOf { it[j] } / n }
            val scale = DoubleArray(d) { j ->
                val v = x.sumOf { (it[j] - mean[j]) * (it[j] - mean[j]) } / n
                if (v > 1e-12) sqrt(v) else 1.0
            }
            val z = x.map { row -> DoubleArray(d) { (row[it] - mean[it]) / scale[it] } }
            val counts = IntArray(k).also { c -> y.forEach { c[it]++ } }
            val sampleWeight = DoubleArray(n) { n.toDouble() / (k * counts[y[it]].coerceAtLeast(1)) }
            val wsum = sampleWeight.sum()

            val w = Array(k) { DoubleArray(d) }
            val b = DoubleArray(k)
            val vw = Array(k) { DoubleArray(d) }
            val vb = DoubleArray(k)
            val momentum = 0.9
            repeat(epochs) {
                val gw = Array(k) { DoubleArray(d) }
                val gb = DoubleArray(k)
                // Gradient at the look-ahead point (Nesterov).
                val lw = Array(k) { c -> DoubleArray(d) { j -> w[c][j] + momentum * vw[c][j] } }
                val lb = DoubleArray(k) { b[it] + momentum * vb[it] }
                for (i in 0 until n) {
                    val logits = DoubleArray(k) { c ->
                        var s = lb[c]
                        for (j in 0 until d) s += lw[c][j] * z[i][j]
                        s
                    }
                    val p = softmax(logits)
                    for (c in 0 until k) {
                        val g = (p[c] - if (y[i] == c) 1.0 else 0.0) * sampleWeight[i] / wsum
                        gb[c] += g
                        for (j in 0 until d) gw[c][j] += g * z[i][j]
                    }
                }
                for (c in 0 until k) {
                    for (j in 0 until d) {
                        val g = gw[c][j] + l2 * lw[c][j]
                        vw[c][j] = momentum * vw[c][j] - learningRate * g
                        w[c][j] += vw[c][j]
                    }
                    vb[c] = momentum * vb[c] - learningRate * gb[c]
                    b[c] += vb[c]
                }
            }
            return SoftmaxRegression(classes, mean, scale, w, b)
        }
    }
}
