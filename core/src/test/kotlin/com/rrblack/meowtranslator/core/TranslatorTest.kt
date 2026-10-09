package com.rrblack.meowtranslator.core

import com.rrblack.meowtranslator.core.analysis.AcousticAnalyzer
import com.rrblack.meowtranslator.core.classify.LabeledExample
import com.rrblack.meowtranslator.core.classify.VocalType
import com.rrblack.meowtranslator.core.translate.AgeGroup
import com.rrblack.meowtranslator.core.translate.CatProfile
import com.rrblack.meowtranslator.core.translate.Intent
import com.rrblack.meowtranslator.core.translate.PhraseKey
import com.rrblack.meowtranslator.core.translate.Translator
import com.rrblack.meowtranslator.core.classify.SoundSource
import com.rrblack.meowtranslator.core.translate.Evidence
import com.rrblack.meowtranslator.core.translate.Note
import com.rrblack.meowtranslator.core.i18n.EnglishTexts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class TranslatorTest {
    private val engine = MeowTranslatorEngine()

    /**
     * Synthetic hisses and purrs are crude stand-ins that the data-trained cat detector rightly doubts, so
     * call-type → meaning logic is tested with a detector that accepts everything.
     */
    private val acceptAll = Translator(callDetector = { mapOf(SoundSource.CAT to 1.0, SoundSource.HUMAN to 0.0, SoundSource.OTHER to 0.0) })
    private fun translateAsCat(samples: FloatArray) = acceptAll.translate(AcousticAnalyzer().analyze(samples, Synth.SR))

    @Test
    fun hissMeansBackOff() {
        val t = translateAsCat(Synth.padded(Synth.noise(0.8)))
        assertEquals(VocalType.HISS, t.vocalType)
        assertEquals(Intent.DEFENSIVE, t.intent)
        assertTrue(t.emotion.valence < 0)
        assertTrue(EnglishTexts.headline(t.headline).isNotBlank())
    }

    @Test
    fun purrMeansContent() {
        val t = translateAsCat(Synth.padded(Synth.purr(2.5)))
        assertEquals(Intent.CONTENT, t.intent)
        assertTrue(t.emotion.valence > 0)
        assertTrue(t.emotion.arousal < 0.5)
    }

    @Test
    fun soundsTheDetectorRejectsAreNotTranslated() {
        val rejectAll = Translator(callDetector = { mapOf(SoundSource.CAT to 0.0, SoundSource.HUMAN to 0.9, SoundSource.OTHER to 0.1) })
        val t = rejectAll.translate(AcousticAnalyzer().analyze(Synth.padded(Synth.tone(0.8) { 600.0 }), Synth.SR))
        assertEquals(Intent.UNCLEAR, t.intent)
        assertEquals(PhraseKey.HUMAN_VOICE, t.headline.key)
        assertEquals(null, t.primary) // Nothing to save to history or learn from.
    }

    @Test
    fun onlyTheCatPartOfAMixedRecordingIsTranslated() {
        // First sound "is a person", second is the cat: the translation must describe the second one.
        val person = Synth.tone(0.6) { 300.0 }
        val cat = Synth.tone(0.8) { 520 + 250 * sin(Math.PI * it) }
        val audio = Synth.concat(Synth.silence(0.3), person, Synth.silence(0.5), cat, Synth.silence(0.3))
        val detector = Translator(callDetector = { f ->
            val isCat = f.f0MedianHz > 450
            mapOf(SoundSource.CAT to if (isCat) 0.9 else 0.05, SoundSource.HUMAN to if (isCat) 0.05 else 0.9, SoundSource.OTHER to 0.05)
        })
        val t = detector.translate(AcousticAnalyzer().analyze(audio, Synth.SR))
        assertTrue(t.primary!!.startSec > 1.0)
        assertEquals(1, t.callCount)
        assertTrue(t.evidence.any { it is Evidence.IgnoredSounds && it.human == 1 })
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
