package com.farmvision.agrirover.ui.rover.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.ui.theme.DarkGreen
import com.farmvision.agrirover.ui.theme.ParrotGreen
import com.farmvision.agrirover.ui.theme.TextPrimary

/**
 * Circular Directional D-Pad Controller with center STOP nub.
 * Preserves all required Phase 6 test tags:
 * - rover_ctrl_forward
 * - rover_ctrl_left
 * - rover_ctrl_stop
 * - rover_ctrl_right
 * - rover_ctrl_reverse
 */
@Composable
fun CircularDirectionalController(
    activeCommand: String,
    isEnabled: Boolean,
    onDirectionClick: (String) -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Forward (Up)
        Surface(
            onClick = { onDirectionClick("FORWARD") },
            enabled = isEnabled,
            shape = CircleShape,
            color = if (activeCommand == "FORWARD" && isEnabled) ParrotGreen else MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(
                if (activeCommand == "FORWARD" && isEnabled) 2.dp else 1.dp,
                if (activeCommand == "FORWARD" && isEnabled) DarkGreen else MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .size(56.dp)
                .testTag("rover_ctrl_forward")
                .semantics { role = Role.Button }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Drive Forward",
                    tint = if (activeCommand == "FORWARD" && isEnabled) TextPrimary else DarkGreen,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Middle Row: Left, Center STOP, Right
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Left
            Surface(
                onClick = { onDirectionClick("TURN LEFT") },
                enabled = isEnabled,
                shape = CircleShape,
                color = if (activeCommand == "TURN LEFT" && isEnabled) ParrotGreen else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    if (activeCommand == "TURN LEFT" && isEnabled) 2.dp else 1.dp,
                    if (activeCommand == "TURN LEFT" && isEnabled) DarkGreen else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .size(56.dp)
                    .testTag("rover_ctrl_left")
                    .semantics { role = Role.Button }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Turn Left",
                        tint = if (activeCommand == "TURN LEFT" && isEnabled) TextPrimary else DarkGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Center STOP Nub
            Surface(
                onClick = onStopClick,
                enabled = isEnabled,
                shape = CircleShape,
                color = if (isEnabled) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(
                    2.dp,
                    if (isEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .size(64.dp)
                    .testTag("rover_ctrl_stop")
                    .semantics { role = Role.Button }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Stop,
                            contentDescription = "Hold Position / Stop",
                            tint = if (isEnabled) MaterialTheme.colorScheme.error else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "STOP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isEnabled) MaterialTheme.colorScheme.error else Color.Gray,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            // Right
            Surface(
                onClick = { onDirectionClick("TURN RIGHT") },
                enabled = isEnabled,
                shape = CircleShape,
                color = if (activeCommand == "TURN RIGHT" && isEnabled) ParrotGreen else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    if (activeCommand == "TURN RIGHT" && isEnabled) 2.dp else 1.dp,
                    if (activeCommand == "TURN RIGHT" && isEnabled) DarkGreen else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .size(56.dp)
                    .testTag("rover_ctrl_right")
                    .semantics { role = Role.Button }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Turn Right",
                        tint = if (activeCommand == "TURN RIGHT" && isEnabled) TextPrimary else DarkGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Reverse (Down)
        Surface(
            onClick = { onDirectionClick("REVERSE") },
            enabled = isEnabled,
            shape = CircleShape,
            color = if (activeCommand == "REVERSE" && isEnabled) ParrotGreen else MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(
                if (activeCommand == "REVERSE" && isEnabled) 2.dp else 1.dp,
                if (activeCommand == "REVERSE" && isEnabled) DarkGreen else MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .size(56.dp)
                .testTag("rover_ctrl_reverse")
                .semantics { role = Role.Button }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Drive Reverse",
                    tint = if (activeCommand == "REVERSE" && isEnabled) TextPrimary else DarkGreen,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

