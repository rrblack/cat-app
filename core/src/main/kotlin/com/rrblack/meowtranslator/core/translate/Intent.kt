package com.rrblack.meowtranslator.core.translate

/** What the cat is most likely trying to communicate. */
enum class Intent(val title: String, val summary: String, val emoji: String) {
    GREETING("Greeting", "Saying hello or acknowledging you.", "👋"),
    FOOD("Food request", "Asking to be fed — often around mealtimes or near the bowl.", "🍽️"),
    ATTENTION("Wants something", "Asking for attention, play, a door opened, or for you to come.", "🙋"),
    HANDLING("About being touched", "Reacting to being petted, brushed, or picked up.", "🖐️"),
    DISTRESS("Stressed / lonely", "Uneasy, alone, or somewhere unfamiliar — looking for reassurance.", "😿"),
    COMPLAINT("Annoyed", "Displeased or protesting about something.", "😾"),
    CONTENT("Content", "Relaxed and comfortable.", "😌"),
    SOLICITING("Purring for something", "A purr with an urgent, cry-like edge — usually asking for food.", "🥺"),
    FRIENDLY("Friendly hello", "A warm greeting, sometimes an invitation to follow.", "😺"),
    HUNTING("Prey excitement", "Excited and frustrated by prey it can see but not reach.", "🐦"),
    DEFENSIVE("Back off", "Feeling threatened and warning you or another animal away.", "🙀"),
    THREAT("Warning", "Upset and prepared to defend itself.", "⚠️"),
    CALLING_OUT("Calling out", "A long, loud call — distress, mating, territorial, or disorientation.", "📢"),
    UNCLEAR("Unclear", "Not enough clean cat sound to interpret.", "❓"),
    ;

    companion object {
        /** Intents a meow can carry; these are what owners can teach the personal model. */
        val MEOW_INTENTS = listOf(GREETING, FOOD, ATTENTION, HANDLING, DISTRESS, COMPLAINT)
    }
}
