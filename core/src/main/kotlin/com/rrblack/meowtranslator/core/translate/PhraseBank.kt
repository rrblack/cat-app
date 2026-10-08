package com.rrblack.meowtranslator.core.translate

/** Intensity of the call, derived from loudness, length, pitch and repetition. */
enum class Intensity { LOW, MEDIUM, HIGH }

/**
 * First-person renderings of each intent. Several wordings per intensity keep translations from feeling
 * canned; the choice is deterministic per recording so the same sound always reads the same way.
 */
internal object PhraseBank {
    private fun set(low: List<String>, medium: List<String>, high: List<String>) =
        mapOf(Intensity.LOW to low, Intensity.MEDIUM to medium, Intensity.HIGH to high)

    private val phrases: Map<String, Map<Intensity, List<String>>> = mapOf(
        "GREETING" to set(
            listOf("Oh, hi.", "Hey, you.", "Mm-hello."),
            listOf("Hi! There you are.", "Hello, human!", "Oh good, it's you."),
            listOf("HI! You're back! Hi!", "There you are! I missed you!", "Hello hello hello!"),
        ),
        "FOOD" to set(
            listOf("Is it almost dinner time?", "I could eat…", "Just a reminder: my bowl exists."),
            listOf("Food, please!", "Feed me? It's time, I'm sure of it.", "Can we go to the kitchen now?"),
            listOf("FOOD. NOW. PLEASE!", "I am STARVING! The bowl! The bowl!", "Hurry up, I'm so hungry!"),
        ),
        "ATTENTION" to set(
            listOf("Hey… could you come here?", "Psst. Look at me.", "I'd like something, please."),
            listOf("Hey! Pay attention to me.", "Come here, I want something.", "Can you open this? Or play with me?"),
            listOf("Hey! HEY! Over here!", "I need you RIGHT NOW!", "Stop what you're doing and come here!"),
        ),
        "HANDLING_POSITIVE" to set(
            listOf("Mm, that's nice.", "Okay… keep going.", "I don't mind this."),
            listOf("Ooh, right there.", "Yes, more of that please.", "That feels good."),
            listOf("Don't stop!", "More! More!", "Yes! That's the spot!"),
        ),
        "HANDLING_NEGATIVE" to set(
            listOf("Hmm, I'm not sure about this.", "That's probably enough.", "Careful…"),
            listOf("Okay, that's enough now.", "Hey, I don't love this.", "Can you stop, please?"),
            listOf("STOP. Enough!", "Let go of me!", "I've had enough of this!"),
        ),
        "DISTRESS" to set(
            listOf("Where is everyone?", "I feel a bit uneasy.", "Is anyone there?"),
            listOf("I don't like it here. Come get me.", "Where are you? I'm worried.", "Please don't leave me alone."),
            listOf("Help! Get me out of here!", "I'm scared! Where ARE you?", "Come back! I don't like this at all!"),
        ),
        "COMPLAINT" to set(
            listOf("Ugh.", "Hmph. Not impressed.", "I disapprove."),
            listOf("I'm not happy about this.", "Seriously? No.", "This is unacceptable."),
            listOf("I am VERY annoyed!", "Absolutely not!", "Stop it! I hate this!"),
        ),
        "CONTENT" to set(
            listOf("All is well. I'm comfy.", "So cozy right now.", "Life is good."),
            listOf("I'm happy and relaxed.", "This is nice. Stay here.", "I feel safe with you."),
            listOf("Bliss. Absolute bliss.", "I love this! Don't move.", "Best. Moment. Ever."),
        ),
        "SOLICITING" to set(
            listOf("I'm lovely, aren't I? …Snack?", "Purr… a treat would be nice.", "I'm being sweet for a reason."),
            listOf("Purr — and now, food please.", "I'm being adorable. Feed me.", "Wouldn't you like to fill my bowl?"),
            listOf("Purrrr… FOOD. Please. Now.", "You can't resist this purr. Feed me!", "Purring as hard as I can — breakfast!"),
        ),
        "FRIENDLY" to set(
            listOf("Mrrp? Oh, hi.", "Hey, friend.", "Mrrt."),
            listOf("Mrrp! Hello! Follow me!", "Hi! Come see this!", "Hey, you! Come along!"),
            listOf("Mrrrp! So happy to see you!", "Hi hi! Come on, this way!", "Brrrp! You're here!"),
        ),
        "HUNTING" to set(
            listOf("Ooh… what's that?", "I see something…", "Hmm, a bird."),
            listOf("A bird! I want it!", "Look at that! Let me at it!", "Prey spotted!"),
            listOf("I MUST CATCH IT!", "Bird bird bird bird!", "Let me out, I need to get it!"),
        ),
        "DEFENSIVE" to set(
            listOf("Back off a little.", "Don't come closer.", "I don't trust this."),
            listOf("Back off! I feel threatened.", "Stay away from me!", "I'm warning you — leave me alone."),
            listOf("GET AWAY FROM ME!", "Back OFF! Now!", "Don't you dare!"),
        ),
        "THREAT" to set(
            listOf("I'm not comfortable.", "Leave me be.", "Don't push it."),
            listOf("I'm warning you. Leave me alone.", "Back away, slowly.", "This is your last warning."),
            listOf("I WILL fight you!", "One more step and I swat!", "Leave. NOW."),
        ),
        "CALLING_OUT_DISTRESS" to set(
            listOf("Is anybody out there?", "Hellooo?", "Where did everyone go?"),
            listOf("Where is everyone?! Come find me!", "I'm calling for you — answer me!", "Hello? HELLO?"),
            listOf("HELP! Where are you?!", "Somebody come here right now!", "I'm lost and I don't like it!"),
        ),
        "CALLING_OUT_MATING" to set(
            listOf("Any cats around here?", "I'm looking for company.", "Hello out there…"),
            listOf("Is anyone out there for me?", "Looking for love!", "Come find me!"),
            listOf("I NEED A MATE!", "Answer me, out there!", "Where are you, my love?!"),
        ),
        "UNCLEAR" to set(
            listOf("…"),
            listOf("…"),
            listOf("…"),
        ),
    )

    fun phrase(key: String, intensity: Intensity, seed: Int): String {
        val options = phrases[key]?.get(intensity) ?: phrases.getValue("UNCLEAR").getValue(Intensity.MEDIUM)
        return options[Math.floorMod(seed, options.size)]
    }
}
