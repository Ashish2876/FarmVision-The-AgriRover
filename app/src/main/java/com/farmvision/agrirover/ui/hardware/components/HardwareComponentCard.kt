package com.farmvision.agrirover.ui.hardware.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Engineering
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.WaterDrop
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.data.model.hardware.ComponentCategory
import com.farmvision.agrirover.data.model.hardware.ConfigurationStatus
import com.farmvision.agrirover.ui.hardware.model.EndpointConnectionStatus
import com.farmvision.agrirover.ui.hardware.model.HardwareUiItem
import com.farmvision.agrirover.ui.hardware.model.SensorHealthStatus
import com.farmvision.agrirover.ui.theme.LocalAgriStatusColors

/**
 * Standard Hardware Component Card matching Section 6 anatomy:
 * - Icon container (styled by category)
 * - Name and model identity
 * - Intended role line
 * - Connection status chip + Diagnostic health chip
 * - Latest reading with unit (tabular figures)
 * - Freshness indicator
 * - Chevron indicating detail navigation
 * - Accessible minimum 48dp touch target
 */
@Composable
fun HardwareComponentCard(
    item: HardwareUiItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalAgriStatusColors.current
    val categoryIcon = getCategoryIcon(item.component.category)

    val (connectionColor, connectionBg) = if (!item.isElectronic) {
        MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.surfaceVariant
    } else {
        when (item.connectionStatus) {
            EndpointConnectionStatus.Connected -> statusColors.online to statusColors.online.copy(alpha = 0.12f)
            EndpointConnectionStatus.Connecting -> statusColors.connecting to statusColors.connecting.copy(alpha = 0.12f)
            EndpointConnectionStatus.Disconnected -> statusColors.offline to statusColors.offline.copy(alpha = 0.12f)
            EndpointConnectionStatus.Stale -> statusColors.warning to statusColors.warning.copy(alpha = 0.12f)
            EndpointConnectionStatus.Timeout -> statusColors.warning to statusColors.warning.copy(alpha = 0.12f)
            EndpointConnectionStatus.Error -> statusColors.offline to statusColors.offline.copy(alpha = 0.12f)
            EndpointConnectionStatus.Unknown -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f)
            EndpointConnectionStatus.NotConfigured -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f)
        }
    }

    val connectionLabel = if (!item.isElectronic) {
        "Cataloged (Passive)"
    } else {
        item.connectionStatus.label
    }

    val diagnosticLabel = when (item.component.status) {
        ConfigurationStatus.NotConfigured -> "Not Configured"
        ConfigurationStatus.Draft -> "Draft Spec"
        ConfigurationStatus.Verified -> "Verified"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = "${item.component.displayName}, ${item.component.category.displayName}, $connectionLabel, Status: $diagnosticLabel"
            }
            .clickable(onClick = onClick),
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
            // Row 1: Icon, Name, Model, and Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(FarmVisionSpacing.small),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(FarmVisionSpacing.iconSizeMedium)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(FarmVisionSpacing.small))

                    Column {
                        Text(
                            text = item.component.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.component.modelDesignation,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "View Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(FarmVisionSpacing.iconSizeLarge)
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            // Row 2: Intended Role
            Text(
                text = item.component.intendedRole,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            // Row 3: Status Chip (Connection) & Diagnostic Chip (Configuration Maturity)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connection chip
                Surface(
                    shape = RoundedCornerShape(FarmVisionSpacing.extraSmall),
                    color = connectionBg,
                    border = BorderStroke(1.dp, connectionColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = connectionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = connectionColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Diagnostic configuration maturity chip
                Surface(
                    shape = RoundedCornerShape(FarmVisionSpacing.extraSmall),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = diagnosticLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (item.component.quantity != null) {
                    Text(
                        text = "Qty: ${item.component.quantity}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            // Row 4: Telemetry / Command Value & Freshness
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reading: ${item.latestValueDisplay}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = item.freshnessText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getCategoryIcon(category: ComponentCategory): ImageVector {
    return when (category) {
        ComponentCategory.Controller -> Icons.Rounded.DeveloperBoard
        ComponentCategory.Camera -> Icons.Rounded.CameraAlt
        ComponentCategory.ProgrammingTool -> Icons.Rounded.Build
        ComponentCategory.Sensor -> Icons.Rounded.Sensors
        ComponentCategory.MotorDriver -> Icons.Rounded.Memory
        ComponentCategory.Motor -> Icons.Rounded.Agriculture
        ComponentCategory.Servo -> Icons.Rounded.Engineering
        ComponentCategory.ServoHardware -> Icons.Rounded.Build
        ComponentCategory.Irrigation -> Icons.Rounded.WaterDrop
        ComponentCategory.Power -> Icons.Rounded.Power
        ComponentCategory.Assembly -> Icons.Rounded.Build
    }
}
