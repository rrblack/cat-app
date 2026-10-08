package com.rrblack.meowtranslator.core.classify

import com.rrblack.meowtranslator.core.translate.Intent
import kotlin.math.sqrt

/** A meow the owner has labelled with what was really going on. */
data class LabeledExample(val features: DoubleArray, val intent: Intent) {
    override fun equals(other: Any?) = other is LabeledExample && other.intent == intent && other.features.contentEquals(features)
    override fun hashCode() = 31 * features.contentHashCode() + intent.hashCode()
}

/**
 * Learns one particular cat's "dialect" from owner feedback.
 *
 * Research consistently finds that meows are highly individual: classifiers do far better within a cat than
 * across cats, and owners understand their own cat better than strangers do (Nicastro & Owren 2003;
 * Prato-Previde et al. 2020). So owner-labelled examples are the strongest evidence available.
 *
 * Distance-weighted k-nearest-neighbours on the same standardised features as the context model.
 */
class PersonalModel(private val examples: List<LabeledExample>) {
    val size: Int get() = examples.size

    /** How much to trust the personal model relative to the general one (0..~0.85). */
    val weight: Double get() = if (examples.isEmpty()) 0.0 else 0.85 * examples.size / (examples.size + HALF_TRUST_AT)

    fun predict(features: DoubleArray, k: Int = 7): Map<Intent, Double>? {
        if (examples.isEmpty()) return null
        val scale = ContextModelWeights.SCALE
        val nearest = examples.map { ex ->
            var d = 0.0
            for (i in features.indices) {
                val diff = (features[i] - ex.features[i]) / scale[i]
                d += diff * diff
            }
            ex to sqrt(d / features.size)
        }.sortedBy { it.second }.take(k)

        val votes = HashMap<Intent, Double>()
        for ((ex, d) in nearest) votes[ex.intent] = (votes[ex.intent] ?: 0.0) + 1.0 / (0.25 + d)
        // Laplace smoothing over the labels this owner actually uses, so one example can't be 100% certain.
        val labels = examples.map { it.intent }.toSet()
        val total = votes.values.sum() + 0.5 * labels.size
        return labels.associateWith { ((votes[it] ?: 0.0) + 0.5) / total }
    }

    companion object {
        /** With this many examples the personal model gets ~half of its maximum weight. */
        const val HALF_TRUST_AT = 12.0
    }
}
