package com.rrblack.meowtranslator.core.classify

import com.rrblack.meowtranslator.core.analysis.SegmentFeatures

/**
 * Situational context of a meow, learned from the CatMeows dataset (21 cats, 440 meows recorded while
 * being brushed, waiting for food, or isolated in an unfamiliar room).
 */
enum class MeowContext { BRUSHING, FOOD, ISOLATION }

object ContextModel {
    private val model = SoftmaxRegression(
        classes = ContextModelWeights.CLASSES,
        mean = ContextModelWeights.MEAN,
        scale = ContextModelWeights.SCALE,
        weights = ContextModelWeights.WEIGHTS,
        bias = ContextModelWeights.BIAS,
    )

    val crossCatAccuracy: Double get() = ContextModelWeights.LOCO_ACCURACY
    val crossCatBalancedAccuracy: Double get() = ContextModelWeights.LOCO_BALANCED_ACCURACY
    val trainingCalls: Int get() = ContextModelWeights.TRAINING_CALLS
    val trainingCats: Int get() = ContextModelWeights.TRAINING_CATS
    val withinCatBalancedAccuracy: Double get() = ContextModelWeights.WITHIN_CAT_BALANCED_ACCURACY

    /** Simulated owner teaching on cats the model never saw: (examples taught, balanced accuracy). */
    val personalizationCurve: List<Pair<Int, Double>> get() = ContextModelWeights.PERSONALIZATION_CURVE

    /** Rows = true context, columns = predicted, in [MeowContext] order. */
    val crossCatConfusion: List<List<Int>> get() = ContextModelWeights.CONFUSION.map { it.toList() }

    fun predict(features: SegmentFeatures): Map<MeowContext, Double> {
        val p = model.predict(features.toModelVector())
        return ContextModelWeights.CLASSES.withIndex().associate { (i, name) -> MeowContext.valueOf(name) to p[i] }
    }
}
