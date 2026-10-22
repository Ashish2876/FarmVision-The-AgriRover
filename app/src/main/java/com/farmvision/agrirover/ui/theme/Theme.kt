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
    primary = FreshLeaf,
    onPrimary = DarkBackground,
    primaryContainer = DeepForest,
    onPrimaryContainer = MintAccent,

    secondary = MintAccent,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkThemeText,

    tertiary = PrimaryGreen,
    onTertiary = DarkBackground,
    tertiaryContainer = DarkSurfaceContainerHigh,
    onTertiaryContainer = MintAccent,

    background = DarkBackground,
    onBackground = DarkThemeText,

    surface = DarkSurface,
    onSurface = DarkThemeText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextMutedDark,

    outline = BorderSubtleDark,
    outlineVariant = BorderAccentDark,

    error = StatusAlert,
    onError = DarkBackground
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimaryGreen,
    onPrimary = WarmOffWhite,
    primaryContainer = MintAccent,
    onPrimaryContainer = DeepForest,

    secondary = DeepForest,
    onSecondary = WarmOffWhite,
    secondaryContainer = Color(0xFFE2EDE5),
    onSecondaryContainer = DeepForest,

    tertiary = FreshLeaf,
    onTertiary = DarkBackground,
    tertiaryContainer = Color(0xFFC7EBD4),
    onTertiaryContainer = DeepForest,

    background = WarmOffWhite,
    onBackground = PrimaryTextLight,

    surface = LightSurface,
    onSurface = PrimaryTextLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextMutedLight,

    outline = BorderSubtleLight,
    outlineVariant = BorderAccentLight,

    error = Color(0xFFBA2518),
    onError = WarmOffWhite
)

@Composable
fun FarmVisionTheme(
    darkTheme: Boolean = true, // Default to futuristic dark mode for high-tech rover aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val statusColors = if (darkTheme) DarkAgriStatusColors else LightAgriStatusColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalAgriStatusColors provides statusColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
