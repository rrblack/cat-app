package com.rrblack.meowtranslator.core.i18n

import com.rrblack.meowtranslator.core.analysis.ContourShape
import com.rrblack.meowtranslator.core.classify.MeowContext
import com.rrblack.meowtranslator.core.classify.VocalType
import com.rrblack.meowtranslator.core.translate.AgeGroup
import com.rrblack.meowtranslator.core.translate.ConfidenceLevel
import com.rrblack.meowtranslator.core.translate.EmotionLabel
import com.rrblack.meowtranslator.core.translate.Evidence
import com.rrblack.meowtranslator.core.translate.Headline
import com.rrblack.meowtranslator.core.translate.Intensity
import com.rrblack.meowtranslator.core.translate.Intent
import com.rrblack.meowtranslator.core.translate.Note
import com.rrblack.meowtranslator.core.translate.PhraseKey
import com.rrblack.meowtranslator.core.translate.QualityWarning
import java.util.Locale
import kotlin.math.roundToInt

enum class Language(val tag: String, val nativeName: String) {
    ENGLISH("en", "English"),
    JAPANESE("ja", "日本語"),
    ;

    companion object {
        /** Japanese for any `ja*` locale, English otherwise. */
        fun forLanguageTag(tag: String): Language = if (tag.lowercase(Locale.ROOT).startsWith("ja")) JAPANESE else ENGLISH
    }
}

/** Everything the engine says, in one language. */
interface Texts {
    val language: Language

    /** Phrase options for a headline; must be non-empty for every key and intensity. */
    fun phrases(key: PhraseKey, intensity: Intensity): List<String>
    val insistentSuffix: String
    val defaultSpeaker: String

    fun intentTitle(intent: Intent): String
    fun intentSummary(intent: Intent): String
    fun vocalTypeName(type: VocalType): String
    fun vocalTypeDescription(type: VocalType): String
    fun emotion(label: EmotionLabel): String
    fun contour(shape: ContourShape): String
    fun confidence(level: ConfidenceLevel): String
    fun context(context: MeowContext): String
    fun ageGroup(age: AgeGroup): String
    fun evidence(e: Evidence): String
    fun note(note: Note): String
    fun warning(warning: QualityWarning): String

    fun headline(h: Headline): String {
        val options = phrases(h.key, h.intensity)
        return options[Math.floorMod(h.seed, options.size)] + if (h.insistent) insistentSuffix else ""
    }

    fun speaker(catName: String): String = catName.ifBlank { defaultSpeaker }

    companion object {
        fun of(language: Language): Texts = when (language) {
            Language.ENGLISH -> EnglishTexts
            Language.JAPANESE -> JapaneseTexts
        }

        fun pct(p: Double) = "${(p * 100).roundToInt()}%"
        fun oneDecimal(x: Double) = String.format(Locale.US, "%.1f", x)
    }
}
