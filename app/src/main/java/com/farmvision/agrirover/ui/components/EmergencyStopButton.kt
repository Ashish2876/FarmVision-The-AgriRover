package com.farmvision.agrirover.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.ui.theme.DarkBackground
import com.farmvision.agrirover.ui.theme.DarkSurfaceVariant
import com.farmvision.agrirover.ui.theme.FarmVisionTheme
import com.farmvision.agrirover.ui.theme.StatusAlert

/**
 * High-priority Emergency Stop (E-Stop) safety button.
 * Designed according to industrial machinery safety guidelines with high-visibility hazard styling,
 * pulsing emergency beacon, and haptic-ready press response.
 */
@Composable
fun EmergencyStopButton(
    onEmergencyStopTriggered: () -> Unit,
    modifier: Modifier = Modifier,
    isEngaged: Boolean = false,
    size: Dp = 100.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "eStopHalo")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    val buttonScale = if (isPressed) 0.92f else 1.0f

    Box(
        modifier = modifier.size(size + 30.dp),
        contentAlignment = Alignment.Center
    ) {
        // Pulsing Warning Halo (active when armed and not yet engaged)
        if (!isEngaged) {
            Box(
                modifier = Modifier
                    .size(size + 20.dp)
                    .scale(haloScale)
                    .alpha(haloAlpha)
                    .clip(CircleShape)
                    .background(StatusAlert)
            )
        }

        // Protective Outer Bezel Ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(DarkSurfaceVariant, DarkBackground)
                    )
                )
                .border(
                    BorderStroke(2.dp, if (isEngaged) Color(0xFF555555) else StatusAlert.copy(alpha = 0.8f)),
                    CircleShape
                )
        ) {
            // Inner Mushroom Strike Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(size - 18.dp)
                    .scale(buttonScale)
                    .clip(CircleShape)
                    .background(
                        if (isEngaged) {
                            Brush.verticalGradient(listOf(Color(0xFF4A1E1B), Color(0xFF2E1210)))
                        } else {
                            Brush.verticalGradient(listOf(Color(0xFFFF4D4D), StatusAlert, Color(0xFF9E1B1B)))
                        }
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = !isEngaged,
                        onClick = onEmergencyStopTriggered
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = "Emergency Stop",
                        tint = Color.White,
                        modifier = Modifier.size((size.value * 0.28f).dp)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (isEngaged) "CUTOFF" else "E-STOP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        fontSize = (size.value * 0.11f).sp,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Preview(name = "E-Stop Button Preview", showBackground = true, backgroundColor = 0xFF0B1410)
@Composable
private fun EmergencyStopButtonPreview() {
    FarmVisionTheme {
        Box(modifier = Modifier.padding(24.dp)) {
            EmergencyStopButton(
                onEmergencyStopTriggered = {}
            )
        }
    }
}
