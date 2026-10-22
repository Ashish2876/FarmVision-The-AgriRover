package com.farmvision.agrirover.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Standard W3C WCAG 2.1 relative luminance and contrast ratio calculator.
 * Used for automated verification of text and graphical contrast compliance.
 */
object ContrastCalculator {

    /**
     * Calculates the relative luminance of a Compose Color according to WCAG 2.1.
     * Normalized to [0.0, 1.0].
     */
    fun calculateLuminance(color: Color): Double {
        fun channelLuminance(c: Float): Double {
            val d = c.toDouble()
            return if (d <= 0.03928) {
                d / 12.92
            } else {
                ((d + 0.055) / 1.055).pow(2.4)
            }
        }

        val r = channelLuminance(color.red)
        val g = channelLuminance(color.green)
        val b = channelLuminance(color.blue)

        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /**
     * Calculates the contrast ratio between two colors.
     * Output range is 1.0 to 21.0.
     */
    fun calculateContrastRatio(foreground: Color, background: Color): Double {
        val lum1 = calculateLuminance(foreground)
        val lum2 = calculateLuminance(background)

        val lighter = max(lum1, lum2)
        val darker = min(lum1, lum2)

        return (lighter + 0.05) / (darker + 0.05)
    }

    /**
     * Checks if a contrast ratio satisfies WCAG AA for normal text (minimum 4.5:1).
     */
    fun isWcagAaNormalText(foreground: Color, background: Color): Boolean {
        return calculateContrastRatio(foreground, background) >= 4.5
    }

    /**
     * Checks if a contrast ratio satisfies WCAG AA for large text (minimum 3.0:1)
     * and graphical UI elements/icons.
     */
    fun isWcagAaLargeTextOrGraphic(foreground: Color, background: Color): Boolean {
        return calculateContrastRatio(foreground, background) >= 3.0
    }
}
