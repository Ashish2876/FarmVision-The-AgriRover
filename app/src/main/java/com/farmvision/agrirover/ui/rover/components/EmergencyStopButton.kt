package com.farmvision.agrirover.ui.rover.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.ui.theme.ControlStationTheme

/**
 * High-priority tactile Emergency Stop button conforming to industrial safety standards.
 * - Minimum 88 dp circular target
 * - Instant pointer-down engagement (zero release latency)
 * - Latching lockout requiring deliberate re-arm action
 * - Preserves `rover_ctrl_estop` and `rover_ctrl_rearm` test tags
 */
@Composable
fun EmergencyStopButton(
    isLatched: Boolean,
    onEmergencyStop: () -> Unit,
    onRearm: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp
) {
    val colors = ControlStationTheme.colors
    var isPressed by remember { mutableStateOf(false) }

    val baseRed = if (isLatched) colors.eStopRedPressed else colors.eStopRed
    val currentBg by animateColorAsState(
        targetValue = if (isPressed) colors.eStopRedPressed else baseRed,
        label = "eStopBg"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .testTag("rover_ctrl_estop")
                .semantics {
                    contentDescription = if (isLatched) "Emergency Stop Latched" else "Emergency Stop"
                    role = Role.Button
                }
                .pointerInput(isLatched) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        onEmergencyStop()
                        down.consume()

                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })

                        isPressed = false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Glowing red halo ring
            Canvas(modifier = Modifier.size(size)) {
                val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
                val radius = size.toPx() / 2f - 2f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.eStopRedGlow, Color.Transparent),
                        center = center,
                        radius = radius * 1.25f
                    ),
                    radius = radius * 1.25f,
                    center = center
                )

                // Outer border bevel
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.5f)
                )
            }

            // Central button dome
            Box(
                modifier = Modifier
                    .size(size - 6.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF5252),
                                currentBg,
                                Color(0xFF8B0000)
                            )
                        )
                    )
                    .border(BorderStroke(1.5.dp, Color(0xFFFFCDD2)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isLatched) "STOPPED" else "EMERGENCY\nSTOP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 8.5.sp,
                            lineHeight = 10.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}

/**
 * Re-arm safety banner displayed when Emergency Cutoff is latched.
 */
@Composable
fun EmergencyRearmBanner(
    onRearm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ControlStationTheme.colors

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xE67A0000))
            .border(BorderStroke(1.dp, Color(0xFFFF5252)), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "POWER CUTOFF ENGAGED",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White
                )
            )
            Text(
                text = "Emergency cutoff latched — deliberate re-arm required",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    color = Color(0xFFFFCDD2)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onRearm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.testTag("rover_ctrl_rearm")
            ) {
                Text(
                    text = "RE-ARM ROVER",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFFB71C1C)
                    )
                )
            }
        }
    }
}

