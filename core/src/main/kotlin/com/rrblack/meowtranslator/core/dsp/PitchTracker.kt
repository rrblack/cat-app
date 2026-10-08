package com.rrblack.meowtranslator.core.dsp

import kotlin.math.abs
import kotlin.math.log2

/**
 * Chooses the smoothest plausible F0 path through per-frame YIN candidates with the Viterbi algorithm.
 *
 * Each frame is either unvoiced or takes one of its candidates. Costs: a candidate's aperiodicity,
 * a penalty proportional to the pitch jump (in semitones) between consecutive voiced frames, and a fixed
 * penalty for switching voicing on/off. Octave errors therefore need sustained acoustic evidence to win.
 */
object PitchTracker {
    private const val UNVOICED_COST = 0.32
    private const val JUMP_COST_PER_ST = 0.035
    private const val MAX_FREE_JUMP_ST = 0.6
    private const val VOICING_SWITCH_COST = 0.25

    data class Result(val f0: DoubleArray, val aperiodicity: DoubleArray, val voiced: BooleanArray)

    fun track(candidates: List<List<Yin.Candidate>>): Result {
        val n = candidates.size
        val f0 = DoubleArray(n)
        val ap = DoubleArray(n) { 1.0 }
        val voiced = BooleanArray(n)
        if (n == 0) return Result(f0, ap, voiced)

        // State 0 = unvoiced, state k (k >= 1) = candidates[t][k - 1].
        val cost = ArrayList<DoubleArray>(n)
        val back = ArrayList<IntArray>(n)
        fun st(hz: Double) = 12 * log2(hz)

        val first = candidates[0]
        cost += DoubleArray(first.size + 1) { if (it == 0) UNVOICED_COST else first[it - 1].cost }
        back += IntArray(first.size + 1)

        for (t in 1 until n) {
            val prev = candidates[t - 1]
            val cur = candidates[t]
            val prevCost = cost[t - 1]
            val c = DoubleArray(cur.size + 1)
            val b = IntArray(cur.size + 1)
            for (s in 0..cur.size) {
                val local = if (s == 0) UNVOICED_COST else cur[s - 1].cost
                var best = Double.MAX_VALUE
                var arg = 0
                for (p in 0..prev.size) {
                    val trans = when {
                        s == 0 && p == 0 -> 0.0
                        s == 0 || p == 0 -> VOICING_SWITCH_COST
                        else -> {
                            val jump = abs(st(cur[s - 1].frequency) - st(prev[p - 1].frequency))
                            (jump - MAX_FREE_JUMP_ST).coerceAtLeast(0.0) * JUMP_COST_PER_ST
                        }
                    }
                    val total = prevCost[p] + trans
                    if (total < best) {
                        best = total
                        arg = p
                    }
                }
                c[s] = best + local
                b[s] = arg
            }
            cost += c
            back += b
        }

        var s = cost[n - 1].indices.minBy { cost[n - 1][it] }
        for (t in n - 1 downTo 0) {
            if (s > 0) {
                val cand = candidates[t][s - 1]
                f0[t] = cand.frequency
                ap[t] = cand.aperiodicity
                voiced[t] = true
            } else {
                ap[t] = candidates[t].firstOrNull()?.aperiodicity ?: 1.0
            }
            s = back[t][s]
        }
        return Result(f0, ap, voiced)
    }
}
