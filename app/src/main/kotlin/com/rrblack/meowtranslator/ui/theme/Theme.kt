package com.rrblack.meowtranslator.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.LocaleList
import com.rrblack.meowtranslator.core.i18n.Language

private val LightColors = lightColorScheme(
    primary = Color(0xFF9A4A00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC7),
    onPrimaryContainer = Color(0xFF321300),
    secondary = Color(0xFF765848),
    secondaryContainer = Color(0xFFFFDBC9),
    onSecondaryContainer = Color(0xFF2B160A),
    tertiary = Color(0xFF646032),
    tertiaryContainer = Color(0xFFEBE4AA),
    background = Color(0xFFFFFBF7),
    surface = Color(0xFFFFFBF7),
    surfaceVariant = Color(0xFFF4DED3),
    onSurfaceVariant = Color(0xFF52443C),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB688),
    onPrimary = Color(0xFF532200),
    primaryContainer = Color(0xFF763300),
    onPrimaryContainer = Color(0xFFFFDBC7),
    secondary = Color(0xFFE6BEAB),
    secondaryContainer = Color(0xFF5D4132),
    onSecondaryContainer = Color(0xFFFFDBC9),
    tertiary = Color(0xFFCEC890),
    tertiaryContainer = Color(0xFF4C481D),
    background = Color(0xFF1A120D),
    surface = Color(0xFF1A120D),
    surfaceVariant = Color(0xFF52443C),
    onSurfaceVariant = Color(0xFFD7C2B8),
)

/**
 * Tags every text style with the display language. For Japanese this makes the font fallback pick
 * Japanese glyph shapes for kanji even on a phone whose system language is English or Chinese.
 */
private fun typographyFor(language: Language): Typography {
    val locales = LocaleList(language.tag)
    fun TextStyle.tagged() = copy(localeList = locales)
    val t = Typography()
    return Typography(
        displayLarge = t.displayLarge.tagged(), displayMedium = t.displayMedium.tagged(), displaySmall = t.displaySmall.tagged(),
        headlineLarge = t.headlineLarge.tagged(), headlineMedium = t.headlineMedium.tagged(), headlineSmall = t.headlineSmall.tagged(),
        titleLarge = t.titleLarge.tagged(), titleMedium = t.titleMedium.tagged(), titleSmall = t.titleSmall.tagged(),
        bodyLarge = t.bodyLarge.tagged(), bodyMedium = t.bodyMedium.tagged(), bodySmall = t.bodySmall.tagged(),
        labelLarge = t.labelLarge.tagged(), labelMedium = t.labelMedium.tagged(), labelSmall = t.labelSmall.tagged(),
    )
}

@Composable
fun MeowTheme(language: Language = Language.ENGLISH, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, typography = typographyFor(language), content = content)
}
