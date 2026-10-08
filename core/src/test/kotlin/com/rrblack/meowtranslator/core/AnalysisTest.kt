package com.rrblack.meowtranslator.core

import com.rrblack.meowtranslator.core.analysis.AcousticAnalyzer
import com.rrblack.meowtranslator.core.analysis.ContourShape
import com.rrblack.meowtranslator.core.classify.VocalType
import com.rrblack.meowtranslator.core.classify.VocalTypeClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class AnalysisTest {
    private val analyzer = AcousticAnalyzer()

    private fun typeOf(sound: FloatArray): VocalType {
        val a = analyzer.analyze(Synth.padded(sound), Synth.SR)
        return VocalTypeClassifier.classify(a.primary!!.features).maxBy { it.value }.key
    }

    @Test
    fun segmentsSeparateCalls() {
        val meow = Synth.tone(0.6) { 600.0 }
        val a = analyzer.analyze(Synth.concat(Synth.silence(0.3), meow, Synth.silence(0.5), meow, Synth.silence(0.3)), Synth.SR)
        assertEquals(2, a.vocalizations.size)
        assertEquals(0.3, a.vocalizations[0].startSec, 0.12)
    }

    @Test
    fun silenceHasNoVocalizations() {
        assertTrue(analyzer.analyze(Synth.silence(2.0), Synth.SR).vocalizations.isEmpty())
    }

    @Test
    fun contourShapes() {
        fun shape(f0: (Double) -> Double) = analyzer.analyze(Synth.padded(Synth.tone(0.8, f0 = f0)), Synth.SR).primary!!.features.contourShape
        assertEquals(ContourShape.RISING, shape { 450 + 300 * it })
        assertEquals(ContourShape.FALLING, shape { 800 - 350 * it })
        assertEquals(ContourShape.RISE_FALL, shape { 500 + 300 * sin(Math.PI * it) })
        assertEquals(ContourShape.FLAT, shape { 600.0 })
    }

    @Test
    fun meowIsRecognised() = assertEquals(VocalType.MEOW, typeOf(Synth.tone(0.7) { 520 + 250 * sin(Math.PI * it) }))

    @Test
    fun hissIsRecognised() = assertEquals(VocalType.HISS, typeOf(Synth.noise(0.8)))

    @Test
    fun purrIsRecognised() = assertEquals(VocalType.PURR, typeOf(Synth.purr(2.5)))

    @Test
    fun yowlIsRecognised() = assertEquals(VocalType.YOWL, typeOf(Synth.tone(2.8) { 450 + 350 * sin(Math.PI * 2 * it).let { it * it } }))

    @Test
    fun growlIsRecognised() {
        // Low, rough voice: 130 Hz with jittered pitch and noise.
        val rnd = kotlin.random.Random(5)
        val growl = Synth.tone(1.2, harmonics = 12, noise = 0.06) { 130 + 25 * rnd.nextDouble() }
        assertEquals(VocalType.GROWL, typeOf(growl))
    }

    @Test
    fun trillIsRecognised() {
        // Short rising call with ~25 Hz "rolled" amplitude flutter.
        val base = Synth.tone(0.35) { 450 + 200 * it }
        val trill = FloatArray(base.size) { (base[it] * (0.55 + 0.45 * sin(2 * Math.PI * 25 * it / Synth.SR))).toFloat() }
        assertEquals(VocalType.TRILL, typeOf(trill))
    }

    @Test
    fun chatterIsDetectedAcrossTheBout() {
        val chirp = Synth.tone(0.07) { 900.0 }
        val parts = mutableListOf(Synth.silence(0.3))
        repeat(6) {
            parts += chirp
            parts += Synth.silence(0.12, seed = it + 10)
        }
        val a = analyzer.analyze(Synth.concat(*parts.toTypedArray()), Synth.SR)
        assertTrue(VocalTypeClassifier.chatterLikelihood(a.vocalizations) > 0.5)
    }
}
