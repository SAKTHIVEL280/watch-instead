package com.lockin.app.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.lockin.app.domain.Accent

private class Tones(
    val lightPrimary: Long,
    val lightContainer: Long,
    val lightOnContainer: Long,
    val darkPrimary: Long,
    val darkOnPrimary: Long,
    val darkContainer: Long,
    val darkOnContainer: Long,
    val lightSurface: Long,
    val darkSurface: Long,
)

private fun tones(accent: Accent) = when (accent) {
    Accent.INDIGO -> Tones(
        0xFF4F46C8, 0xFFE3DFFF, 0xFF17006E, 0xFFC5C0FF, 0xFF2800A0, 0xFF3B2FAF, 0xFFE3DFFF, 0xFFFCF8FF, 0xFF131318,
    )
    Accent.ROSE -> Tones(
        0xFFB3261E, 0xFFFFDAD6, 0xFF410002, 0xFFFFB4AB, 0xFF690005, 0xFF93000A, 0xFFFFDAD6, 0xFFFFF8F7, 0xFF1A1111,
    )
    Accent.TEAL -> Tones(
        0xFF006A60, 0xFF9EF2E4, 0xFF00201C, 0xFF82D5C8, 0xFF003731, 0xFF005048, 0xFF9EF2E4, 0xFFF4FBF8, 0xFF0F1513,
    )
    Accent.AMBER -> Tones(
        0xFF7C5800, 0xFFFFDEA6, 0xFF271900, 0xFFF6BE48, 0xFF412D00, 0xFF5E4200, 0xFFFFDEA6, 0xFFFFF8F2, 0xFF17130B,
    )
}

/** Swatch shown in settings. */
fun Accent.swatch(): Color = Color(tones(this).lightPrimary)

/** Tints the container surfaces, which would otherwise keep the library's default violet cast. */
private fun ColorScheme.tinted(): ColorScheme {
    fun tint(alpha: Float) = primary.copy(alpha = alpha).compositeOver(surface)
    return copy(
        surfaceContainerLowest = surface,
        surfaceContainerLow = tint(0.05f),
        surfaceContainer = tint(0.08f),
        surfaceContainerHigh = tint(0.11f),
        surfaceContainerHighest = tint(0.14f),
    )
}

fun accentColorScheme(accent: Accent, dark: Boolean): ColorScheme {
    val t = tones(accent)
    val scheme = if (dark) {
        darkColorScheme(
            primary = Color(t.darkPrimary),
            onPrimary = Color(t.darkOnPrimary),
            primaryContainer = Color(t.darkContainer),
            onPrimaryContainer = Color(t.darkOnContainer),
            secondaryContainer = Color(t.darkContainer),
            onSecondaryContainer = Color(t.darkOnContainer),
            tertiary = Color(t.darkPrimary),
            tertiaryContainer = Color(t.darkContainer),
            onTertiaryContainer = Color(t.darkOnContainer),
            background = Color(t.darkSurface),
            surface = Color(t.darkSurface),
        )
    } else {
        lightColorScheme(
            primary = Color(t.lightPrimary),
            onPrimary = Color.White,
            primaryContainer = Color(t.lightContainer),
            onPrimaryContainer = Color(t.lightOnContainer),
            secondaryContainer = Color(t.lightContainer),
            onSecondaryContainer = Color(t.lightOnContainer),
            tertiary = Color(t.lightPrimary),
            tertiaryContainer = Color(t.lightContainer),
            onTertiaryContainer = Color(t.lightOnContainer),
            background = Color(t.lightSurface),
            surface = Color(t.lightSurface),
        )
    }
    return scheme.tinted()
}
