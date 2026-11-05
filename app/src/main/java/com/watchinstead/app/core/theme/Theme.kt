package com.watchinstead.app.core.theme

import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.watchinstead.app.data.settings.SettingsRepository
import com.watchinstead.app.domain.Appearance
import com.watchinstead.app.domain.ThemeMode

private val WatchInsteadShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

val supportsDynamicColor: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun Appearance.isDark(): Boolean = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WatchInsteadTheme(appearance: Appearance = Appearance(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = appearance.isDark()
    val scheme = when {
        appearance.dynamicColor && supportsDynamicColor ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> accentColorScheme(appearance.accent, dark)
    }.let { if (dark && appearance.pureBlack) it.withPureBlack() else it }
    MaterialExpressiveTheme(
        colorScheme = scheme.animated(),
        motionScheme = MotionScheme.expressive(),
        shapes = WatchInsteadShapes,
        content = content,
    )
}

private fun ColorScheme.withPureBlack() = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0C),
    surfaceContainer = Color(0xFF111114),
)

/** Cross-fades the roles that cover the most pixels, so theme changes glide instead of flash. */
@Composable
private fun ColorScheme.animated(): ColorScheme {
    @Composable fun Color.fade(): Color {
        val color by animateColorAsState(this, tween(THEME_FADE_MILLIS), label = "theme colour")
        return color
    }
    return copy(
        primary = primary.fade(),
        onPrimary = onPrimary.fade(),
        primaryContainer = primaryContainer.fade(),
        onPrimaryContainer = onPrimaryContainer.fade(),
        secondaryContainer = secondaryContainer.fade(),
        onSecondaryContainer = onSecondaryContainer.fade(),
        background = background.fade(),
        onBackground = onBackground.fade(),
        surface = surface.fade(),
        onSurface = onSurface.fade(),
        onSurfaceVariant = onSurfaceVariant.fade(),
        surfaceContainer = surfaceContainer.fade(),
        surfaceContainerHigh = surfaceContainerHigh.fade(),
        surfaceContainerHighest = surfaceContainerHighest.fade(),
    )
}

private const val THEME_FADE_MILLIS = 350

/**
 * Sets content under the user's chosen appearance. Nothing is drawn until it has loaded, so a
 * screen never flashes the wrong theme; every activity must go through here to stay consistent.
 */
fun ComponentActivity.setWatchInsteadContent(settings: SettingsRepository, content: @Composable () -> Unit) {
    setContent {
        val appearance by settings.appearance.collectAsState(null as Appearance?)
        appearance?.let { current ->
            val dark = current.isDark()
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark }
                enableEdgeToEdge(style, style)
                onDispose { }
            }
            WatchInsteadTheme(current, content)
        }
    }
}
