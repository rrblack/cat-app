package com.rrblack.meowtranslator.trainer

import com.rrblack.meowtranslator.core.analysis.AcousticAnalyzer
import com.rrblack.meowtranslator.core.analysis.SegmentFeatures
import com.rrblack.meowtranslator.core.audio.WavIO
import com.rrblack.meowtranslator.core.classify.LabeledExample
import com.rrblack.meowtranslator.core.classify.MeowContext
import com.rrblack.meowtranslator.core.classify.PersonalModel
import com.rrblack.meowtranslator.core.classify.SoftmaxRegression
import com.rrblack.meowtranslator.core.translate.Intent
import com.rrblack.meowtranslator.core.translate.Translator
import kotlin.random.Random
import java.io.File
import java.util.Locale

/**
 * Trains the meow-context model on the CatMeows dataset and writes it as Kotlin source.
 *
 *   ./gradlew :trainer:run --args="<path/to/CatMeows/dataset> core/src/main/kotlin/com/rrblack/meowtranslator/core/classify/ContextModelWeights.kt"
 *
 * Dataset: Ludovico, Ntalampiras, Presti et al., "CatMeows: A Publicly-Available Dataset of Cat Vocalizations"
 * (Zenodo, doi:10.5281/zenodo.4008297, CC BY-NC 4.0). File names encode the context as their first letter:
 * B = brushing, F = waiting for food, I = isolation in an unfamiliar environment.
 *
 * Accuracy is estimated with leave-one-cat-out cross-validation: every fold tests on a cat the model has
 * never heard, which is the situation the app is actually in.
 */

private val CONTEXTS = listOf("B" to "BRUSHING", "F" to "FOOD", "I" to "ISOLATION")

private class Sample(val file: String, val cat: String, val label: Int, val x: DoubleArray, val features: SegmentFeatures)

fun main(args: Array<String>) {
    require(args.size >= 2) { "usage: <dataset dir> <output .kt file>" }
    Locale.setDefault(Locale.US)
    val dir = File(args[0])
    val out = File(args[1])
    val analyzer = AcousticAnalyzer()

    val files = dir.listFiles { f -> f.extension.equals("wav", true) }!!.sorted()
    val samples = files.mapNotNull { f ->
        val parts = f.nameWithoutExtension.split("_")
        val label = CONTEXTS.indexOfFirst { it.first == parts[0] }
        if (label < 0) return@mapNotNull null
        val audio = f.inputStream().use { WavIO.read(it) }
        val voc = analyzer.analyze(audio.samples, audio.sampleRate).primary ?: return@mapNotNull null
        Sample(f.name, parts[1], label, voc.features.toModelVector(), voc.features)
    }
    val cats = samples.map { it.cat }.distinct()
    println("Loaded ${samples.size} calls from ${cats.size} cats (${files.size} files)")

    val classes = CONTEXTS.map { it.second }
    var best: Pair<Double, Evaluation>? = null
    for (l2 in listOf(0.003, 0.01, 0.03, 0.1, 0.3)) {
        val eval = leaveOneCatOut(samples, cats, classes, l2)
        println("l2=$l2  LOCO accuracy=%.3f  balanced=%.3f  log-loss=%.3f".format(eval.accuracy, eval.balancedAccuracy, eval.logLoss))
        if (best == null || eval.balancedAccuracy > best.second.balancedAccuracy) best = l2 to eval
    }
    val (l2, eval) = best!!
    println("Selected l2=$l2")
    println("Confusion (rows = true, cols = predicted): ${classes.joinToString()}")
    eval.confusion.forEachIndexed { i, row -> println("  ${classes[i].padEnd(10)} ${row.joinToString(" ") { "%4d".format(it) }}") }

    val withinCat = stratifiedKFold(samples, classes, l2, folds = 10)
    println("Within-cat estimate (random 10-fold, cats seen in training): accuracy=%.3f balanced=%.3f".format(withinCat.accuracy, withinCat.balancedAccuracy))

    val personalization = simulatePersonalization(samples, cats, classes, l2)

    val model = SoftmaxRegression.train(samples.map { it.x }, samples.map { it.label }.toIntArray(), classes, l2)
    out.parentFile.mkdirs()
    out.writeText(render(model, eval, withinCat, personalization, samples.size, cats.size, l2))
    println("Wrote ${out.path}")
}

private class Evaluation(val accuracy: Double, val balancedAccuracy: Double, val logLoss: Double, val confusion: Array<IntArray>)

private fun leaveOneCatOut(samples: List<Sample>, cats: List<String>, classes: List<String>, l2: Double): Evaluation {
    val k = classes.size
    val confusion = Array(k) { IntArray(k) }
    var logLoss = 0.0
    for (cat in cats) {
        val train = samples.filter { it.cat != cat }
        val test = samples.filter { it.cat == cat }
        val m = SoftmaxRegression.train(train.map { it.x }, train.map { it.label }.toIntArray(), classes, l2)
        for (s in test) {
            val p = m.predict(s.x)
            confusion[s.label][p.indices.maxBy { p[it] }]++
            logLoss -= kotlin.math.ln(p[s.label].coerceAtLeast(1e-9))
        }
    }
    val total = confusion.sumOf { it.sum() }
    val correct = (0 until k).sumOf { confusion[it][it] }
    val recall = (0 until k).map { confusion[it][it].toDouble() / confusion[it].sum().coerceAtLeast(1) }
    return Evaluation(correct.toDouble() / total, recall.average(), logLoss / total, confusion)
}

private fun stratifiedKFold(samples: List<Sample>, classes: List<String>, l2: Double, folds: Int): Evaluation {
    val k = classes.size
    val rnd = Random(42)
    val fold = IntArray(samples.size)
    for (c in classes.indices) {
        samples.indices.filter { samples[it].label == c }.shuffled(rnd).forEachIndexed { i, idx -> fold[idx] = i % folds }
    }
    val confusion = Array(k) { IntArray(k) }
    var logLoss = 0.0
    for (f in 0 until folds) {
        val train = samples.indices.filter { fold[it] != f }.map { samples[it] }
        val m = SoftmaxRegression.train(train.map { it.x }, train.map { it.label }.toIntArray(), classes, l2)
        for (i in samples.indices.filter { fold[it] == f }) {
            val p = m.predict(samples[i].x)
            confusion[samples[i].label][p.indices.maxBy { p[it] }]++
            logLoss -= kotlin.math.ln(p[samples[i].label].coerceAtLeast(1e-9))
        }
    }
    val total = confusion.sumOf { it.sum() }
    val recall = (0 until k).map { confusion[it][it].toDouble() / confusion[it].sum().coerceAtLeast(1) }
    return Evaluation((0 until k).sumOf { confusion[it][it] }.toDouble() / total, recall.average(), logLoss / total, confusion)
}

/**
 * Simulates an owner teaching the app: for each cat, the general model is trained WITHOUT that cat, then
 * N of the cat's own meows are given as labelled examples and the full translator (data model + prosody +
 * personal model) is scored on the cat's remaining meows. Returns balanced accuracy for each N.
 */
private fun simulatePersonalization(samples: List<Sample>, cats: List<String>, classes: List<String>, l2: Double): List<Pair<Int, Double>> {
    val intentOf = mapOf(0 to Intent.HANDLING, 1 to Intent.FOOD, 2 to Intent.DISTRESS)
    val results = mutableListOf<Pair<Int, Double>>()
    for (taught in listOf(0, 3, 6, 10, 15)) {
        val hits = Array(classes.size) { IntArray(2) } // [correct, total] per class
        for (cat in cats) {
            val own = samples.filter { it.cat == cat }
            if (own.size < taught + 5) continue
            val train = samples.filter { it.cat != cat }
            val m = SoftmaxRegression.train(train.map { it.x }, train.map { it.label }.toIntArray(), classes, l2)
            val translator = Translator { f ->
                val p = m.predict(f.toModelVector())
                classes.withIndex().associate { (i, c) -> MeowContext.valueOf(c) to p[i] }
            }
            repeat(if (taught == 0) 1 else 5) { rep ->
                val shuffled = own.shuffled(Random(rep * 1000 + cat.hashCode()))
                val teach = shuffled.take(taught)
                val test = shuffled.drop(taught)
                val personal = if (teach.isEmpty()) null else PersonalModel(teach.map { LabeledExample(it.x, intentOf.getValue(it.label)) })
                for (s in test) {
                    val dist = translator.meowIntents(s.features, personal = personal?.predict(s.x), personalWeight = personal?.weight ?: 0.0)
                    val predicted = intentOf.entries.maxBy { dist[it.value] ?: 0.0 }.key
                    hits[s.label][1]++
                    if (predicted == s.label) hits[s.label][0]++
                }
            }
        }
        val balanced = hits.map { it[0].toDouble() / it[1].coerceAtLeast(1) }.average()
        println("Personalisation: $taught taught examples -> balanced accuracy %.3f".format(balanced))
        results += taught to balanced
    }
    return results
}

private fun render(
    m: SoftmaxRegression,
    e: Evaluation,
    within: Evaluation,
    personalization: List<Pair<Int, Double>>,
    n: Int,
    cats: Int,
    l2: Double,
): String {
    fun arr(a: DoubleArray) = a.joinToString(", ") { "%.6f".format(it) }
    val sb = StringBuilder()
    sb.appendLine("// GENERATED by :trainer (TrainContextModel.kt) — do not edit by hand.")
    sb.appendLine("// Trained on the CatMeows dataset (Ludovico, Ntalampiras, Presti et al., doi:10.5281/zenodo.4008297, CC BY-NC 4.0).")
    sb.appendLine("package com.rrblack.meowtranslator.core.classify")
    sb.appendLine()
    sb.appendLine("internal object ContextModelWeights {")
    sb.appendLine("    const val TRAINING_CALLS = $n")
    sb.appendLine("    const val TRAINING_CATS = $cats")
    sb.appendLine("    const val L2 = $l2")
    sb.appendLine("    /** Leave-one-cat-out cross-validated accuracy (chance for the majority class is ~0.50, balanced chance 0.33). */")
    sb.appendLine("    const val LOCO_ACCURACY = %.4f".format(e.accuracy))
    sb.appendLine("    const val LOCO_BALANCED_ACCURACY = %.4f".format(e.balancedAccuracy))
    sb.appendLine("    /** Random 10-fold accuracy, i.e. when the model has already heard other meows from the same cats. */")
    sb.appendLine("    const val WITHIN_CAT_BALANCED_ACCURACY = %.4f".format(within.balancedAccuracy))
    sb.appendLine("    /** Simulated owner teaching on unseen cats: (examples taught, balanced accuracy of the full translator). */")
    sb.appendLine("    val PERSONALIZATION_CURVE = listOf(${personalization.joinToString { "%d to %.4f".format(it.first, it.second) }})")
    sb.appendLine("    val CLASSES = listOf(${m.classes.joinToString { "\"$it\"" }})")
    sb.appendLine("    val FEATURES = listOf(${SegmentFeatures.FEATURE_NAMES.joinToString { "\"$it\"" }})")
    sb.appendLine("    val CONFUSION = arrayOf(${e.confusion.joinToString { "intArrayOf(${it.joinToString()})" }})")
    sb.appendLine("    val MEAN = doubleArrayOf(${arr(m.mean)})")
    sb.appendLine("    val SCALE = doubleArrayOf(${arr(m.scale)})")
    sb.appendLine("    val WEIGHTS = arrayOf(")
    m.weights.forEach { sb.appendLine("        doubleArrayOf(${arr(it)}),") }
    sb.appendLine("    )")
    sb.appendLine("    val BIAS = doubleArrayOf(${arr(m.bias)})")
    sb.appendLine("}")
    return sb.toString()
}
