package com.farmvision.agrirover.ui.farm.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
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
import com.farmvision.agrirover.core.designsystem.component.FarmVisionEmptyState
import com.farmvision.agrirover.core.designsystem.component.FarmVisionOutlinedButton
import com.farmvision.agrirover.core.designsystem.component.FarmVisionPrimaryButton
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusBadge
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.domain.model.farm.AreaUnit
import com.farmvision.agrirover.domain.model.farm.Farm
import com.farmvision.agrirover.domain.model.farm.GrowthStage
import com.farmvision.agrirover.domain.model.farm.Planting
import com.farmvision.agrirover.domain.model.farm.Plot
import com.farmvision.agrirover.ui.farm.management.PlantingViewItem
import java.time.LocalDate

/**
 * Screen displaying details for a specific Plot, its parent Farm,
 * and child plantings with add/edit/delete actions.
 */
@Composable
fun PlotDetailScreen(
    plot: Plot,
    farm: Farm,
    plantings: List<PlantingViewItem>,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAddPlantingClick: () -> Unit,
    onPlantingClick: (String) -> Unit,
    onSimulateIrrigation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
                        contentDescription = "Back to Farm Details",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = plot.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Plot in ${farm.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit Plot",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete Plot",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        // Plot Info Card
        FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Plot Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!plot.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
                Text(
                    text = plot.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (plot.areaSquareMeters != null) {
                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
                val ha = AreaUnit.Hectares.fromSquareMeters(plot.areaSquareMeters)
                Text(
                    text = "Area: ${String.format(java.util.Locale.US, "%.2f", ha)} ha (${String.format(java.util.Locale.US, "%,.0f", plot.areaSquareMeters)} m²)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!plot.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Notes: ${plot.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

        // Plantings Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "CROPS IN THIS PLOT (${plantings.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FarmVisionOutlinedButton(
                text = "Add Crop",
                onClick = onAddPlantingClick,
                leadingIcon = Icons.Rounded.Add
            )
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

        if (plantings.isEmpty()) {
            FarmVisionEmptyState(
                title = "No Crops in Plot",
                message = "This plot has no recorded crops or plantings.",
                actionText = "Record First Crop",
                onActionClick = onAddPlantingClick
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FarmVisionSpacing.medium)
            ) {
                plantings.forEach { item ->
                    PlotPlantingCard(
                        item = item,
                        onClick = { onPlantingClick(item.planting.id) },
                        onSimulateIrrigation = { onSimulateIrrigation(item.planting.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
    }
}

@Composable
private fun PlotPlantingCard(
    item: PlantingViewItem,
    onClick: () -> Unit,
    onSimulateIrrigation: () -> Unit
) {
    val planting = item.planting
    val isPlanned = planting.plantingDate != null && planting.plantingDate.isAfter(LocalDate.now())

    FarmVisionCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = planting.cropName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!planting.variety.isNullOrBlank()) {
                    Text(
                        text = planting.variety,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (isPlanned) {
                    FarmVisionStatusBadge(status = FarmVisionStatusType.DemoData, customLabel = "PLANNED")
                } else if (planting.growthStage != GrowthStage.Unspecified) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = planting.growthStage.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

        // Conditions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.WaterDrop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Moisture: ${String.format(java.util.Locale.US, "%.1f", item.currentMoisture)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (planting.daysToHarvest != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Harvest in ${planting.daysToHarvest}d",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

        // Action Button
        FarmVisionPrimaryButton(
            text = if (item.isWatered) "Irrigation Active (Tap to Reset)" else "Simulate Irrigation",
            onClick = onSimulateIrrigation,
            leadingIcon = if (item.isWatered) Icons.Rounded.Check else Icons.Rounded.WaterDrop,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
