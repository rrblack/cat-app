package com.rrblack.meowtranslator.core.translate

import com.rrblack.meowtranslator.core.analysis.Analysis
import com.rrblack.meowtranslator.core.analysis.ContourShape
import com.rrblack.meowtranslator.core.analysis.Vocalization
import com.rrblack.meowtranslator.core.classify.MeowContext
import com.rrblack.meowtranslator.core.classify.VocalType

/*
 * A translation is language-neutral data; [Texts] renders it in English or Japanese. That lets the UI
 * switch language instantly, including for results and history recorded in the other language.
 */

/** Intensity of the call, derived from loudness, length, pitch and repetition. */
enum class Intensity { LOW, MEDIUM, HIGH }

/** Which family of phrases the headline comes from. */
enum class PhraseKey {
    GREETING, FOOD, ATTENTION, HANDLING_POSITIVE, HANDLING_NEGATIVE, DISTRESS, COMPLAINT, CONTENT, SOLICITING,
    FRIENDLY, HUNTING, DEFENSIVE, THREAT, CALLING_OUT_DISTRESS, CALLING_OUT_MATING,

    /** Not a translation: nothing audible, a person talking, or a sound that isn't a cat. */
    UNCLEAR, HUMAN_VOICE, NOT_A_CAT,
}

/** The first-person translation, chosen deterministically so the same sound always reads the same way. */
data class Headline(
    val key: PhraseKey,
    val intensity: Intensity,
    val seed: Int,
    /** Repeated meowing: adds "(and I'll keep asking)". */
    val insistent: Boolean = false,
)

enum class EmotionLabel { EXCITED_HAPPY, CALM_CONTENT, STRESSED, UNEASY, ALERT, NEUTRAL }

/** Emotional state on the two standard axes used in animal-emotion research. */
data class Emotion(
    /** -1 = negative, +1 = positive. */
    val valence: Double,
    /** 0 = calm, 1 = highly aroused. */
    val arousal: Double,
) {
    val label: EmotionLabel
        get() = when {
            valence > 0.2 && arousal >= 0.55 -> EmotionLabel.EXCITED_HAPPY
            valence > 0.2 -> EmotionLabel.CALM_CONTENT
            valence < -0.2 && arousal >= 0.55 -> EmotionLabel.STRESSED
            valence < -0.2 -> EmotionLabel.UNEASY
            arousal >= 0.6 -> EmotionLabel.ALERT
            else -> EmotionLabel.NEUTRAL
        }
}

enum class ConfidenceLevel { HIGH, MODERATE, LOW }

/** One reason behind a translation. */
sealed interface Evidence {
    data class CallType(val type: VocalType, val probability: Double, val durationSec: Double, val f0Hz: Double) : Evidence
    data class DataModel(val context: MeowContext, val probability: Double, val trainingCalls: Int, val trainingCats: Int) : Evidence
    data class Melody(val shape: ContourShape, val endMinusStartSt: Double) : Evidence
    data object ShortCall : Evidence
    data object LongCall : Evidence
    data object RoughVoice : Evidence
    data class PurrPulsing(val rateHz: Double) : Evidence
    data class TrillFlutter(val rateHz: Double) : Evidence
    data object HissNoise : Evidence
    data object GrowlLowRough : Evidence
    data object YowlLong : Evidence
    data object ChatterBurst : Evidence
    data class Repeated(val count: Int) : Evidence
    data class PersonalMatch(val intent: Intent, val examples: Int) : Evidence
    data class PersonalWeight(val examples: Int, val weight: Double) : Evidence
    /** Sounds in the recording that were left out because they weren't the cat. */
    data class IgnoredSounds(val human: Int, val other: Int) : Evidence
}

/** Practical welfare / behaviour notes. */
enum class Note { PURR_CAN_MEAN_STRESS, GIVE_SPACE, SENIOR_YOWLING, INTACT_YOWLING, CHATTER_PLAY, KITTEN_MEOWING, DISTRESS_VET, BODY_LANGUAGE }

/** Problems with the recording that reduce reliability. */
enum class QualityWarning { NO_SOUND, CLIPPED, NOISY, TOO_QUIET }

data class Ranked<T>(val item: T, val probability: Double)

/** The full result of translating one recording. */
data class Translation(
    val headline: Headline,
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
    val evidence: List<Evidence>,
    val notes: List<Note>,
    val qualityWarnings: List<QualityWarning>,
    val callCount: Int,
    val primary: Vocalization?,
    val analysis: Analysis,
    /** Number of owner-labelled examples that influenced this result. */
    val personalExamplesUsed: Int,
) {
    val confidenceLevel: ConfidenceLevel
        get() = when {
            confidence >= 0.65 -> ConfidenceLevel.HIGH
            confidence >= 0.4 -> ConfidenceLevel.MODERATE
            else -> ConfidenceLevel.LOW
        }

    /** The feature vector of the main call, used when the owner teaches the app what really happened. */
    val learnableFeatures: DoubleArray? get() = primary?.features?.toModelVector()
}
