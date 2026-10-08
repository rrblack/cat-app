pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MeowTranslator"

// Pure-Kotlin signal processing + translation engine (no Android dependencies, unit-testable on the JVM).
include(":core")
// Offline tool that trains the meow-context model on the CatMeows dataset and regenerates its weights.
include(":trainer")
// The Android app.
include(":app")
