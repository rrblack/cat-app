package com.rrblack.meowtranslator.core.classify

/**
 * Cat call types, following the acoustic repertoire described by Moelk (1944), Bradshaw & Cameron-Beaumont
 * (2000) and Tavernier et al. (2020).
 */
enum class VocalType(val displayName: String, val description: String) {
    MEOW(
        "Meow",
        "Open-then-closing mouth call. Adult cats rarely meow at each other — it is mostly aimed at humans.",
    ),
    TRILL(
        "Trill / chirrup",
        "Short, rolled, usually rising call made with the mouth closed. Used as a friendly greeting or to get you to follow.",
    ),
    PURR(
        "Purr",
        "Low, continuous pulsing at ~20–30 Hz. Usually contentment, but cats also purr to self-soothe or to solicit.",
    ),
    HISS(
        "Hiss",
        "Unvoiced burst of noise with the mouth open. A defensive warning.",
    ),
    GROWL(
        "Growl",
        "Low-pitched, rough, sustained call. An offensive or defensive threat.",
    ),
    YOWL(
        "Yowl / howl",
        "Long, loud, drawn-out call. Seen in mating, territorial disputes, distress and disorientation.",
    ),
    CHATTER(
        "Chatter",
        "Rapid series of short clicks or chirps, typically made while watching prey through a window.",
    ),
    UNKNOWN(
        "Unclear sound",
        "Not enough clean cat sound to tell what it was.",
    ),
}
