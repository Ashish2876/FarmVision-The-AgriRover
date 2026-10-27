package com.farmvision.agrirover.ui.hardware.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.data.model.hardware.ControllerTarget
import com.farmvision.agrirover.ui.hardware.model.EndpointConnectionStatus
import com.farmvision.agrirover.ui.theme.LocalAgriStatusColors

/**
 * Controller endpoint card displaying telemetry freshness, firmware version, and connection state.
 * Strictly separates the main ESP32 DevKit V1 and the ESP32-CAM as independent network endpoints.
 */
@Composable
fun ControllerEndpointCard(
    target: ControllerTarget,
    title: String,
    subtitle: String,
    connectionStatus: EndpointConnectionStatus,
    endpointAddress: String = "192.168.4.1 (Draft)",
    firmwareVersion: String? = null,
    uptimeText: String? = null,
    lastSeenText: String = "Never",
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val statusColors = LocalAgriStatusColors.current
    val (statusColor, statusBg) = when (connectionStatus) {
        EndpointConnectionStatus.Connected -> statusColors.online to statusColors.online.copy(alpha = 0.15f)
        EndpointConnectionStatus.Connecting -> statusColors.connecting to statusColors.connecting.copy(alpha = 0.15f)
        EndpointConnectionStatus.Disconnected -> statusColors.offline to statusColors.offline.copy(alpha = 0.15f)
        EndpointConnectionStatus.Stale -> statusColors.warning to statusColors.warning.copy(alpha = 0.15f)
        EndpointConnectionStatus.Timeout -> statusColors.warning to statusColors.warning.copy(alpha = 0.15f)
        EndpointConnectionStatus.Error -> statusColors.offline to statusColors.offline.copy(alpha = 0.15f)
        EndpointConnectionStatus.Unknown -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
        EndpointConnectionStatus.NotConfigured -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
    }

    val icon: ImageVector = if (target == ControllerTarget.Esp32Cam) {
        Icons.Rounded.CameraAlt
    } else {
        Icons.Rounded.DeveloperBoard
    }


    val cardModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = "$title: ${connectionStatus.label}"
            }
            .clickable(onClick = onClick)
    } else {
        modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$title: ${connectionStatus.label}"
            }
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(FarmVisionSpacing.medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(FarmVisionSpacing.borderThin, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FarmVisionSpacing.medium)
        ) {
            // Header Row: Icon, Title, Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(FarmVisionSpacing.iconSizeMedium)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(FarmVisionSpacing.small))

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(FarmVisionSpacing.small))

                // Connection Chip: Text + Icon + Color
                Surface(
                    shape = RoundedCornerShape(FarmVisionSpacing.extraSmall),
                    color = statusBg,
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = FarmVisionSpacing.small, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = connectionStatus.icon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(FarmVisionSpacing.iconSizeSmall)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = connectionStatus.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            // Endpoint & Diagnostics Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Endpoint Target",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = endpointAddress,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Last Response",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = lastSeenText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (firmwareVersion != null || uptimeText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = firmwareVersion ?: "Firmware: Not reported",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uptimeText ?: "Uptime: --",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
