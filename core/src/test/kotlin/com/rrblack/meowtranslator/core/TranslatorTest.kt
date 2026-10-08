package com.rrblack.meowtranslator.core

import com.rrblack.meowtranslator.core.analysis.AcousticAnalyzer
import com.rrblack.meowtranslator.core.classify.LabeledExample
import com.rrblack.meowtranslator.core.classify.VocalType
import com.rrblack.meowtranslator.core.translate.AgeGroup
import com.rrblack.meowtranslator.core.translate.CatProfile
import com.rrblack.meowtranslator.core.translate.Intent
import com.rrblack.meowtranslator.core.translate.Evidence
import com.rrblack.meowtranslator.core.translate.Note
import com.rrblack.meowtranslator.core.i18n.EnglishTexts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class TranslatorTest {
    private val engine = MeowTranslatorEngine()

    @Test
    fun hissMeansBackOff() {
        val t = engine.translate(Synth.padded(Synth.noise(0.8)), Synth.SR)
        assertEquals(VocalType.HISS, t.vocalType)
        assertEquals(Intent.DEFENSIVE, t.intent)
        assertTrue(t.emotion.valence < 0)
        assertTrue(EnglishTexts.headline(t.headline).isNotBlank())
    }

    @Test
    fun purrMeansContent() {
        val t = engine.translate(Synth.padded(Synth.purr(2.5)), Synth.SR)
        assertEquals(Intent.CONTENT, t.intent)
        assertTrue(t.emotion.valence > 0)
        assertTrue(t.emotion.arousal < 0.5)
    }

    @Test
    fun silenceIsUnclear() {
        val t = engine.translate(Synth.silence(2.0), Synth.SR)
        assertEquals(Intent.UNCLEAR, t.intent)
        assertEquals(0.0, t.confidence, 0.0)
    }

    @Test
    fun meowGetsAMeowIntentAndEvidence() {
        val t = engine.translate(Synth.padded(Synth.tone(0.8) { 520 + 250 * sin(Math.PI * it) }), Synth.SR, CatProfile(name = "Milo"))
        assertEquals(VocalType.MEOW, t.vocalType)
        assertTrue(t.intent in Intent.MEOW_INTENTS)
        assertTrue(t.contexts!!.values.sum() in 0.99..1.01)
        assertTrue(t.evidence.any { it is Evidence.DataModel })
        assertEquals(1.0, t.intents.sumOf { it.probability }, 0.01)
    }

    @Test
    fun yowlNotesDependOnProfile() {
        val yowl = Synth.padded(Synth.tone(2.8) { 450 + 350 * sin(Math.PI * 2 * it).let { it * it } })
        val senior = engine.translate(yowl, Synth.SR, CatProfile(ageGroup = AgeGroup.SENIOR, neutered = true))
        assertEquals(Intent.CALLING_OUT, senior.intent)
        assertTrue(Note.SENIOR_YOWLING in senior.notes)
    }

    @Test
    fun teachingTheAppChangesItsReading() {
        val analyzer = AcousticAnalyzer()
        // The owner says this cat's short high meows are always about food.
        val examples = (0 until 15).map { i ->
            val meow = Synth.padded(Synth.tone(0.5 + 0.01 * i) { 900 - 100 * it + i })
            LabeledExample(analyzer.analyze(meow, Synth.SR).primary!!.features.toModelVector(), Intent.FOOD)
        } + (0 until 15).map { i ->
            val meow = Synth.padded(Synth.tone(1.1 + 0.01 * i) { 380 - 60 * it + i })
            LabeledExample(analyzer.analyze(meow, Synth.SR).primary!!.features.toModelVector(), Intent.DISTRESS)
        }
        val probe = Synth.padded(Synth.tone(0.52) { 905 - 100 * it })
        val taught = engine.translate(probe, Synth.SR, taughtExamples = examples)
        assertEquals(Intent.FOOD, taught.intent)
        assertEquals(30, taught.personalExamplesUsed)
        assertTrue(taught.evidence.any { it is Evidence.PersonalMatch })
    }
}
