package com.rrblack.meowtranslator.core.i18n

import com.rrblack.meowtranslator.core.analysis.ContourShape
import com.rrblack.meowtranslator.core.classify.MeowContext
import com.rrblack.meowtranslator.core.classify.VocalType
import com.rrblack.meowtranslator.core.i18n.Texts.Companion.oneDecimal
import com.rrblack.meowtranslator.core.i18n.Texts.Companion.pct
import com.rrblack.meowtranslator.core.translate.AgeGroup
import com.rrblack.meowtranslator.core.translate.ConfidenceLevel
import com.rrblack.meowtranslator.core.translate.EmotionLabel
import com.rrblack.meowtranslator.core.translate.Evidence
import com.rrblack.meowtranslator.core.translate.Intensity
import com.rrblack.meowtranslator.core.translate.Intent
import com.rrblack.meowtranslator.core.translate.Note
import com.rrblack.meowtranslator.core.translate.PhraseKey
import com.rrblack.meowtranslator.core.translate.QualityWarning
import java.util.Locale
import kotlin.math.roundToInt

object EnglishTexts : Texts {
    override val language = Language.ENGLISH
    override val insistentSuffix = " (and I'll keep asking)"
    override val defaultSpeaker = "Your cat"

    private fun set(low: List<String>, medium: List<String>, high: List<String>) =
        mapOf(Intensity.LOW to low, Intensity.MEDIUM to medium, Intensity.HIGH to high)

    private val phraseBank: Map<PhraseKey, Map<Intensity, List<String>>> = mapOf(
        PhraseKey.GREETING to set(
            listOf("Oh, hi.", "Hey, you.", "Mm-hello."),
            listOf("Hi! There you are.", "Hello, human!", "Oh good, it's you."),
            listOf("HI! You're back! Hi!", "There you are! I missed you!", "Hello hello hello!"),
        ),
        PhraseKey.FOOD to set(
            listOf("Is it almost dinner time?", "I could eat…", "Just a reminder: my bowl exists."),
            listOf("Food, please!", "Feed me? It's time, I'm sure of it.", "Can we go to the kitchen now?"),
            listOf("FOOD. NOW. PLEASE!", "I am STARVING! The bowl! The bowl!", "Hurry up, I'm so hungry!"),
        ),
        PhraseKey.ATTENTION to set(
            listOf("Hey… could you come here?", "Psst. Look at me.", "I'd like something, please."),
            listOf("Hey! Pay attention to me.", "Come here, I want something.", "Can you open this? Or play with me?"),
            listOf("Hey! HEY! Over here!", "I need you RIGHT NOW!", "Stop what you're doing and come here!"),
        ),
        PhraseKey.HANDLING_POSITIVE to set(
            listOf("Mm, that's nice.", "Okay… keep going.", "I don't mind this."),
            listOf("Ooh, right there.", "Yes, more of that please.", "That feels good."),
            listOf("Don't stop!", "More! More!", "Yes! That's the spot!"),
        ),
        PhraseKey.HANDLING_NEGATIVE to set(
            listOf("Hmm, I'm not sure about this.", "That's probably enough.", "Careful…"),
            listOf("Okay, that's enough now.", "Hey, I don't love this.", "Can you stop, please?"),
            listOf("STOP. Enough!", "Let go of me!", "I've had enough of this!"),
        ),
        PhraseKey.DISTRESS to set(
            listOf("Where is everyone?", "I feel a bit uneasy.", "Is anyone there?"),
            listOf("I don't like it here. Come get me.", "Where are you? I'm worried.", "Please don't leave me alone."),
            listOf("Help! Get me out of here!", "I'm scared! Where ARE you?", "Come back! I don't like this at all!"),
        ),
        PhraseKey.COMPLAINT to set(
            listOf("Ugh.", "Hmph. Not impressed.", "I disapprove."),
            listOf("I'm not happy about this.", "Seriously? No.", "This is unacceptable."),
            listOf("I am VERY annoyed!", "Absolutely not!", "Stop it! I hate this!"),
        ),
        PhraseKey.CONTENT to set(
            listOf("All is well. I'm comfy.", "So cozy right now.", "Life is good."),
            listOf("I'm happy and relaxed.", "This is nice. Stay here.", "I feel safe with you."),
            listOf("Bliss. Absolute bliss.", "I love this! Don't move.", "Best. Moment. Ever."),
        ),
        PhraseKey.SOLICITING to set(
            listOf("I'm lovely, aren't I? …Snack?", "Purr… a treat would be nice.", "I'm being sweet for a reason."),
            listOf("Purr — and now, food please.", "I'm being adorable. Feed me.", "Wouldn't you like to fill my bowl?"),
            listOf("Purrrr… FOOD. Please. Now.", "You can't resist this purr. Feed me!", "Purring as hard as I can — breakfast!"),
        ),
        PhraseKey.FRIENDLY to set(
            listOf("Mrrp? Oh, hi.", "Hey, friend.", "Mrrt."),
            listOf("Mrrp! Hello! Follow me!", "Hi! Come see this!", "Hey, you! Come along!"),
            listOf("Mrrrp! So happy to see you!", "Hi hi! Come on, this way!", "Brrrp! You're here!"),
        ),
        PhraseKey.HUNTING to set(
            listOf("Ooh… what's that?", "I see something…", "Hmm, a bird."),
            listOf("A bird! I want it!", "Look at that! Let me at it!", "Prey spotted!"),
            listOf("I MUST CATCH IT!", "Bird bird bird bird!", "Let me out, I need to get it!"),
        ),
        PhraseKey.DEFENSIVE to set(
            listOf("Back off a little.", "Don't come closer.", "I don't trust this."),
            listOf("Back off! I feel threatened.", "Stay away from me!", "I'm warning you — leave me alone."),
            listOf("GET AWAY FROM ME!", "Back OFF! Now!", "Don't you dare!"),
        ),
        PhraseKey.THREAT to set(
            listOf("I'm not comfortable.", "Leave me be.", "Don't push it."),
            listOf("I'm warning you. Leave me alone.", "Back away, slowly.", "This is your last warning."),
            listOf("I WILL fight you!", "One more step and I swat!", "Leave. NOW."),
        ),
        PhraseKey.CALLING_OUT_DISTRESS to set(
            listOf("Is anybody out there?", "Hellooo?", "Where did everyone go?"),
            listOf("Where is everyone?! Come find me!", "I'm calling for you — answer me!", "Hello? HELLO?"),
            listOf("HELP! Where are you?!", "Somebody come here right now!", "I'm lost and I don't like it!"),
        ),
        PhraseKey.CALLING_OUT_MATING to set(
            listOf("Any cats around here?", "I'm looking for company.", "Hello out there…"),
            listOf("Is anyone out there for me?", "Looking for love!", "Come find me!"),
            listOf("I NEED A MATE!", "Answer me, out there!", "Where are you, my love?!"),
        ),
        PhraseKey.UNCLEAR to listOf("I didn't catch that — try again closer to your cat.").let { set(it, it, it) },
        PhraseKey.HUMAN_VOICE to listOf("That sounded like a person talking, not your cat. Try again when your cat meows.").let { set(it, it, it) },
        PhraseKey.NOT_A_CAT to listOf("That didn't sound like a cat — just background noise. Try again when your cat makes a sound.").let { set(it, it, it) },
    )

    override fun phrases(key: PhraseKey, intensity: Intensity) = phraseBank.getValue(key).getValue(intensity)

    override fun intentTitle(intent: Intent) = when (intent) {
        Intent.GREETING -> "Greeting"
        Intent.FOOD -> "Food request"
        Intent.ATTENTION -> "Wants something"
        Intent.HANDLING -> "About being touched"
        Intent.DISTRESS -> "Stressed / lonely"
        Intent.COMPLAINT -> "Annoyed"
        Intent.CONTENT -> "Content"
        Intent.SOLICITING -> "Purring for something"
        Intent.FRIENDLY -> "Friendly hello"
        Intent.HUNTING -> "Prey excitement"
        Intent.DEFENSIVE -> "Back off"
        Intent.THREAT -> "Warning"
        Intent.CALLING_OUT -> "Calling out"
        Intent.UNCLEAR -> "Unclear"
    }

    override fun intentSummary(intent: Intent) = when (intent) {
        Intent.GREETING -> "Saying hello or acknowledging you."
        Intent.FOOD -> "Asking to be fed — often around mealtimes or near the bowl."
        Intent.ATTENTION -> "Asking for attention, play, a door opened, or for you to come."
        Intent.HANDLING -> "Reacting to being petted, brushed, or picked up."
        Intent.DISTRESS -> "Uneasy, alone, or somewhere unfamiliar — looking for reassurance."
        Intent.COMPLAINT -> "Displeased or protesting about something."
        Intent.CONTENT -> "Relaxed and comfortable."
        Intent.SOLICITING -> "A purr with an urgent, cry-like edge — usually asking for food."
        Intent.FRIENDLY -> "A warm greeting, sometimes an invitation to follow."
        Intent.HUNTING -> "Excited and frustrated by prey it can see but not reach."
        Intent.DEFENSIVE -> "Feeling threatened and warning you or another animal away."
        Intent.THREAT -> "Upset and prepared to defend itself."
        Intent.CALLING_OUT -> "A long, loud call — distress, mating, territorial, or disorientation."
        Intent.UNCLEAR -> "Not enough clean cat sound to interpret."
    }

    override fun vocalTypeName(type: VocalType) = when (type) {
        VocalType.MEOW -> "Meow"
        VocalType.TRILL -> "Trill / chirrup"
        VocalType.PURR -> "Purr"
        VocalType.HISS -> "Hiss"
        VocalType.GROWL -> "Growl"
        VocalType.YOWL -> "Yowl / howl"
        VocalType.CHATTER -> "Chatter"
        VocalType.UNKNOWN -> "Unclear sound"
    }

    override fun vocalTypeDescription(type: VocalType) = when (type) {
        VocalType.MEOW -> "Open-then-closing mouth call. Adult cats rarely meow at each other — it is mostly aimed at humans."
        VocalType.TRILL -> "Short, rolled, usually rising call made with the mouth closed. Used as a friendly greeting or to get you to follow."
        VocalType.PURR -> "Low, continuous pulsing at ~20–30 Hz. Usually contentment, but cats also purr to self-soothe or to solicit."
        VocalType.HISS -> "Unvoiced burst of noise with the mouth open. A defensive warning."
        VocalType.GROWL -> "Low-pitched, rough, sustained call. An offensive or defensive threat."
        VocalType.YOWL -> "Long, loud, drawn-out call. Seen in mating, territorial disputes, distress and disorientation."
        VocalType.CHATTER -> "Rapid series of short clicks or chirps, typically made while watching prey through a window."
        VocalType.UNKNOWN -> "Not enough clean cat sound to tell what it was."
    }

    override fun emotion(label: EmotionLabel) = when (label) {
        EmotionLabel.EXCITED_HAPPY -> "Excited & happy"
        EmotionLabel.CALM_CONTENT -> "Calm & content"
        EmotionLabel.STRESSED -> "Stressed / upset"
        EmotionLabel.UNEASY -> "Uneasy / displeased"
        EmotionLabel.ALERT -> "Alert & eager"
        EmotionLabel.NEUTRAL -> "Neutral"
    }

    override fun contour(shape: ContourShape) = when (shape) {
        ContourShape.RISING -> "rising"
        ContourShape.FALLING -> "falling"
        ContourShape.RISE_FALL -> "rise-fall (arched)"
        ContourShape.FALL_RISE -> "fall-rise"
        ContourShape.FLAT -> "level"
        ContourShape.UNVOICED -> "no clear pitch"
    }

    override fun confidence(level: ConfidenceLevel) = when (level) {
        ConfidenceLevel.HIGH -> "High"
        ConfidenceLevel.MODERATE -> "Moderate"
        ConfidenceLevel.LOW -> "Low"
    }

    override fun context(context: MeowContext) = when (context) {
        MeowContext.FOOD -> "Waiting for food"
        MeowContext.BRUSHING -> "Being brushed"
        MeowContext.ISOLATION -> "Alone in an unfamiliar place"
    }

    override fun ageGroup(age: AgeGroup) = when (age) {
        AgeGroup.KITTEN -> "Kitten (< 1 year)"
        AgeGroup.ADULT -> "Adult"
        AgeGroup.SENIOR -> "Senior (11+ years)"
    }

    override fun evidence(e: Evidence): String = when (e) {
        is Evidence.CallType -> {
            val pitch = if (e.f0Hz > 0) "pitch around ${e.f0Hz.roundToInt()} Hz" else "no clear pitch"
            "Sounds like a ${vocalTypeName(e.type).lowercase(Locale.US)} (${pct(e.probability)} match): ${oneDecimal(e.durationSec)} s long, $pitch."
        }
        is Evidence.DataModel -> {
            val situation = when (e.context) {
                MeowContext.FOOD -> "cats waiting to be fed"
                MeowContext.BRUSHING -> "cats being brushed by their owner"
                MeowContext.ISOLATION -> "cats left alone in an unfamiliar room"
            }
            "Data model (${e.trainingCalls} real meows from ${e.trainingCats} cats): most similar to meows from $situation (${pct(e.probability)})."
        }
        is Evidence.Melody -> {
            val dir = when {
                e.endMinusStartSt > 1 -> "ends higher than it starts (+${oneDecimal(e.endMinusStartSt)} semitones)"
                e.endMinusStartSt < -1 -> "ends lower than it starts (${oneDecimal(e.endMinusStartSt)} semitones)"
                else -> "starts and ends at a similar pitch"
            }
            "Melody: ${contour(e.shape)}; $dir. Rising meows lean positive or requesting, falling ones lean stressed (Schötz et al.)."
        }
        Evidence.ShortCall -> "Short and brief — typical of greetings and acknowledgements."
        Evidence.LongCall -> "Long, drawn-out call — signals more urgency."
        Evidence.RoughVoice -> "Rough, breathy voice quality — tends to go with negative or agitated states."
        is Evidence.PurrPulsing -> "Steady ~${e.rateHz.roundToInt()} Hz pulsing, the signature of a purr."
        is Evidence.TrillFlutter -> "Short, rolled call with ~${e.rateHz.roundToInt()} Hz flutter — a trill/chirrup."
        Evidence.HissNoise -> "Noise-like, no pitch, energy spread across the spectrum — a hiss."
        Evidence.GrowlLowRough -> "Low and rough, energy concentrated in low frequencies — a growl."
        Evidence.YowlLong -> "Very long voiced call with large pitch movement — a yowl."
        Evidence.ChatterBurst -> "Rapid burst of very short calls — chattering."
        is Evidence.Repeated -> "Repeated ${e.count} times — repetition signals insistence."
        is Evidence.PersonalMatch -> "Matches meows you've labelled as \"${intentTitle(e.intent)}\" (${e.examples} examples taught)."
        is Evidence.PersonalWeight -> "Your ${e.examples} taught examples were weighed in (${pct(e.weight)} influence)."
        is Evidence.IgnoredSounds -> buildList {
            if (e.human > 0) add("${e.human} bit${if (e.human == 1) "" else "s"} of human voice")
            if (e.other > 0) add("${e.other} other sound${if (e.other == 1) "" else "s"}")
        }.joinToString(" and ").let { "Ignored $it that didn't sound like your cat." }
    }

    override fun note(note: Note) = when (note) {
        Note.PURR_CAN_MEAN_STRESS -> "Cats also purr when stressed, unwell or in pain — check body language if the situation seems off."
        Note.GIVE_SPACE -> "Give space and let the cat retreat; don't punish warnings — they're how cats avoid fights."
        Note.SENIOR_YOWLING -> "Night-time yowling in older cats can be a sign of hyperthyroidism, high blood pressure, hearing loss or cognitive decline — worth a vet check."
        Note.INTACT_YOWLING -> "Intact cats yowl to attract mates; neutering usually stops this."
        Note.CHATTER_PLAY -> "Window-watching is enrichment. Play sessions with a wand toy let them finish the \"hunt\"."
        Note.KITTEN_MEOWING -> "Kittens meow mostly for food, warmth and company — frequent meowing is normal at this age."
        Note.DISTRESS_VET -> "If distress calls are new, frequent, or come with hiding or appetite changes, talk to your vet."
        Note.BODY_LANGUAGE -> "Sound is only part of the message: ears, tail, eyes and the situation matter just as much."
    }

    override fun warning(warning: QualityWarning) = when (warning) {
        QualityWarning.NO_SOUND -> "No distinct sound found above the background noise."
        QualityWarning.CLIPPED -> "The recording is distorted (too loud) — hold the phone a little further away."
        QualityWarning.NOISY -> "Lots of background noise — results are less reliable."
        QualityWarning.TOO_QUIET -> "Very quiet recording — try getting closer to your cat."
    }
}
