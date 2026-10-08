package com.rrblack.meowtranslator.core.dsp

/**
 * YIN fundamental-frequency analysis (de Cheveigné & Kawahara, 2002).
 *
 * Instead of committing to one F0 per frame, [candidates] returns every dip of the cumulative-mean-normalised
 * difference function so that [PitchTracker] can choose a smooth path through them. This avoids the octave
 * and harmonic jumps plain YIN makes on cat calls, whose strong formants often dominate the true fundamental.
 */
class Yin(
    private val sampleRate: Int,
    minFreq: Double,
    maxFreq: Double,
) {
    val maxLag: Int = (sampleRate / minFreq).toInt()
    val minLag: Int = (sampleRate / maxFreq).toInt().coerceAtLeast(2)

    private companion object {
        const val SUBHARMONIC_PENALTY = 0.15
        const val SUBHARMONIC_TOLERANCE = 0.08
    }

    /**
     * A pitch hypothesis; [aperiodicity] is 0 for a perfectly periodic signal and ~1 for noise.
     * [cost] is the aperiodicity plus a penalty if this lag is just a multiple of an equally good shorter one.
     */
    data class Candidate(val frequency: Double, val aperiodicity: Double, val cost: Double = aperiodicity)

    fun candidates(frame: FloatArray, offset: Int, length: Int, maxCandidates: Int = 6, ceiling: Double = 0.7): List<Candidate> {
        val w = length - maxLag
        require(w > maxLag / 2) { "Frame too short for YIN: length=$length maxLag=$maxLag" }
        val cmnd = DoubleArray(maxLag + 2)
        cmnd[0] = 1.0
        var running = 0.0
        for (tau in 1..maxLag + 1) {
            var sum = 0.0
            if (offset + w + tau <= frame.size) {
                for (j in 0 until w) {
                    val diff = frame[offset + j] - frame[offset + j + tau]
                    sum += diff * diff
                }
            }
            running += sum
            cmnd[tau] = if (running > 0) sum * tau / running else 1.0
        }

        val out = ArrayList<Candidate>()
        for (tau in minLag..maxLag) {
            if (cmnd[tau] < ceiling && cmnd[tau] <= cmnd[tau - 1] && cmnd[tau] <= cmnd[tau + 1]) {
                val a = cmnd[tau - 1]
                val b = cmnd[tau]
                val c = cmnd[tau + 1]
                val denom = a - 2 * b + c
                val shift = if (kotlin.math.abs(denom) > 1e-12) (0.5 * (a - c) / denom).coerceIn(-0.5, 0.5) else 0.0
                out += Candidate(sampleRate / (tau + shift), b.coerceIn(0.0, 1.0))
            }
        }
        // A signal periodic at lag T is also periodic at 2T, 3T…; like classic YIN, prefer the shortest such lag.
        // The preference fades out as the shorter lag gets less periodic, so a strong formant harmonic
        // can't steal the pitch from a clearly better fundamental.
        val scored = out.map { b ->
            var penalty = 0.0
            for (a in out) {
                val margin = (b.aperiodicity + SUBHARMONIC_TOLERANCE - a.aperiodicity) / SUBHARMONIC_TOLERANCE
                if (a.frequency <= b.frequency || margin <= 0) continue
                val m = a.frequency / b.frequency
                val k = kotlin.math.round(m)
                if (k >= 2 && kotlin.math.abs(m - k) < 0.04 * k) {
                    penalty = maxOf(penalty, SUBHARMONIC_PENALTY * kotlin.math.log2(k) * margin.coerceAtMost(1.0))
                }
            }
            b.copy(cost = b.aperiodicity + penalty)
        }.sortedBy { it.cost }
        return if (scored.size > maxCandidates) scored.subList(0, maxCandidates) else scored
    }
}
