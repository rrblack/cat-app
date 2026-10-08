package com.rrblack.meowtranslator.core.classify

import com.rrblack.meowtranslator.core.analysis.SegmentFeatures
import com.rrblack.meowtranslator.core.analysis.Vocalization
import kotlin.math.exp

/**
 * Literature-informed rules that score each call type from acoustic features. There is no large public
 * dataset labelled by call type, so these encode published acoustic descriptions:
 *
 *  - Purr: amplitude pulsing at ~20–30 Hz, energy concentrated low, sustained (Sissom et al. 1991; Schötz & Eklund 2011).
 *  - Hiss: unvoiced, noise-like (high spectral flatness) with energy spread high (Tavernier et al. 2020).
 *  - Growl: long, low-pitched, rough/aperiodic, energy concentrated low (Moelk 1944; Schötz 2013).
 *  - Trill: short voiced call with "rolled" modulation, mouth closed (Moelk 1944; Schötz 2013).
 *  - Yowl: long (> ~2 s), loud, voiced with large pitch movement (Bradshaw & Cameron-Beaumont 2000).
 *  - Chatter: bursts of very short calls repeated several times per second (Schötz 2013).
 *  - Meow: voiced call of ~0.3–1.5 s with F0 typically 300–1000 Hz; validated against the 440 CatMeows meows.
 *
 * Returns soft scores (normalised to probabilities) so the UI can show how sure it is.
 */
object VocalTypeClassifier {

    fun classify(f: SegmentFeatures): Map<VocalType, Double> {
        val dur = f.durationSec
        val vf = f.voicedFraction
        val f0 = f.f0MedianHz
        val voiced = rise(vf, 0.3, 0.08)
        val pulsing = rise(f.amStrength, 0.22, 0.04) * band(f.amRateHz, 16.0, 38.0, 2.5)
        val lowHeavy = rise(f.lowBandRatio, 0.25, 0.06)

        // Breathy meows can lose their measurable pitch but keep a formant-shaped (non-flat) spectrum.
        val meowVoicing = maxOf(voiced, 0.5 * fall(f.spectralFlatness, 0.17, 0.025))
        val meowPitch = if (f0 > 0) rise(f0, 230.0, 30.0) * fall(f0, 1600.0, 100.0) else 0.75

        val scores = linkedMapOf(
            VocalType.MEOW to meowVoicing * meowPitch * rise(dur, 0.2, 0.05) * fall(dur, 2.2, 0.25) * (1 - 0.8 * pulsing),
            VocalType.TRILL to voiced * fall(dur, 0.55, 0.07) * rise(dur, 0.08, 0.02) *
                rise(f.amStrength, 0.16, 0.03) * band(f.amRateHz, 14.0, 40.0, 2.5),
            VocalType.PURR to pulsing * rise(dur, 0.8, 0.15) * lowHeavy * fall(f.spectralCentroidHz, 1300.0, 200.0),
            VocalType.HISS to (1 - voiced) * rise(f.spectralFlatness, 0.2, 0.035) * rise(f.aperiodicity, 0.45, 0.06) *
                rise(f.spectralCentroidHz, 1400.0, 200.0) * rise(dur, 0.15, 0.04),
            VocalType.GROWL to rise(dur, 0.5, 0.1) * lowHeavy * (if (f0 > 0) fall(f0, 260.0, 30.0) else 0.7) *
                (0.4 + 0.6 * rise(f.aperiodicity, 0.2, 0.06)) * (1 - 0.8 * pulsing),
            VocalType.YOWL to voiced * rise(dur, 2.1, 0.25) * rise(f0, 200.0, 40.0) * (0.5 + 0.5 * rise(f.f0RangeSt, 4.0, 1.5)),
        )
        // Small floor keeps every option visible and avoids divide-by-zero on odd sounds.
        val unknownScore = 0.04
        val total = scores.values.sum() + unknownScore
        val out = LinkedHashMap<VocalType, Double>()
        for ((k, v) in scores) out[k] = v / total
        out[VocalType.CHATTER] = 0.0
        out[VocalType.UNKNOWN] = unknownScore / total
        return out
    }

    /**
     * Chatter is a property of the whole bout: several very short calls in quick succession.
     * Returns a 0..1 likelihood.
     */
    fun chatterLikelihood(calls: List<Vocalization>): Double {
        if (calls.size < 3) return 0.0
        val short = calls.filter { it.features.durationSec < 0.16 }
        if (short.size < 3) return 0.0
        val onsets = short.map { it.startSec }.sorted()
        val gaps = onsets.zipWithNext { a, b -> b - a }
        val fastGaps = gaps.count { it in 0.05..0.4 }
        val shortShare = short.size.toDouble() / calls.size
        return (fastGaps.toDouble() / gaps.size) * rise(shortShare, 0.6, 0.1) * rise(short.size.toDouble(), 3.5, 0.7)
    }

    private fun sigmoid(x: Double) = 1.0 / (1.0 + exp(-x))
    internal fun rise(x: Double, center: Double, width: Double) = sigmoid((x - center) / width)
    internal fun fall(x: Double, center: Double, width: Double) = 1 - rise(x, center, width)
    internal fun band(x: Double, lo: Double, hi: Double, width: Double) = rise(x, lo, width) * fall(x, hi, width)
}
