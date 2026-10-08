package com.rrblack.meowtranslator.core.translate

/**
 * What the cat is most likely trying to communicate.
 * Display text lives in [Texts] so every language is rendered from the same result.
 */
enum class Intent(val emoji: String) {
    GREETING("👋"),
    FOOD("🍽️"),
    ATTENTION("🙋"),
    HANDLING("🖐️"),
    DISTRESS("😿"),
    COMPLAINT("😾"),
    CONTENT("😌"),
    SOLICITING("🥺"),
    FRIENDLY("😺"),
    HUNTING("🐦"),
    DEFENSIVE("🙀"),
    THREAT("⚠️"),
    CALLING_OUT("📢"),
    UNCLEAR("❓"),
    ;

    companion object {
        /** Intents a meow can carry; these are what owners can teach the personal model. */
        val MEOW_INTENTS = listOf(GREETING, FOOD, ATTENTION, HANDLING, DISTRESS, COMPLAINT)
    }
}
