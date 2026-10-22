package com.farmvision.agrirover.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing

/**
 * FarmVision Primary Button (Filled).
 * High-emphasis action trigger with guaranteed minimum 48dp touch target,
 * optional leading icon, and loading spinner state.
 */
@Composable
fun FarmVisionPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = FarmVisionSpacing.minTouchTarget, minHeight = FarmVisionSpacing.minTouchTarget)
            .semantics { role = Role.Button },
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = FarmVisionSpacing.large, vertical = FarmVisionSpacing.medium)
    ) {
        ButtonContent(text = text, leadingIcon = leadingIcon, isLoading = isLoading, contentColor = MaterialTheme.colorScheme.onPrimary)
    }
}

/**
 * FarmVision Secondary Button (Tonal).
 * Medium-emphasis action trigger using secondary container styling.
 */
@Composable
fun FarmVisionSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = FarmVisionSpacing.minTouchTarget, minHeight = FarmVisionSpacing.minTouchTarget)
            .semantics { role = Role.Button },
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        contentPadding = PaddingValues(horizontal = FarmVisionSpacing.large, vertical = FarmVisionSpacing.medium)
    ) {
        ButtonContent(text = text, leadingIcon = leadingIcon, isLoading = isLoading, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

/**
 * FarmVision Outlined Button.
 * Secondary boundary-defined action trigger with subtle border and clear text.
 */
@Composable
fun FarmVisionOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = FarmVisionSpacing.minTouchTarget, minHeight = FarmVisionSpacing.minTouchTarget)
            .semantics { role = Role.Button },
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(FarmVisionSpacing.borderThin, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        ),
        contentPadding = PaddingValues(horizontal = FarmVisionSpacing.large, vertical = FarmVisionSpacing.medium)
    ) {
        ButtonContent(text = text, leadingIcon = leadingIcon, isLoading = isLoading, contentColor = MaterialTheme.colorScheme.primary)
    }
}

/**
 * FarmVision Text Button.
 * Low-emphasis inline trigger for auxiliary actions.
 */
@Composable
fun FarmVisionTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = FarmVisionSpacing.minTouchTarget, minHeight = FarmVisionSpacing.minTouchTarget)
            .semantics { role = Role.Button },
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        ),
        contentPadding = PaddingValues(horizontal = FarmVisionSpacing.medium, vertical = FarmVisionSpacing.small)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(FarmVisionSpacing.iconSizeMedium)
                )
                Spacer(modifier = Modifier.width(FarmVisionSpacing.small))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * FarmVision Icon Button.
 * Compact interactive icon with enforced minimum 48dp accessible touch target.
 */
@Composable
fun FarmVisionIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(FarmVisionSpacing.minTouchTarget)
            .semantics { role = Role.Button },
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.38f),
            modifier = Modifier.size(FarmVisionSpacing.iconSizeLarge)
        )
    }
}

@Composable
private fun ButtonContent(
    text: String,
    leadingIcon: ImageVector?,
    isLoading: Boolean,
    contentColor: Color
) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = contentColor,
            strokeWidth = 2.dp
        )
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(FarmVisionSpacing.iconSizeMedium),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(FarmVisionSpacing.small))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
