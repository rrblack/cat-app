package com.rrblack.meowtranslator.core.analysis

/** Overall melodic shape of a call's pitch (F0) contour. */
enum class ContourShape { RISING, FALLING, RISE_FALL, FALL_RISE, FLAT, UNVOICED }

/** Summary acoustic description of one vocalization. */
data class SegmentFeatures(
    val durationSec: Double,
    /** Fraction of frames with a reliable pitch. */
    val voicedFraction: Double,
    val f0MedianHz: Double,
    val f0MinHz: Double,
    val f0MaxHz: Double,
    /** 10th–90th percentile pitch spread in semitones. */
    val f0RangeSt: Double,
    /** Linear trend of the pitch contour in semitones per second. */
    val f0SlopeStPerSec: Double,
    /** Mean pitch of the final quarter minus the first quarter, in semitones (> 0 = ends higher). */
    val f0EndMinusStartSt: Double,
    /** Where the pitch peak occurs, 0 = start, 1 = end. */
    val f0PeakPosition: Double,
    /** Mean absolute frame-to-frame pitch change (semitones) — vocal roughness / instability. */
    val f0Jitter: Double,
    val contourShape: ContourShape,
    /** Mean YIN aperiodicity: 0 = pure tone, 1 = noise. Low values = clear, tonal voice. */
    val aperiodicity: Double,
    /** Loudest frame relative to the recording's noise floor, in dB. */
    val dbAboveFloor: Double,
    val peakDbFs: Double,
    /** Spread of frame loudness (dB) — how much the call swells or fades. */
    val loudnessStdDb: Double,
    /** Where the loudest moment occurs, 0 = start, 1 = end. */
    val loudnessPeakPosition: Double,
    val spectralCentroidHz: Double,
    val spectralCentroidStdHz: Double,
    val spectralFlatness: Double,
    val spectralRolloffHz: Double,
    val highBandRatio: Double,
    val lowBandRatio: Double,
    /** Dominant amplitude-modulation rate (Hz) in the 12–45 Hz band, e.g. a purr's ~25 Hz pulsing. */
    val amRateHz: Double,
    /** Share of envelope-modulation energy concentrated around [amRateHz] (0..1). */
    val amStrength: Double,
    val mfccMean: DoubleArray,
    val mfccStd: DoubleArray,
    /** Loudness peaks per second; speech runs at ~3–7 syllables/s, a meow is usually a single swell. */
    val syllableRate: Double = 0.0,
    /** Unvoiced→voiced transitions per second; speech keeps breaking voicing for consonants. */
    val voicingOnsetRate: Double = 0.0,
    /** Share of voiced frames below 250 Hz — typical of adult human voices, rare in meows. */
    val lowPitchFraction: Double = 0.0,
    /** Mean frame-to-frame MFCC change: how fast the sound's colour changes (high for speech). */
    val spectralChangeRate: Double = 0.0,
) {
    /** Numeric vector for statistical models; see [FEATURE_NAMES]. Excludes absolute loudness (depends on mic gain). */
    fun toModelVector(): DoubleArray {
        val v = ArrayList<Double>(FEATURE_NAMES.size)
        v += kotlin.math.ln(durationSec.coerceAtLeast(0.01))
        v += voicedFraction
        v += semitones(f0MedianHz)
        v += semitones(f0MinHz)
        v += semitones(f0MaxHz)
        v += f0RangeSt
        v += f0SlopeStPerSec.coerceIn(-60.0, 60.0)
        v += f0EndMinusStartSt
        v += f0PeakPosition
        v += f0Jitter
        v += aperiodicity
        v += loudnessStdDb
        v += loudnessPeakPosition
        v += spectralCentroidHz / 1000.0
        v += spectralCentroidStdHz / 1000.0
        v += spectralFlatness
        v += spectralRolloffHz / 1000.0
        v += highBandRatio
        v += lowBandRatio
        v += amStrength
        for (i in 1 until mfccMean.size) v += mfccMean[i]
        for (i in 1 until mfccStd.size) v += mfccStd[i]
        return v.toDoubleArray()
    }

    /** Features for the cat / human / other detector: the model vector plus speech-vs-call cues. */
    fun toDetectorVector(): DoubleArray =
        toModelVector() + doubleArrayOf(syllableRate, voicingOnsetRate, lowPitchFraction, spectralChangeRate)

    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)

    companion object {
        /** Pitch in semitones above 100 Hz (0 when unvoiced). */
        fun semitones(hz: Double): Double = if (hz > 0) 12.0 * kotlin.math.log2(hz / 100.0) else 0.0

        val FEATURE_NAMES: List<String> = listOf(
            "log_duration", "voiced_fraction", "f0_median_st", "f0_min_st", "f0_max_st", "f0_range_st",
            "f0_slope", "f0_end_minus_start", "f0_peak_pos", "f0_jitter", "aperiodicity",
            "loudness_std", "loudness_peak_pos", "centroid_khz", "centroid_std_khz", "flatness", "rolloff_khz",
            "high_band", "low_band", "am_strength",
        ) + (1..12).map { "mfcc${it}_mean" } + (1..12).map { "mfcc${it}_std" }

        val DETECTOR_FEATURE_NAMES: List<String> =
            FEATURE_NAMES + listOf("syllable_rate", "voicing_onset_rate", "low_pitch_fraction", "spectral_change_rate")
    }
}
