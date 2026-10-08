package com.rrblack.meowtranslator.core

import com.rrblack.meowtranslator.core.analysis.AcousticAnalyzer
import com.rrblack.meowtranslator.core.classify.LabeledExample
import com.rrblack.meowtranslator.core.classify.PersonalModel
import com.rrblack.meowtranslator.core.translate.CatProfile
import com.rrblack.meowtranslator.core.translate.Translation
import com.rrblack.meowtranslator.core.translate.Translator

/** Entry point: audio in, translation out. Thread-safe; create once and reuse. */
class MeowTranslatorEngine {
    private val translator = Translator()

    fun translate(
        samples: FloatArray,
        sampleRate: Int,
        profile: CatProfile = CatProfile(),
        taughtExamples: List<LabeledExample> = emptyList(),
    ): Translation {
        val analysis = AcousticAnalyzer().analyze(samples, sampleRate)
        val personal = if (taughtExamples.isEmpty()) null else PersonalModel(taughtExamples)
        return translator.translate(analysis, profile, personal)
    }
}
