package com.rrblack.meowtranslator.core.classify

import com.rrblack.meowtranslator.core.analysis.SegmentFeatures

/** Who or what made a sound. */
enum class SoundSource { CAT, HUMAN, OTHER }

/**
 * Decides whether a detected sound is a cat at all, before anything gets translated.
 *
 * Trained (see :trainer TrainCallDetector) on cat calls from CatMeows and ESC-50, human speech from
 * LibriSpeech plus human non-speech sounds from ESC-50 (laughing, coughing, a crying baby…), and
 * household / outdoor noises from ESC-50 — each also replayed through simulated room echo and noise.
 */
object CallDetector {
    private val model = SoftmaxRegression(
        classes = CallDetectorWeights.CLASSES,
        mean = CallDetectorWeights.MEAN,
        scale = CallDetectorWeights.SCALE,
        weights = CallDetectorWeights.WEIGHTS,
        bias = CallDetectorWeights.BIAS,
    )

    /** Minimum P(cat) for a sound to be treated as the cat; chosen in training to keep most real meows. */
    val catThreshold: Double get() = CallDetectorWeights.CAT_THRESHOLD

    fun predict(features: SegmentFeatures): Map<SoundSource, Double> {
        val p = model.predict(features.toDetectorVector())
        return CallDetectorWeights.CLASSES.withIndex().associate { (i, name) -> SoundSource.valueOf(name) to p[i] }
    }

    fun isCat(features: SegmentFeatures): Boolean = (predict(features)[SoundSource.CAT] ?: 0.0) >= catThreshold

    /** Held-out, whole-recording evaluation numbers (share of recordings handled correctly), for the How-it-works screen. */
    val catRecall: Double get() = CallDetectorWeights.EVAL_CAT_RECALL
    val otherCatRecall: Double get() = CallDetectorWeights.EVAL_ESC_CAT_RECALL
    val speechRejection: Double get() = CallDetectorWeights.EVAL_SPEECH_REJECTION
    val noiseRejection: Double get() = CallDetectorWeights.EVAL_OTHER_REJECTION
    val humanSoundRejection: Double get() = CallDetectorWeights.EVAL_HUMAN_SOUND_REJECTION
}
