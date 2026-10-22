package com.farmvision.agrirover.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.ui.theme.BorderSubtleDark
import com.farmvision.agrirover.ui.theme.DarkSurface
import com.farmvision.agrirover.ui.theme.DarkSurfaceVariant

/**
 * Premium glassmorphic container card for FarmVision AgriRover.
 * Features subtle surface gradient illumination, layered depth, and aerodynamic borders.
 */
@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.dp,
    borderColor: Color = BorderSubtleDark,
    backgroundColors: List<Color> = listOf(
        DarkSurface,
        DarkSurfaceVariant.copy(alpha = 0.85f)
    ),
    elevation: Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .shadow(elevation, shape, clip = false)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(colors = backgroundColors)
            )
            .border(
                border = BorderStroke(borderWidth, borderColor),
                shape = shape
            )
            .then(clickableModifier)
            .padding(16.dp),
        content = content
    )
}
