package com.farmvision.agrirover.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing

/**
 * Reusable container providing consistent background tint, sizing, and shape for icons.
 */
@Composable
fun FarmVisionIconContainer(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    iconTint: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = RoundedCornerShape(10.dp),
    size: Dp = 40.dp,
    iconSize: Dp = FarmVisionSpacing.iconSizeMedium
) {
    Surface(
        modifier = modifier.size(size),
        shape = shape,
        color = containerColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * Standard horizontal divider adhering to FarmVision border widths and outline tokens.
 */
@Composable
fun FarmVisionDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
    thickness: Dp = FarmVisionSpacing.borderThin
) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = thickness,
        color = color
    )
}
