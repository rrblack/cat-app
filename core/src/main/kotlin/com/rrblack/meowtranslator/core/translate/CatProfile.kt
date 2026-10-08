package com.rrblack.meowtranslator.core.translate

enum class AgeGroup { KITTEN, ADULT, SENIOR }

/** Optional facts about the cat that change how some calls should be read. */
data class CatProfile(
    val name: String = "",
    val ageGroup: AgeGroup = AgeGroup.ADULT,
    /** null = unknown. Intact cats yowl to attract mates; that reading is unlikely for neutered cats. */
    val neutered: Boolean? = null,
)
