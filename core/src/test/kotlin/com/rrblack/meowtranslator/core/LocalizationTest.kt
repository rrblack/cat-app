package com.rrblack.meowtranslator.core

import com.rrblack.meowtranslator.core.analysis.ContourShape
import com.rrblack.meowtranslator.core.classify.MeowContext
import com.rrblack.meowtranslator.core.classify.VocalType
import com.rrblack.meowtranslator.core.i18n.Language
import com.rrblack.meowtranslator.core.i18n.Texts
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizationTest {
    private val japanese = Regex("[\\u3040-\\u30FF\\u4E00-\\u9FFF]")

    private val sampleEvidence: List<Evidence> = listOf(
        Evidence.CallType(VocalType.MEOW, 0.9, 0.8, 520.0),
        Evidence.CallType(VocalType.HISS, 0.7, 0.6, 0.0),
        Evidence.DataModel(MeowContext.FOOD, 0.5, 440, 21),
        Evidence.Melody(ContourShape.RISING, 2.5),
        Evidence.Melody(ContourShape.FALLING, -3.0),
        Evidence.Melody(ContourShape.FLAT, 0.2),
        Evidence.ShortCall, Evidence.LongCall, Evidence.RoughVoice,
        Evidence.PurrPulsing(25.0), Evidence.TrillFlutter(22.0),
        Evidence.HissNoise, Evidence.GrowlLowRough, Evidence.YowlLong, Evidence.ChatterBurst,
        Evidence.Repeated(3), Evidence.PersonalMatch(Intent.FOOD, 12), Evidence.PersonalWeight(4, 0.2),
    )

    /** Every string the engine can produce, for one language. */
    private fun everything(t: Texts): List<String> = buildList {
        for (k in PhraseKey.entries) for (i in Intensity.entries) {
            val options = t.phrases(k, i)
            assertTrue("$k/$i has no phrases in ${t.language}", options.isNotEmpty())
            addAll(options)
        }
        Intent.entries.forEach { add(t.intentTitle(it)); add(t.intentSummary(it)) }
        VocalType.entries.forEach { add(t.vocalTypeName(it)); add(t.vocalTypeDescription(it)) }
        EmotionLabel.entries.forEach { add(t.emotion(it)) }
        ContourShape.entries.forEach { add(t.contour(it)) }
        ConfidenceLevel.entries.forEach { add(t.confidence(it)) }
        MeowContext.entries.forEach { add(t.context(it)) }
        AgeGroup.entries.forEach { add(t.ageGroup(it)) }
        sampleEvidence.forEach { add(t.evidence(it)) }
        Note.entries.forEach { add(t.note(it)) }
        QualityWarning.entries.forEach { add(t.warning(it)) }
        add(t.defaultSpeaker)
    }

    @Test
    fun everyStringExistsInBothLanguages() {
        val en = everything(Texts.of(Language.ENGLISH))
        val ja = everything(Texts.of(Language.JAPANESE))
        assertEquals(en.size, ja.size)
        en.forEach { assertTrue("blank English string", it.isNotBlank()) }
        ja.forEach { assertTrue("not Japanese: $it", japanese.containsMatchIn(it)) }
    }

    @Test
    fun headlineIsTheSameMeaningInBothLanguages() {
        val h = Headline(PhraseKey.FOOD, Intensity.HIGH, seed = 7, insistent = true)
        val en = Texts.of(Language.ENGLISH).headline(h)
        val ja = Texts.of(Language.JAPANESE).headline(h)
        assertTrue(en.endsWith("(and I'll keep asking)"))
        assertTrue(ja.endsWith("（まだまだ言うよ）"))
        assertNotEquals(en, ja)
    }

    @Test
    fun speakerFallsBackPerLanguage() {
        assertEquals("Milo", Texts.of(Language.JAPANESE).speaker("Milo"))
        assertEquals("うちの子", Texts.of(Language.JAPANESE).speaker(""))
        assertEquals("Your cat", Texts.of(Language.ENGLISH).speaker(" "))
    }

    @Test
    fun languageFromLocale() {
        assertEquals(Language.JAPANESE, Language.forLanguageTag("ja-JP"))
        assertEquals(Language.ENGLISH, Language.forLanguageTag("en-US"))
        assertEquals(Language.ENGLISH, Language.forLanguageTag("fr"))
    }

    @Test
    fun realTranslationRendersInJapanese() {
        val t = MeowTranslatorEngine().translate(Synth.padded(Synth.noise(0.8)), Synth.SR)
        val ja = Texts.of(Language.JAPANESE)
        assertTrue(japanese.containsMatchIn(ja.headline(t.headline)))
        t.evidence.forEach { assertTrue(japanese.containsMatchIn(ja.evidence(it))) }
    }
}
