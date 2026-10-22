package com.farmvision.agrirover.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.ui.theme.BorderAccentDark
import com.farmvision.agrirover.ui.theme.BorderSubtleDark
import com.farmvision.agrirover.ui.theme.DarkBackground
import com.farmvision.agrirover.ui.theme.DarkSurfaceHigh
import com.farmvision.agrirover.ui.theme.DarkSurfaceVariant
import com.farmvision.agrirover.ui.theme.DeepForest
import com.farmvision.agrirover.ui.theme.FarmVisionTheme
import com.farmvision.agrirover.ui.theme.FreshLeaf
import com.farmvision.agrirover.ui.theme.MintAccent
import com.farmvision.agrirover.ui.theme.PrimaryGreen
import com.farmvision.agrirover.ui.theme.StatusAlert
import com.farmvision.agrirover.ui.theme.TextMutedDark

enum class TactileButtonVariant {
    Primary,
    Secondary,
    Tonal,
    Danger
}

/**
 * High-contrast tactile action button engineered for outdoor field conditions and glove-friendly touch targets.
 * Features animated tactile depression feedback and loading spinner states.
 */
@Composable
fun TactileButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: TactileButtonVariant = TactileButtonVariant.Primary,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.96f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "tactileButtonPress"
    )

    val shape = RoundedCornerShape(14.dp)

    val (backgroundBrush, contentColor, borderStroke) = when (variant) {
        TactileButtonVariant.Primary -> Triple(
            Brush.horizontalGradient(listOf(PrimaryGreen, FreshLeaf)),
            DarkBackground,
            null
        )
        TactileButtonVariant.Secondary -> Triple(
            Brush.horizontalGradient(listOf(DarkSurfaceVariant, DarkSurfaceHigh)),
            MintAccent,
            BorderStroke(1.dp, BorderAccentDark)
        )
        TactileButtonVariant.Tonal -> Triple(
            Brush.horizontalGradient(listOf(DeepForest, Color(0xFF1B4D36))),
            MintAccent,
            BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.4f))
        )
        TactileButtonVariant.Danger -> Triple(
            Brush.horizontalGradient(listOf(StatusAlert, Color(0xFFC0392B))),
            Color.White,
            null
        )
    }

    val activeAlpha = if (enabled) 1.0f else 0.45f

    Surface(
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 50.dp)
            .clip(shape)
            .then(
                if (borderStroke != null && enabled) {
                    Modifier.background(borderStroke.brush, shape)
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = Color.Transparent,
        border = if (enabled) borderStroke else BorderStroke(1.dp, BorderSubtleDark)
    ) {
        Box(
            modifier = Modifier
                .background(backgroundBrush, shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled && !isLoading,
                    onClick = onClick
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = contentColor.copy(alpha = activeAlpha),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = contentColor.copy(alpha = activeAlpha)
                    )

                    if (trailingIcon != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = null,
                            tint = contentColor.copy(alpha = activeAlpha),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Tactile Buttons Preview", showBackground = true, backgroundColor = 0xFF0B1410)
@Composable
private fun TactileButtonPreview() {
    FarmVisionTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TactileButton(
                text = "Primary Trigger",
                onClick = {},
                variant = TactileButtonVariant.Primary
            )
            TactileButton(
                text = "Secondary",
                onClick = {},
                variant = TactileButtonVariant.Secondary
            )
        }
    }
}
