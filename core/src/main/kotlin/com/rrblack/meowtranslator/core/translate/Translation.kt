package com.rrblack.meowtranslator.core.translate

import com.rrblack.meowtranslator.core.analysis.Analysis
import com.rrblack.meowtranslator.core.analysis.Vocalization
import com.rrblack.meowtranslator.core.classify.MeowContext
import com.rrblack.meowtranslator.core.classify.VocalType

/** Emotional state on the two standard axes used in animal-emotion research. */
data class Emotion(
    /** -1 = negative, +1 = positive. */
    val valence: Double,
    /** 0 = calm, 1 = highly aroused. */
    val arousal: Double,
) {
    val label: String
        get() = when {
            valence > 0.2 && arousal >= 0.55 -> "Excited & happy"
            valence > 0.2 -> "Calm & content"
            valence < -0.2 && arousal >= 0.55 -> "Stressed / upset"
            valence < -0.2 -> "Uneasy / displeased"
            arousal >= 0.6 -> "Alert & eager"
            else -> "Neutral"
        }
}

data class Ranked<T>(val item: T, val probability: Double)

/** The full result of translating one recording. */
data class Translation(
    /** The translation itself, in the cat's voice. */
    val headline: String,
    val speaker: String,
    val intent: Intent,
    val intents: List<Ranked<Intent>>,
    val vocalType: VocalType,
    val vocalTypes: List<Ranked<VocalType>>,
    /** Data-model context probabilities (meows only). */
    val contexts: Map<MeowContext, Double>?,
    val emotion: Emotion,
    val intensity: Intensity,
    /** 0..1 — combines how decisive the models were with recording quality. */
    val confidence: Double,
    /** Why the app reached this reading, in plain language with sources. */
    val evidence: List<String>,
    /** Practical welfare / behaviour notes relevant to this call. */
    val notes: List<String>,
    /** Problems with the recording that reduce reliability. */
    val qualityWarnings: List<String>,
    val callCount: Int,
    val primary: Vocalization?,
    val analysis: Analysis,
    /** Number of owner-labelled examples that influenced this result. */
    val personalExamplesUsed: Int,
) {
    val confidenceLabel: String
        get() = when {
            confidence >= 0.65 -> "High"
            confidence >= 0.4 -> "Moderate"
            else -> "Low"
        }

    /** The feature vector of the main call, used when the owner teaches the app what really happened. */
    val learnableFeatures: DoubleArray? get() = primary?.features?.toModelVector()
}
