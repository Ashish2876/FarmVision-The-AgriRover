package com.farmvision.agrirover.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============================================================================
// FarmVision AgriRover — Color Scheme Mappings
// ============================================================================

private val DarkColorScheme = darkColorScheme(
    primary = ParrotGreen,
    onPrimary = TextPrimary,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = LightLime,

    secondary = LightLime,
    onSecondary = TextPrimary,
    secondaryContainer = DarkSurfaceHigh,
    onSecondaryContainer = DarkThemeText,

    tertiary = ParrotGreenDeep,
    onTertiary = TextPrimary,
    tertiaryContainer = DarkSurfaceHigh,
    onTertiaryContainer = LightLime,

    background = DarkBackground,
    onBackground = DarkThemeText,

    surface = DarkSurface,
    onSurface = DarkThemeText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextMutedDark,

    outline = BorderSubtleDark,
    outlineVariant = BorderAccentDark,

    error = Color(0xFFF87171),
    onError = DarkBackground
)

private val LightColorScheme = lightColorScheme(
    primary = ParrotGreen,
    onPrimary = TextPrimary,
    primaryContainer = LightLime,
    onPrimaryContainer = DarkGreen,

    secondary = DarkGreen,
    onSecondary = PureWhite,
    secondaryContainer = PaleGreen,
    onSecondaryContainer = DarkGreen,

    tertiary = ParrotGreenDeep,
    onTertiary = TextPrimary,
    tertiaryContainer = LightLime,
    onTertiaryContainer = DarkGreen,

    background = SoftWhite,
    onBackground = TextPrimary,

    surface = PureWhite,
    onSurface = TextPrimary,
    surfaceVariant = PaleGreen,
    onSurfaceVariant = TextSecondary,

    outline = BorderSubtle,
    outlineVariant = Color(0xFFC5D4BD),

    error = Color(0xFFBA2518),
    onError = PureWhite
)

@Composable
fun FarmVisionTheme(
    darkTheme: Boolean = false, // Default to fresh vibrant Parrot Green + Pure White light aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val statusColors = if (darkTheme) DarkAgriStatusColors else LightAgriStatusColors
    val chartColors = DefaultChartSeriesColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = (if (darkTheme) colorScheme.background else PureWhite).toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalAgriStatusColors provides statusColors,
        LocalChartSeriesColors provides chartColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
