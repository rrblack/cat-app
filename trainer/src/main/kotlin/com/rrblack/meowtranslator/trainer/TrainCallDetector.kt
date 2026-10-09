package com.rrblack.meowtranslator.trainer

import com.rrblack.meowtranslator.core.analysis.AcousticAnalyzer
import com.rrblack.meowtranslator.core.analysis.Analysis
import com.rrblack.meowtranslator.core.classify.SoundSource
import com.rrblack.meowtranslator.core.translate.Intent
import com.rrblack.meowtranslator.core.translate.Translator
import com.rrblack.meowtranslator.core.analysis.SegmentFeatures
import com.rrblack.meowtranslator.core.analysis.Vocalization
import com.rrblack.meowtranslator.core.audio.WavIO
import com.rrblack.meowtranslator.core.classify.SoftmaxRegression
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.random.Random

/**
 * Trains the cat / human / other sound detector and writes CallDetectorWeights.kt.
 *
 *   TrainCallDetector <CatMeows dataset dir> <CatMeows extra "other_vocalizations" dir> <8 kHz speech WAV dir>
 *                     <8 kHz ESC-50 WAV dir> <esc50.csv> <output .kt>
 *
 * Every clip is used twice: as recorded, and replayed through a random simulated home (echo, noise, quieter
 * level). Evaluation is 5-fold with whole groups held out — unseen cats, unseen speakers, unseen ESC-50 folds —
 * so the numbers describe sounds the model has never heard.
 */

private val HUMAN_ESC = setOf("laughing", "crying_baby", "coughing", "sneezing", "breathing", "snoring")
private val CLASSES = listOf("CAT", "HUMAN", "OTHER")

private class Item(val source: String, val group: String, val fold: Int, val label: Int, val x: DoubleArray)

/** A whole clean clip, for recording-level evaluation through the full translator. */
private class Recording(val source: String, val fold: Int, val analysis: Analysis)

private class Clip(val file: File, val source: String, val group: String, val fold: Int, val label: Int)

fun main(args: Array<String>) {
    Locale.setDefault(Locale.US)
    val (catDir, extraDir, speechDir, escDir, escCsv) = args.take(5).map { File(it) }
    val out = File(args[5])

    val clips = mutableListOf<Clip>()
    fun foldOf(key: String) = Math.floorMod(key.hashCode(), 5)
    catDir.wavs().forEach { val cat = it.name.split("_")[1]; clips += Clip(it, "catmeows", cat, foldOf(cat), 0) }
    extraDir.wavs().forEach { val cat = it.name.split("_")[1]; clips += Clip(it, "catmeows-other", cat, foldOf(cat), 0) }
    speechDir.wavs().forEach { val spk = it.name.split("_")[1]; clips += Clip(it, "speech", spk, foldOf(spk), 1) }
    escCsv.readLines().drop(1).forEach { line ->
        val c = line.split(",")
        val category = c[3]
        val label = when (category) {
            "cat" -> 0
            in HUMAN_ESC -> 1
            else -> 2
        }
        val source = if (label == 0) "esc-cat" else if (label == 1) "esc-human" else "esc-other"
        clips += Clip(File(escDir, c[0]), source, "esc-${c[5]}", c[1].toInt() - 1, label)
    }
    println("Clips: " + clips.groupingBy { it.source }.eachCount())

    // Feature extraction (parallel; each clip clean + one simulated-home version).
    val pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    val conditions = Augment.CONDITIONS.drop(1)
    val recordings = mutableListOf<Recording>()
    val items = clips.mapIndexed { i, clip ->
        pool.submit<Pair<Recording, List<Item>>> {
            val audio = clip.file.inputStream().use { WavIO.read(it) }
            val analyzer = AcousticAnalyzer()
            val rnd = Random(i)
            val analyses = listOf(audio.samples, Augment.apply(audio.samples, audio.sampleRate, conditions[rnd.nextInt(conditions.size)], rnd))
                .map { analyzer.analyze(it, audio.sampleRate) }
            Recording(clip.source, clip.fold, analyses[0]) to analyses.flatMap { a ->
                selectSegments(a.vocalizations, clip.label).map { Item(clip.source, clip.group, clip.fold, clip.label, it.features.toDetectorVector()) }
            }
        }
    }.map { it.get() }.flatMap { (rec, segs) -> recordings += rec; segs }
    pool.shutdown()
    println("Segments: " + items.groupingBy { "${it.source}:${CLASSES[it.label]}" }.eachCount())
    System.getenv("DETECTOR_DUMP")?.let { path ->
        File(path).printWriter().use { w ->
            w.println("source,group,fold,label," + SegmentFeatures.DETECTOR_FEATURE_NAMES.joinToString(","))
            items.forEach { w.println("${it.source},${it.group},${it.fold},${it.label}," + it.x.joinToString(",")) }
        }
    }

    // Grouped 5-fold evaluation.
    var best: Triple<Double, Double, Map<String, Double>>? = null
    for (l2 in listOf(0.001, 0.003, 0.01)) {
        val catScores = mutableListOf<Pair<Item, Double>>()
        for (fold in 0 until 5) {
            val train = items.filter { it.fold != fold }
            val m = SoftmaxRegression.train(train.map { it.x }, train.map { it.label }.toIntArray(), CLASSES, l2, epochs = 400, learningRate = 0.2)
            items.filter { it.fold == fold }.forEach { catScores += it to m.predict(it.x)[0] }
        }
        // Pick the threshold that keeps 95% of CatMeows calls, then report everything at that threshold.
        val catMeowScores = catScores.filter { it.first.source == "catmeows" }.map { it.second }.sorted()
        val threshold = catMeowScores[(catMeowScores.size * 0.05).toInt()].coerceIn(0.2, 0.6)
        val accept = catScores.groupBy { it.first.source }.mapValues { (_, v) -> v.count { it.second >= threshold }.toDouble() / v.size }
        println("l2=$l2 threshold=%.3f accepted-as-cat: ".format(threshold) + accept.entries.joinToString { "${it.key}=%.3f".format(it.value) })
        val score = accept.getValue("esc-cat") + (1 - accept.getValue("speech")) + (1 - accept.getValue("esc-other")) + (1 - accept.getValue("esc-human"))
        if (best == null || score > best.first) best = Triple(score, l2, accept + mapOf("threshold" to threshold))
    }
    val (_, l2, segAccept) = best!!
    println("Selected l2=$l2")

    // Recording-level: run the full translator with each fold's detector on that fold's held-out clips.
    val threshold = segAccept.getValue("threshold")
    val translated = mutableMapOf<String, MutableList<Boolean>>()
    for (fold in 0 until 5) {
        val train = items.filter { it.fold != fold }
        val m = SoftmaxRegression.train(train.map { it.x }, train.map { it.label }.toIntArray(), CLASSES, l2, epochs = 400, learningRate = 0.2)
        val translator = Translator(
            callDetector = { f -> m.predict(f.toDetectorVector()).let { p -> CLASSES.withIndex().associate { (i, c) -> SoundSource.valueOf(c) to p[i] } } },
            catThreshold = threshold,
        )
        recordings.filter { it.fold == fold }.forEach { r ->
            translated.getOrPut(r.source) { mutableListOf() } += translator.translate(r.analysis).intent != Intent.UNCLEAR
        }
    }
    val accept = translated.mapValues { (_, v) -> v.count { it }.toDouble() / v.size } + mapOf("threshold" to threshold)
    println("Held-out recordings translated (rather than rejected): " + accept.entries.joinToString { "${it.key}=%.3f".format(it.value) })

    val model = SoftmaxRegression.train(items.map { it.x }, items.map { it.label }.toIntArray(), CLASSES, l2, epochs = 400, learningRate = 0.2)
    out.writeText(render(model, accept, l2))
    println("Wrote ${out.path}")
}

private fun File.wavs() = listFiles { f -> f.extension.equals("wav", true) }!!.sorted()

/**
 * Which segments of a clip carry its label. Cat clips (dataset meows, ESC-50 cats) also contain bumps and
 * room noise, so only their prominent calls are used; for non-cat clips every audible segment is a negative.
 */
private fun selectSegments(v: List<Vocalization>, label: Int): List<Vocalization> {
    val audible = v.filter { it.features.dbAboveFloor >= 12 && it.features.durationSec >= 0.08 }
    if (label != 0 || audible.isEmpty()) return audible
    val loudest = audible.maxOf { it.features.peakDbFs }
    return audible.filter { it.features.peakDbFs >= loudest - 8 && it.features.durationSec >= 0.15 }
}

private fun render(m: SoftmaxRegression, accept: Map<String, Double>, l2: Double): String {
    fun arr(a: DoubleArray) = a.joinToString(", ") { "%.6f".format(it) }
    return buildString {
        appendLine("// GENERATED by :trainer (TrainCallDetector.kt) — do not edit by hand.")
        appendLine("// Data: CatMeows (doi:10.5281/zenodo.4008297, CC BY-NC 4.0), ESC-50 (Piczak 2015, CC BY-NC 3.0),")
        appendLine("// LibriSpeech dev-clean (Panayotov et al. 2015, CC BY 4.0).")
        appendLine("package com.rrblack.meowtranslator.core.classify")
        appendLine()
        appendLine("internal object CallDetectorWeights {")
        appendLine("    const val L2 = $l2")
        appendLine("    const val CAT_THRESHOLD = %.4f".format(accept.getValue("threshold")))
        appendLine("    /** Held-out recordings (unseen cats / speakers / ESC-50 folds) that the full translator accepts as cat. */")
        appendLine("    const val EVAL_CAT_RECALL = %.4f".format(accept.getValue("catmeows")))
        appendLine("    const val EVAL_ESC_CAT_RECALL = %.4f".format(accept.getValue("esc-cat")))
        appendLine("    const val EVAL_SPEECH_REJECTION = %.4f".format(1 - accept.getValue("speech")))
        appendLine("    const val EVAL_HUMAN_SOUND_REJECTION = %.4f".format(1 - accept.getValue("esc-human")))
        appendLine("    const val EVAL_OTHER_REJECTION = %.4f".format(1 - accept.getValue("esc-other")))
        appendLine("    val CLASSES = listOf(${m.classes.joinToString { "\"$it\"" }})")
        appendLine("    val FEATURES = listOf(${SegmentFeatures.DETECTOR_FEATURE_NAMES.joinToString { "\"$it\"" }})")
        appendLine("    val MEAN = doubleArrayOf(${arr(m.mean)})")
        appendLine("    val SCALE = doubleArrayOf(${arr(m.scale)})")
        appendLine("    val WEIGHTS = arrayOf(")
        m.weights.forEach { appendLine("        doubleArrayOf(${arr(it)}),") }
        appendLine("    )")
        appendLine("    val BIAS = doubleArrayOf(${arr(m.bias)})")
        appendLine("}")
    }
}
