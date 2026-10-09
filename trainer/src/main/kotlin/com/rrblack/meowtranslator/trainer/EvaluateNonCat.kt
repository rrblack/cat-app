package com.rrblack.meowtranslator.trainer

import com.rrblack.meowtranslator.core.MeowTranslatorEngine
import com.rrblack.meowtranslator.core.audio.WavIO
import com.rrblack.meowtranslator.core.translate.Intent
import java.io.File

/**
 * What does the app say about sounds that are NOT cats? Runs the full translator on each WAV and tallies
 * call types, grouped by ESC-50 category (via its meta CSV) or by folder.
 * Usage: EvaluateNonCat <wav dir> [esc50.csv]
 */
fun main(args: Array<String>) {
    val category = args.getOrNull(1)?.let { csv ->
        File(csv).readLines().drop(1).associate { line -> line.split(",").let { it[0] to it[3] } }
    } ?: emptyMap()
    val engine = MeowTranslatorEngine()
    val byGroup = sortedMapOf<String, MutableMap<String, Int>>()
    for (f in File(args[0]).listFiles { x -> x.extension == "wav" }!!.sorted()) {
        val a = f.inputStream().use { WavIO.read(it) }
        val t = engine.translate(a.samples, a.sampleRate)
        val group = category[f.name] ?: "all"
        val outcome = if (t.intent == Intent.UNCLEAR) "REJECTED_${t.headline.key}" else t.vocalType.name
        byGroup.getOrPut(group) { sortedMapOf() }.merge(outcome, 1, Int::plus)
    }
    for ((g, m) in byGroup) println("${g.padEnd(18)} $m")
}
