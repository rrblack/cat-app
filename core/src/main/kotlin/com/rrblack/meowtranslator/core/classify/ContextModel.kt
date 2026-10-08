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

    fun predict(features: SegmentFeatures): Map<MeowContext, Double> {
        val p = model.predict(features.toModelVector())
        return ContextModelWeights.CLASSES.withIndex().associate { (i, name) -> MeowContext.valueOf(name) to p[i] }
    }
}
