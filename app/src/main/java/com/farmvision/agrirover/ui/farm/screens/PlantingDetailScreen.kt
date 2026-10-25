package com.farmvision.agrirover.ui.farm.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCard
import com.farmvision.agrirover.core.designsystem.component.FarmVisionPrimaryButton
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusBadge
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.domain.model.farm.GrowthStage
import com.farmvision.agrirover.domain.model.farm.PlantHealthStatus
import com.farmvision.agrirover.domain.model.farm.RecordOrigin
import com.farmvision.agrirover.ui.farm.management.PlantingViewItem
import java.time.LocalDate

/**
 * Screen displaying comprehensive metadata for an individual crop planting,
 * growth stage, health status, harvest horizon, and simulated hydration controls.
 */
@Composable
fun PlantingDetailScreen(
    item: PlantingViewItem,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSimulateIrrigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val planting = item.planting
    val isPlanned = planting.plantingDate != null && planting.plantingDate.isAfter(LocalDate.now())

    Column(modifier = modifier.fillMaxWidth()) {
        // Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to Plot Details",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = planting.cropName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${item.farmName} • ${item.plotName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit Planting",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete Planting",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        // Crop Overview Card
        FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = planting.cropName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!planting.variety.isNullOrBlank()) {
                        Text(
                            text = planting.variety,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (planting.origin == RecordOrigin.SAMPLE) {
                    FarmVisionStatusBadge(status = FarmVisionStatusType.DemoData, customLabel = "SAMPLE CROP")
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

            // Planting Date / Planned Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Planting Date: ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val dateText = when {
                        planting.plantingDate == null -> "Not recorded"
                        isPlanned -> "${planting.plantingDate} (Planned)"
                        else -> planting.plantingDate.toString()
                    }
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isPlanned) FontWeight.Bold else FontWeight.Normal,
                        color = if (isPlanned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isPlanned) {
                    FarmVisionStatusBadge(status = FarmVisionStatusType.DemoData, customLabel = "PLANNED")
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            // Growth Stage & Health Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Grass,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Growth Stage: ${planting.growthStage.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (planting.userHealthStatus) {
                        PlantHealthStatus.Thriving -> MaterialTheme.colorScheme.primaryContainer
                        PlantHealthStatus.Nominal -> MaterialTheme.colorScheme.secondaryContainer
                        PlantHealthStatus.NeedsAttention -> MaterialTheme.colorScheme.tertiaryContainer
                        PlantHealthStatus.Critical -> MaterialTheme.colorScheme.errorContainer
                        PlantHealthStatus.InsufficientData -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = planting.userHealthStatus.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (planting.userHealthStatus) {
                            PlantHealthStatus.Thriving -> MaterialTheme.colorScheme.onPrimaryContainer
                            PlantHealthStatus.Nominal -> MaterialTheme.colorScheme.onSecondaryContainer
                            PlantHealthStatus.NeedsAttention -> MaterialTheme.colorScheme.onTertiaryContainer
                            PlantHealthStatus.Critical -> MaterialTheme.colorScheme.onErrorContainer
                            PlantHealthStatus.InsufficientData -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (planting.daysToHarvest != null) {
                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
                Text(
                    text = "Expected Harvest: ~${planting.daysToHarvest} days remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!planting.userNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Notes: ${planting.userNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

        // Hydration & Irrigation Card
        FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.WaterDrop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Soil Hydration (Simulated)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                FarmVisionStatusBadge(
                    status = if (item.isWatered) FarmVisionStatusType.Active else FarmVisionStatusType.DemoData,
                    customLabel = if (item.isWatered) "IRRIGATED" else "${String.format(java.util.Locale.US, "%.1f", item.currentMoisture)}%"
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (item.isWatered) {
                        "Hydration logged. Soil moisture elevated to 75%. Hydration level reflects on the Home field moisture metric."
                    } else {
                        "Moisture level is at ${String.format(java.util.Locale.US, "%.1f", item.currentMoisture)}%. Triggering simulated irrigation will schedule furrow absorption."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

            FarmVisionPrimaryButton(
                text = if (item.isWatered) "Irrigation Active (Tap to Reset)" else "Simulate Irrigation",
                onClick = onSimulateIrrigation,
                leadingIcon = if (item.isWatered) Icons.Rounded.Check else Icons.Rounded.WaterDrop,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
    }
}
