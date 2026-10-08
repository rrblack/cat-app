package com.rrblack.meowtranslator.trainer

import com.rrblack.meowtranslator.core.analysis.AcousticAnalyzer
import com.rrblack.meowtranslator.core.analysis.SegmentFeatures
import com.rrblack.meowtranslator.core.audio.WavIO
import java.io.File

/** Debug helper: writes per-file features of a folder of WAVs to CSV. Usage: DumpFeatures <dir> <out.csv> */
fun main(args: Array<String>) {
    val dir = File(args[0])
    val analyzer = AcousticAnalyzer()
    File(args[1]).printWriter().use { out ->
        out.println((listOf("file", "n_voc", "shape", "f0_median_hz", "db_above_floor", "am_rate", "duration") + SegmentFeatures.FEATURE_NAMES).joinToString(","))
        for (f in dir.listFiles { x -> x.extension.equals("wav", true) }!!.sorted()) {
            val audio = f.inputStream().use { WavIO.read(it) }
            val a = analyzer.analyze(audio.samples, audio.sampleRate)
            val p = a.primary
            if (p == null) {
                out.println("${f.name},0")
                continue
            }
            val ft = p.features
            out.println((listOf(f.name, a.vocalizations.size, ft.contourShape.name, ft.f0MedianHz, ft.dbAboveFloor, ft.amRateHz, ft.durationSec) + ft.toModelVector().toList()).joinToString(","))
        }
    }
}
