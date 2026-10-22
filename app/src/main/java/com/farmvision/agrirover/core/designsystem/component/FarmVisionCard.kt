package com.farmvision.agrirover.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing

/**
 * Base FarmVision surface card adhering to Material 3 elevation and tonal conventions.
 * Composes with [FarmVisionCardHeader] and [FarmVisionCardFooter] for modular layouts.
 */
@Composable
fun FarmVisionCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = FarmVisionSpacing.borderThin,
    contentPadding: PaddingValues = PaddingValues(FarmVisionSpacing.large),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .then(clickModifier),
        shape = shape,
        color = containerColor,
        border = BorderStroke(borderWidth, borderColor),
        tonalElevation = FarmVisionSpacing.elevationLow
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Composable card header with leading icon, title, optional supporting text,
 * and optional trailing action slot.
 */
@Composable
fun FarmVisionCardHeader(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color = MaterialTheme.colorScheme.primary,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = leadingIconTint.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = leadingIconTint,
                            modifier = Modifier.size(FarmVisionSpacing.iconSizeMedium)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(FarmVisionSpacing.medium))
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (trailingAction != null) {
            Spacer(modifier = Modifier.width(FarmVisionSpacing.small))
            trailingAction()
        }
    }
}

/**
 * Composable card footer slot for status indicators, timestamps, or action buttons.
 */
@Composable
fun FarmVisionCardFooter(
    modifier: Modifier = Modifier,
    statusContent: (@Composable () -> Unit)? = null,
    actionContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(modifier = Modifier.weight(1f, fill = false)) {
            statusContent?.invoke()
        }
        if (actionContent != null) {
            Spacer(modifier = Modifier.width(FarmVisionSpacing.small))
            actionContent()
        }
    }
}
