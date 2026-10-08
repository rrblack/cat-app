package com.rrblack.meowtranslator.core.analysis

import com.rrblack.meowtranslator.core.dsp.Fft
import com.rrblack.meowtranslator.core.dsp.FrameAnalyzer
import com.rrblack.meowtranslator.core.dsp.FrameTrack
import com.rrblack.meowtranslator.core.dsp.Resampler
import kotlin.math.abs

/** A point on a pitch contour (for plotting). */
data class PitchPoint(val timeSec: Double, val hz: Double)

/** One detected vocalization within a recording. */
data class Vocalization(
    val startSec: Double,
    val endSec: Double,
    val features: SegmentFeatures,
    /** Smoothed pitch contour (voiced frames only). */
    val pitch: List<PitchPoint>,
)

/** Everything measured about a recording. */
data class Analysis(
    val durationSec: Double,
    val noiseFloorDb: Double,
    val peakDb: Double,
    /** Fraction of samples at or near full scale (distortion makes features unreliable). */
    val clippedFraction: Double,
    val vocalizations: List<Vocalization>,
    /** Loudness envelope (dBFS, 10 ms steps) for drawing a waveform overview. */
    val envelopeDb: DoubleArray,
) {
    val signalToNoiseDb: Double get() = peakDb - noiseFloorDb

    /** The most prominent call: the longest among those within 10 dB of the loudest. */
    val primary: Vocalization?
        get() {
            val loudest = vocalizations.maxOfOrNull { it.features.peakDbFs } ?: return null
            return vocalizations.filter { it.features.peakDbFs >= loudest - 10 }.maxBy { it.features.durationSec }
        }
}

/**
 * Turns raw audio into a list of [Vocalization]s with prosodic and spectral features.
 * All analysis happens at [SAMPLE_RATE], matching the CatMeows training data.
 */
class AcousticAnalyzer {
    private val frameAnalyzer = FrameAnalyzer(SAMPLE_RATE)

    fun analyze(samples: FloatArray, sampleRate: Int): Analysis {
        val audio = if (sampleRate == SAMPLE_RATE) samples.copyOf() else Resampler.resample(samples, sampleRate, SAMPLE_RATE)
        removeDc(audio)
        val track = frameAnalyzer.analyze(audio)
        val seg = Segmenter.segment(track)
        val clipped = audio.count { abs(it) >= 0.99f }.toDouble() / audio.size.coerceAtLeast(1)
        val vocalizations = seg.spans.map { describe(track, audio, it, seg) }
        return Analysis(
            durationSec = audio.size.toDouble() / SAMPLE_RATE,
            noiseFloorDb = seg.noiseFloorDb,
            peakDb = seg.peakDb,
            clippedFraction = clipped,
            vocalizations = vocalizations,
            envelopeDb = track.rmsDb,
        )
    }

    private fun removeDc(x: FloatArray) {
        if (x.isEmpty()) return
        // One-pole high-pass at ~40 Hz removes DC offset and handling rumble without touching purr harmonics.
        val r = 0.97f
        var prevIn = x[0]
        var prevOut = 0f
        for (i in x.indices) {
            val cur = x[i]
            val out = cur - prevIn + r * prevOut
            prevIn = cur
            prevOut = out
            x[i] = out
        }
    }

    private fun describe(track: FrameTrack, audio: FloatArray, span: FrameSpan, seg: Segmenter.Result): Vocalization {
        val idx = (span.start until span.endExclusive).toList()
        val n = idx.size
        val db = DoubleArray(n) { track.rmsDb[idx[it]] }

        // Voiced = clearly periodic and loud enough that the pitch estimate means something.
        val voicedMask = BooleanArray(n) {
            track.voiced[idx[it]] && track.aperiodicity[idx[it]] < VOICED_APERIODICITY && db[it] >= seg.thresholdDb - 3
        }
        val voicedIdx = (0 until n).filter { voicedMask[it] }
        var f0 = DoubleArray(voicedIdx.size) { track.f0[idx[voicedIdx[it]]] }
        if (f0.size >= 3) f0 = Stats.medianFilter(f0, 2)
        val voicedTimes = DoubleArray(voicedIdx.size) { track.timeOf(idx[voicedIdx[it]]) }
        val st = DoubleArray(f0.size) { SegmentFeatures.semitones(f0[it]) }

        val durationSec = n * track.hopSize.toDouble() / track.sampleRate
        val voicedFraction = voicedIdx.size.toDouble() / n
        val hasPitch = f0.size >= 3 && voicedFraction >= 0.2

        var slope = 0.0
        var endMinusStart = 0.0
        var peakPos = 0.5
        var jitter = 0.0
        var range = 0.0
        var shape = ContourShape.UNVOICED
        if (hasPitch) {
            slope = Stats.slope(voicedTimes, st)
            val q = (st.size / 4).coerceAtLeast(1)
            val startMean = Stats.mean(st.copyOfRange(0, q))
            val endMean = Stats.mean(st.copyOfRange(st.size - q, st.size))
            endMinusStart = endMean - startMean
            val peakI = st.indices.maxBy { st[it] }
            val troughI = st.indices.minBy { st[it] }
            val tStart = voicedTimes.first()
            val tSpan = (voicedTimes.last() - tStart).coerceAtLeast(1e-6)
            peakPos = (voicedTimes[peakI] - tStart) / tSpan
            val troughPos = (voicedTimes[troughI] - tStart) / tSpan
            range = Stats.percentile(st, 90.0) - Stats.percentile(st, 10.0)
            var j = 0.0
            for (i in 1 until st.size) j += abs(st[i] - st[i - 1])
            jitter = j / (st.size - 1)
            shape = when {
                st[peakI] - maxOf(startMean, endMean) > SHAPE_ST && peakPos in 0.15..0.85 -> ContourShape.RISE_FALL
                endMinusStart > SHAPE_ST -> ContourShape.RISING
                endMinusStart < -SHAPE_ST -> ContourShape.FALLING
                minOf(startMean, endMean) - st[troughI] > SHAPE_ST && troughPos in 0.15..0.85 -> ContourShape.FALL_RISE
                else -> ContourShape.FLAT
            }
        }

        val loudPeakI = (0 until n).maxBy { db[it] }
        val am = amplitudeModulation(audio, span, track)

        val mfccMean = DoubleArray(13)
        val mfccStd = DoubleArray(13)
        for (c in 0 until 13) {
            val col = DoubleArray(n) { track.mfcc[idx[it]][c] }
            mfccMean[c] = Stats.mean(col)
            mfccStd[c] = Stats.std(col)
        }
        fun avg(a: DoubleArray) = Stats.mean(DoubleArray(n) { a[idx[it]] })

        val features = SegmentFeatures(
            durationSec = durationSec,
            voicedFraction = voicedFraction,
            f0MedianHz = if (hasPitch) Stats.median(f0) else 0.0,
            f0MinHz = if (hasPitch) Stats.percentile(f0, 5.0) else 0.0,
            f0MaxHz = if (hasPitch) Stats.percentile(f0, 95.0) else 0.0,
            f0RangeSt = range,
            f0SlopeStPerSec = slope,
            f0EndMinusStartSt = endMinusStart,
            f0PeakPosition = peakPos,
            f0Jitter = jitter,
            contourShape = shape,
            aperiodicity = avg(track.aperiodicity),
            dbAboveFloor = db.max() - seg.noiseFloorDb,
            peakDbFs = db.max(),
            loudnessStdDb = Stats.std(db),
            loudnessPeakPosition = if (n > 1) loudPeakI.toDouble() / (n - 1) else 0.5,
            spectralCentroidHz = avg(track.centroid),
            spectralCentroidStdHz = Stats.std(DoubleArray(n) { track.centroid[idx[it]] }),
            spectralFlatness = avg(track.flatness),
            spectralRolloffHz = avg(track.rolloff),
            highBandRatio = avg(track.highBandRatio),
            lowBandRatio = avg(track.lowBandRatio),
            amRateHz = am.first,
            amStrength = am.second,
            mfccMean = mfccMean,
            mfccStd = mfccStd,
        )
        val pitch = if (hasPitch) voicedTimes.indices.map { PitchPoint(voicedTimes[it], f0[it]) } else emptyList()
        return Vocalization(
            startSec = track.timeOf(span.start) - track.frameSize / 2.0 / track.sampleRate,
            endSec = track.timeOf(span.endExclusive - 1) + track.frameSize / 2.0 / track.sampleRate,
            features = features,
            pitch = pitch,
        )
    }

    /**
     * Envelope modulation spectrum. Purrs are a train of glottal pulses at ~20–30 Hz and trills are
     * "rolled" with a similar flutter, while meows have smooth envelopes.
     * Returns (dominant rate in 12–45 Hz, share of 2–100 Hz modulation energy near that rate).
     */
    private fun amplitudeModulation(audio: FloatArray, span: FrameSpan, track: FrameTrack): Pair<Double, Double> {
        val from = span.start * track.hopSize
        val to = minOf(audio.size, span.endExclusive * track.hopSize + track.frameSize)
        val win = SAMPLE_RATE / ENVELOPE_RATE
        val count = (to - from) / win
        if (count < 16) return 0.0 to 0.0
        val env = DoubleArray(count) { FrameAnalyzer.rms(audio, from + it * win, from + (it + 1) * win) }
        val mean = env.average()
        val hann = Fft.hann(count)
        val size = Fft.nextPowerOfTwo(maxOf(count * 2, 512))
        val p = Fft.powerSpectrum(DoubleArray(count) { (env[it] - mean) * hann[it] }, size)
        val binHz = ENVELOPE_RATE.toDouble() / size
        fun bin(hz: Double) = (hz / binHz).toInt().coerceIn(0, p.size - 1)
        var total = 0.0
        for (k in bin(2.0)..bin(100.0)) total += p[k]
        if (total <= 0) return 0.0 to 0.0
        var peak = bin(12.0)
        for (k in bin(12.0)..bin(45.0)) if (p[k] > p[peak]) peak = k
        val halfWidth = (2.5 / binHz).toInt().coerceAtLeast(1)
        var near = 0.0
        for (k in (peak - halfWidth)..(peak + halfWidth)) near += p[k.coerceIn(0, p.size - 1)]
        return peak * binHz to (near / total).coerceIn(0.0, 1.0)
    }

    companion object {
        const val SAMPLE_RATE = 8000
        private const val ENVELOPE_RATE = 400
        private const val VOICED_APERIODICITY = 0.45
        private const val SHAPE_ST = 1.5
    }
}
