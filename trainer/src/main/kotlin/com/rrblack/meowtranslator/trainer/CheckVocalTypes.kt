package com.rrblack.meowtranslator.trainer

import com.rrblack.meowtranslator.core.analysis.AcousticAnalyzer
import com.rrblack.meowtranslator.core.audio.WavIO
import com.rrblack.meowtranslator.core.classify.VocalTypeClassifier
import java.io.File

/**
 * Sanity check for the rule-based call-type classifier: every file in CatMeows/dataset is a meow,
 * so the share predicted as MEOW is a direct accuracy measure for that class.
 * Usage: CheckVocalTypes <dir> [--verbose]
 */
fun main(args: Array<String>) {
    val analyzer = AcousticAnalyzer()
    val counts = sortedMapOf<String, Int>()
    for (f in File(args[0]).listFiles { x -> x.extension.equals("wav", true) }!!.sorted()) {
        val audio = f.inputStream().use { WavIO.read(it) }
        val p = analyzer.analyze(audio.samples, audio.sampleRate).primary
        val label = if (p == null) "NONE" else VocalTypeClassifier.classify(p.features).maxBy { it.value }.key.name
        counts[label] = (counts[label] ?: 0) + 1
        if (args.contains("--verbose") || (args.contains("--errors") && label != "MEOW")) {
            val ft = p?.features
            println("${f.name.padEnd(36)} $label  dur=%.2f vf=%.2f f0=%.0f ap=%.2f flat=%.2f am=%.2f@%.0f cent=%.0f low=%.2f".format(
                ft?.durationSec, ft?.voicedFraction, ft?.f0MedianHz, ft?.aperiodicity, ft?.spectralFlatness, ft?.amStrength, ft?.amRateHz, ft?.spectralCentroidHz, ft?.lowBandRatio))
        }
    }
    println(counts)
}
