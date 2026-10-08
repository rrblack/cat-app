package com.rrblack.meowtranslator.core.translate

import com.rrblack.meowtranslator.core.analysis.Analysis
import com.rrblack.meowtranslator.core.analysis.ContourShape
import com.rrblack.meowtranslator.core.analysis.SegmentFeatures
import com.rrblack.meowtranslator.core.classify.ContextModel
import com.rrblack.meowtranslator.core.classify.MeowContext
import com.rrblack.meowtranslator.core.classify.PersonalModel
import com.rrblack.meowtranslator.core.classify.VocalType
import com.rrblack.meowtranslator.core.classify.VocalTypeClassifier
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Combines three sources of evidence into a translation:
 *  1. Call type, from literature-based acoustic rules ([VocalTypeClassifier]).
 *  2. For meows, a statistical model trained on real meows recorded in known situations ([ContextModel])
 *     plus prosodic cues reported in the research literature (pitch contour, length, voice quality).
 *  3. The owner's own labelled examples for this cat ([PersonalModel]), which increasingly dominate.
 */
class Translator(
    /** Meow-context model; injectable so the trainer can evaluate cross-validated variants. */
    private val contextModel: (SegmentFeatures) -> Map<MeowContext, Double> = ContextModel::predict,
) {

    fun translate(analysis: Analysis, profile: CatProfile = CatProfile(), personal: PersonalModel? = null): Translation {
        val calls = analysis.vocalizations.filter { it.features.dbAboveFloor >= 12 }
        val primary = analysis.primary
        val quality = qualityWarnings(analysis)
        if (primary == null || calls.isEmpty()) return unclear(analysis, quality)
        val f = primary.features

        // 1. Call type (chatter is judged across the whole bout).
        val typeProbs = VocalTypeClassifier.classify(f).toMutableMap()
        val chatter = VocalTypeClassifier.chatterLikelihood(calls)
        if (chatter > 0) {
            for (k in typeProbs.keys) typeProbs[k] = typeProbs.getValue(k) * (1 - chatter)
            typeProbs[VocalType.CHATTER] = chatter
        }
        val vocalType = typeProbs.maxBy { it.value }.key

        // 2. Intent given each call type, mixed by how likely each type is.
        val contexts = contextModel(f)
        val features = f.toModelVector()
        val personalPrediction = personal?.predict(features)
        val personalWeight = if (personalPrediction != null) personal.weight else 0.0
        val meowIntents = meowIntents(f, contexts, personalPrediction, personalWeight)

        val intentProbs = HashMap<Intent, Double>()
        for ((type, p) in typeProbs) {
            if (p <= 0) continue
            for ((intent, q) in intentsFor(type, f, profile, meowIntents)) {
                intentProbs[intent] = (intentProbs[intent] ?: 0.0) + p * q
            }
        }
        val rankedIntents = intentProbs.entries.sortedByDescending { it.value }.map { Ranked(it.key, it.value) }
        val intent = rankedIntents.first().item

        // 3. Emotion and intensity.
        val emotion = emotion(f, vocalType, contexts, calls.size)
        val intensity = when {
            emotion.arousal >= 0.66 -> Intensity.HIGH
            emotion.arousal >= 0.38 -> Intensity.MEDIUM
            else -> Intensity.LOW
        }

        val seed = (f.durationSec * 1000).roundToInt() * 31 + f.f0MedianHz.roundToInt() * 7 + calls.size
        val headline = Headline(phraseKey(intent, emotion, profile), intensity, seed, calls.size >= 3 && vocalType == VocalType.MEOW)

        val qualityFactor = qualityFactor(analysis, f)
        val confidence = (rankedIntents.first().probability * 0.6 + typeProbs.getValue(vocalType) * 0.4) * qualityFactor

        return Translation(
            headline = headline,
            intent = intent,
            intents = rankedIntents,
            vocalType = vocalType,
            vocalTypes = typeProbs.entries.sortedByDescending { it.value }.map { Ranked(it.key, it.value) },
            contexts = if (vocalType == VocalType.MEOW) contexts else null,
            emotion = emotion,
            intensity = intensity,
            confidence = confidence.coerceIn(0.0, 0.97),
            evidence = evidence(f, vocalType, typeProbs.getValue(vocalType), contexts, calls.size, personal, personalPrediction, intent),
            notes = notes(vocalType, intent, emotion, profile),
            qualityWarnings = quality,
            callCount = calls.size,
            primary = primary,
            analysis = analysis,
            personalExamplesUsed = if (vocalType == VocalType.MEOW) personal?.size ?: 0 else 0,
        )
    }

    /** Probability of each meow intent, blending the data model, prosodic cues and the owner's examples. */
    fun meowIntents(
        f: SegmentFeatures,
        contexts: Map<MeowContext, Double> = contextModel(f),
        personal: Map<Intent, Double>? = null,
        personalWeight: Double = 0.0,
    ): Map<Intent, Double> {
        val cues = Cues(f)
        // Prosodic cues from the literature; deliberately soft because published effects are tendencies.
        val prosody = normalize(
            mapOf(
                Intent.GREETING to cues.short * (0.4 + 0.6 * cues.rising) * (1 - cues.harsh) * (1 - 0.5 * cues.loud),
                Intent.ATTENTION to 0.1 + 0.8 * (1 - cues.short) * cues.rising * (1 - cues.harsh),
                Intent.FOOD to 0.1 + 0.5 * cues.rising,
                Intent.DISTRESS to cues.falling * (0.3 + 0.7 * cues.long) * (0.5 + 0.5 * cues.loud),
                Intent.COMPLAINT to cues.falling * (0.3 + 0.7 * cues.harsh) * (0.6 + 0.4 * cues.low),
                Intent.HANDLING to 0.15,
            ),
        )
        // The trained model only knows three situations; map them onto intents.
        val model = mapOf(
            Intent.FOOD to contexts.getValue(MeowContext.FOOD),
            Intent.HANDLING to contexts.getValue(MeowContext.BRUSHING),
            Intent.DISTRESS to contexts.getValue(MeowContext.ISOLATION),
        )
        val general = normalize(Intent.MEOW_INTENTS.associateWith { 0.6 * (model[it] ?: 0.0) + 0.4 * (prosody[it] ?: 0.0) })
        if (personal == null || personalWeight <= 0) return general
        val keys = general.keys + personal.keys
        return normalize(keys.associateWith { (1 - personalWeight) * (general[it] ?: 0.0) + personalWeight * (personal[it] ?: 0.0) })
    }

    private fun intentsFor(type: VocalType, f: SegmentFeatures, profile: CatProfile, meow: Map<Intent, Double>): Map<Intent, Double> =
        when (type) {
            VocalType.MEOW -> meow
            VocalType.PURR -> {
                // Solicitation purrs carry a voiced, cry-like component in the ~220–520 Hz region (McComb et al. 2009).
                val cry = rise(f.voicedFraction, 0.25, 0.07) * (if (f.f0MedianHz in 200.0..1000.0) 1.0 else 0.2)
                mapOf(Intent.CONTENT to 1 - 0.85 * cry, Intent.SOLICITING to 0.85 * cry)
            }
            VocalType.TRILL -> mapOf(Intent.FRIENDLY to 0.8, Intent.GREETING to 0.2)
            VocalType.HISS -> mapOf(Intent.DEFENSIVE to 0.9, Intent.THREAT to 0.1)
            VocalType.GROWL -> mapOf(Intent.THREAT to 0.85, Intent.DEFENSIVE to 0.15)
            VocalType.YOWL -> mapOf(Intent.CALLING_OUT to 0.85, Intent.DISTRESS to 0.15)
            VocalType.CHATTER -> mapOf(Intent.HUNTING to 1.0)
            VocalType.UNKNOWN -> mapOf(Intent.UNCLEAR to 1.0)
        }

    private fun emotion(f: SegmentFeatures, type: VocalType, contexts: Map<MeowContext, Double>, calls: Int): Emotion {
        val cues = Cues(f)
        val repetition = rise(calls.toDouble(), 2.5, 0.8)
        val baseArousal = when (type) {
            VocalType.PURR -> 0.15
            VocalType.TRILL -> 0.45
            VocalType.HISS, VocalType.GROWL -> 0.8
            VocalType.YOWL -> 0.85
            VocalType.CHATTER -> 0.75
            VocalType.MEOW, VocalType.UNKNOWN -> 0.4
        }
        val pitchHigh = if (f.f0MedianHz > 0) rise(f.f0MedianHz, 650.0, 120.0) else 0.5
        val arousal = (0.45 * baseArousal + 0.2 * cues.loud + 0.15 * cues.long + 0.1 * pitchHigh + 0.1 * repetition +
            if (type == VocalType.MEOW) 0.1 * (cues.loud + repetition) else 0.0).coerceIn(0.0, 1.0)

        val baseValence = when (type) {
            VocalType.PURR -> 0.6
            VocalType.TRILL -> 0.7
            VocalType.HISS -> -0.8
            VocalType.GROWL -> -0.85
            VocalType.YOWL -> -0.4
            VocalType.CHATTER -> 0.0
            VocalType.MEOW, VocalType.UNKNOWN -> 0.0
        }
        val meowValence = if (type == VocalType.MEOW) {
            // Rising contours ~ positive, falling ~ negative (Schötz et al.); rough voice and isolation-like meows ~ negative.
            0.45 * (cues.rising - cues.falling) - 0.35 * cues.harsh - 0.5 * (contexts.getValue(MeowContext.ISOLATION) - 0.33)
        } else {
            0.0
        }
        return Emotion((baseValence + meowValence).coerceIn(-1.0, 1.0), arousal)
    }

    private fun phraseKey(intent: Intent, emotion: Emotion, profile: CatProfile): PhraseKey = when (intent) {
        Intent.HANDLING -> if (emotion.valence >= 0) PhraseKey.HANDLING_POSITIVE else PhraseKey.HANDLING_NEGATIVE
        Intent.CALLING_OUT ->
            if (profile.neutered == false && profile.ageGroup != AgeGroup.KITTEN) PhraseKey.CALLING_OUT_MATING else PhraseKey.CALLING_OUT_DISTRESS
        else -> PhraseKey.valueOf(intent.name)
    }

    private fun evidence(
        f: SegmentFeatures,
        type: VocalType,
        typeP: Double,
        contexts: Map<MeowContext, Double>,
        calls: Int,
        personal: PersonalModel?,
        personalPrediction: Map<Intent, Double>?,
        intent: Intent,
    ): List<Evidence> {
        val out = mutableListOf<Evidence>()
        out += Evidence.CallType(type, typeP, f.durationSec, f.f0MedianHz)
        when (type) {
            VocalType.MEOW -> {
                val top = contexts.maxBy { it.value }
                out += Evidence.DataModel(top.key, top.value, ContextModel.trainingCalls, ContextModel.trainingCats)
                if (f.contourShape != ContourShape.UNVOICED) out += Evidence.Melody(f.contourShape, f.f0EndMinusStartSt)
                if (f.durationSec < 0.45) out += Evidence.ShortCall
                if (f.durationSec > 1.0) out += Evidence.LongCall
                if (f.aperiodicity > 0.32) out += Evidence.RoughVoice
            }
            VocalType.PURR -> out += Evidence.PurrPulsing(f.amRateHz)
            VocalType.TRILL -> out += Evidence.TrillFlutter(f.amRateHz)
            VocalType.HISS -> out += Evidence.HissNoise
            VocalType.GROWL -> out += Evidence.GrowlLowRough
            VocalType.YOWL -> out += Evidence.YowlLong
            VocalType.CHATTER -> out += Evidence.ChatterBurst
            VocalType.UNKNOWN -> {}
        }
        if (calls >= 2) out += Evidence.Repeated(calls)
        if (personal != null && personalPrediction != null && type == VocalType.MEOW) {
            val match = personalPrediction[intent]
            out += if (match != null && match > 0.4) {
                Evidence.PersonalMatch(intent, personal.size)
            } else {
                Evidence.PersonalWeight(personal.size, personal.weight)
            }
        }
        return out
    }

    private fun notes(type: VocalType, intent: Intent, emotion: Emotion, profile: CatProfile): List<Note> {
        val out = mutableListOf<Note>()
        when (type) {
            VocalType.PURR -> out += Note.PURR_CAN_MEAN_STRESS
            VocalType.HISS, VocalType.GROWL -> out += Note.GIVE_SPACE
            VocalType.YOWL -> {
                if (profile.ageGroup == AgeGroup.SENIOR) out += Note.SENIOR_YOWLING
                if (profile.neutered == false) out += Note.INTACT_YOWLING
            }
            VocalType.CHATTER -> out += Note.CHATTER_PLAY
            else -> {}
        }
        if (profile.ageGroup == AgeGroup.KITTEN && type == VocalType.MEOW) out += Note.KITTEN_MEOWING
        if (intent == Intent.DISTRESS && emotion.arousal > 0.6) out += Note.DISTRESS_VET
        out += Note.BODY_LANGUAGE
        return out
    }

    private fun qualityWarnings(a: Analysis): List<QualityWarning> {
        val out = mutableListOf<QualityWarning>()
        if (a.vocalizations.isEmpty()) out += QualityWarning.NO_SOUND
        if (a.clippedFraction > 0.002) out += QualityWarning.CLIPPED
        if (a.vocalizations.isNotEmpty() && a.signalToNoiseDb < 18) out += QualityWarning.NOISY
        if (a.peakDb < -45) out += QualityWarning.TOO_QUIET
        return out
    }

    private fun qualityFactor(a: Analysis, f: SegmentFeatures): Double {
        var q = rise(a.signalToNoiseDb, 15.0, 4.0)
        if (a.clippedFraction > 0.002) q *= 0.75
        if (f.durationSec < 0.12) q *= 0.7
        return 0.35 + 0.65 * q
    }

    private fun unclear(analysis: Analysis, quality: List<QualityWarning>) = Translation(
        headline = Headline(PhraseKey.UNCLEAR, Intensity.MEDIUM, 0),
        intent = Intent.UNCLEAR,
        intents = listOf(Ranked(Intent.UNCLEAR, 1.0)),
        vocalType = VocalType.UNKNOWN,
        vocalTypes = listOf(Ranked(VocalType.UNKNOWN, 1.0)),
        contexts = null,
        emotion = Emotion(0.0, 0.0),
        intensity = Intensity.LOW,
        confidence = 0.0,
        evidence = emptyList(),
        notes = emptyList(),
        qualityWarnings = quality,
        callCount = 0,
        primary = null,
        analysis = analysis,
        personalExamplesUsed = 0,
    )

    /** Soft (0..1) versions of the prosodic cues used throughout. */
    private class Cues(f: SegmentFeatures) {
        val voiced = f.contourShape != ContourShape.UNVOICED
        val rising = if (voiced) rise(f.f0EndMinusStartSt, 0.0, 1.5) else 0.5
        val falling = 1 - rising
        val short = fall(f.durationSec, 0.45, 0.08)
        val long = rise(f.durationSec, 1.0, 0.15)
        val harsh = rise(f.aperiodicity, 0.3, 0.06)
        val low = if (f.f0MedianHz > 0) fall(f.f0MedianHz, 420.0, 60.0) else 0.5
        val loud = rise(f.dbAboveFloor, 35.0, 6.0)
    }

    private companion object {
        fun rise(x: Double, c: Double, w: Double) = 1.0 / (1.0 + exp(-(x - c) / w))
        fun fall(x: Double, c: Double, w: Double) = 1 - rise(x, c, w)
        fun <K> normalize(m: Map<K, Double>): Map<K, Double> {
            val s = m.values.sum()
            return if (s <= 0) m.mapValues { 1.0 / m.size } else m.mapValues { it.value / s }
        }
    }
}
