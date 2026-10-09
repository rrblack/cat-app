package com.rrblack.meowtranslator.trainer

import com.rrblack.meowtranslator.core.MeowTranslatorEngine
import com.rrblack.meowtranslator.core.audio.WavIO
import java.io.File
import kotlin.random.Random

/**
 * Runs the full translator on dataset meows under simulated real-home recording conditions and reports
 * what the app would call them. Usage: EvaluateRobustness <dir of cat meows> [noise wav dir]
 */
fun main(args: Array<String>) {
    val files = File(args[0]).listFiles { f -> f.extension == "wav" }!!.sorted()
    val noiseBank = args.getOrNull(1)?.let { dir -> File(dir).listFiles { f -> f.extension == "wav" }!!.map { f -> f.inputStream().use { WavIO.read(it) }.samples } } ?: emptyList()
    val engine = MeowTranslatorEngine()
    for (c in Augment.CONDITIONS) {
        val rnd = Random(7)
        val types = sortedMapOf<String, Int>()
        val intents = sortedMapOf<String, Int>()
        for (f in files) {
            val a = f.inputStream().use { WavIO.read(it) }
            val t = engine.translate(Augment.apply(a.samples, a.sampleRate, c, rnd, noiseBank), a.sampleRate)
            types.merge(t.vocalType.name, 1, Int::plus)
            intents.merge(t.intent.name, 1, Int::plus)
        }
        println("${c.name.padEnd(30)} types=$types")
        println("${"".padEnd(30)} intents=$intents")
    }
}
