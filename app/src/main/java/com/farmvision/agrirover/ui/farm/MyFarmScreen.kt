package com.farmvision.agrirover.ui.farm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCard
import com.farmvision.agrirover.core.designsystem.component.FarmVisionDemoBanner
import com.farmvision.agrirover.core.designsystem.component.FarmVisionEmptyState
import com.farmvision.agrirover.core.designsystem.component.FarmVisionPrimaryButton
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusBadge
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.data.model.PlantItem
import com.farmvision.agrirover.ui.viewmodel.FarmViewModel

/**
 * My Farm screen for managing plots, crop health indices, and irrigation schedules.
 * Backed by shared [FarmViewModel] StateFlows. Irrigating crops directly updates
 * the overall field soil moisture observed on the Home dashboard.
 */
@Composable
fun MyFarmScreen(
    viewModel: FarmViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var selectedFilter by remember { mutableStateOf("All") }
    val plantsList by viewModel.plants.collectAsState()

    val filteredPlants = remember(selectedFilter, plantsList) {
        when (selectedFilter) {
            "Healthy" -> plantsList.filter { it.healthStatus == FarmVisionStatusType.Active }
            "Needs Attention" -> plantsList.filter { it.healthStatus == FarmVisionStatusType.Warning }
            else -> plantsList
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = FarmVisionSpacing.large, vertical = FarmVisionSpacing.medium)
        ) {
            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "My Farm",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Plots • Crop Catalog • Irrigation Advice",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                FarmVisionStatusBadge(status = FarmVisionStatusType.DemoData)
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

            FarmVisionDemoBanner(
                message = "DEMO DATA: Crop records, soil hydration levels, and watering recommendations are simulated."
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
            ) {
                listOf("All", "Healthy", "Needs Attention").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        onClick = { selectedFilter = filter },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = filter,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            if (filteredPlants.isEmpty()) {
                FarmVisionEmptyState(
                    title = "No Crops Found",
                    message = "No plants match the current filter criteria.",
                    actionText = "Show All Crops",
                    onActionClick = { selectedFilter = "All" }
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(FarmVisionSpacing.medium)
                ) {
                    filteredPlants.forEach { plant ->
                        CropDetailCard(
                            plant = plant,
                            onWaterClick = {
                                viewModel.togglePlantIrrigation(plant.id)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
        }
    }
}

@Composable
private fun CropDetailCard(
    plant: PlantItem,
    onWaterClick: () -> Unit
) {
    FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plant.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = plant.variety,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = plant.plotLocation,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            FarmVisionStatusBadge(
                status = plant.healthStatus,
                customLabel = if (plant.isWatered) "IRRIGATED (${plant.healthPercent}%)" else "${plant.healthPercent}%"
            )
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        // Conditions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
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
                    text = "Moisture: ${plant.currentMoisture}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Harvest in ${plant.daysToHarvest}d",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

        // Recommendation
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (plant.isWatered) "Hydration logged. Rootzone absorption active." else plant.wateringRecommendation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        // Action Button
        FarmVisionPrimaryButton(
            text = if (plant.isWatered) "Irrigation Active (Tap to Reset)" else "Simulate Irrigation",
            onClick = onWaterClick,
            leadingIcon = if (plant.isWatered) Icons.Rounded.Check else Icons.Rounded.WaterDrop,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
