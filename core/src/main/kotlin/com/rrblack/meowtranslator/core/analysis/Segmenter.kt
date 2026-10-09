package com.rrblack.meowtranslator.core.analysis

import com.rrblack.meowtranslator.core.dsp.FrameTrack

/** A contiguous run of frames that contains sound well above the background noise. */
data class FrameSpan(val start: Int, val endExclusive: Int) {
    val length: Int get() = endExclusive - start
}

/**
 * Energy-based voice activity detection with hysteresis, adaptive to the recording's own noise floor.
 */
object Segmenter {
    /** Frames quieter than this (dBFS) are never treated as a vocalization, however quiet the room is. */
    const val ABSOLUTE_MIN_DB = -62.0

    /** A call must rise at least this far above the noise floor (every CatMeows clip clears 19 dB). */
    const val MIN_DYNAMIC_RANGE_DB = 12.0

    /** How far below its own peak a call is followed before the rest is treated as echo or background. */
    const val CALL_RANGE_DB = 25.0

    data class Result(val spans: List<FrameSpan>, val noiseFloorDb: Double, val peakDb: Double, val thresholdDb: Double)

    fun segment(
        track: FrameTrack,
        minDurationSec: Double = 0.04,
        mergeGapSec: Double = 0.05,
    ): Result {
        val db = track.rmsDb
        if (db.isEmpty()) return Result(emptyList(), -120.0, -120.0, 0.0)
        val floor = Stats.percentile(db, 10.0)
        val peak = db.max()
        // Nothing stands out from the background (silence, or steady noise like a fan).
        if (peak - floor < MIN_DYNAMIC_RANGE_DB) return Result(emptyList(), floor, peak, peak)
        val threshold = maxOf(ABSOLUTE_MIN_DB, floor + 9.0, peak - 40.0)
        val release = threshold - 3.0

        val raw = mutableListOf<FrameSpan>()
        var inside = false
        var start = 0
        for (i in db.indices) {
            if (!inside && db[i] >= threshold) {
                inside = true
                start = i
            } else if (inside && db[i] < release) {
                inside = false
                raw += FrameSpan(start, i)
            }
        }
        if (inside) raw += FrameSpan(start, db.size)

        val framesPerSec = track.sampleRate.toDouble() / track.hopSize
        val mergeGap = (mergeGapSec * framesPerSec).toInt()
        val merged = mutableListOf<FrameSpan>()
        for (s in raw) {
            val last = merged.lastOrNull()
            if (last != null && s.start - last.endExclusive <= mergeGap) {
                merged[merged.size - 1] = FrameSpan(last.start, s.endExclusive)
            } else {
                merged += s
            }
        }
        val minFrames = (minDurationSec * framesPerSec).toInt().coerceAtLeast(2)
        val trimmed = merged.map { trim(db, it) }
        return Result(trimmed.filter { it.length >= minFrames }, floor, peak, threshold)
    }

    /**
     * Cuts a call down to the part within [CALL_RANGE_DB] of its own peak. In a quiet but echoey room the
     * reverberation tail otherwise stays above the threshold and its noise-like sound gets mistaken for a hiss.
     */
    private fun trim(db: DoubleArray, span: FrameSpan): FrameSpan {
        var peak = Double.NEGATIVE_INFINITY
        for (i in span.start until span.endExclusive) peak = maxOf(peak, db[i])
        val floor = peak - CALL_RANGE_DB
        var start = span.start
        var end = span.endExclusive
        while (start < end - 1 && db[start] < floor) start++
        while (end - 1 > start && db[end - 1] < floor) end--
        return FrameSpan(start, end)
    }
}
