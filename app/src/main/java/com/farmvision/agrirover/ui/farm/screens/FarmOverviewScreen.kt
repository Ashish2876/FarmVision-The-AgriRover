package com.farmvision.agrirover.ui.farm.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.farmvision.agrirover.core.designsystem.component.FarmVisionOutlinedButton
import com.farmvision.agrirover.core.designsystem.component.FarmVisionPrimaryButton
import com.farmvision.agrirover.core.designsystem.component.FarmVisionSectionHeader
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusBadge
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.domain.model.farm.AreaUnit
import com.farmvision.agrirover.domain.model.farm.Farm
import com.farmvision.agrirover.domain.model.farm.GrowthStage
import com.farmvision.agrirover.domain.model.farm.PlantHealthStatus
import com.farmvision.agrirover.domain.model.farm.Plot
import com.farmvision.agrirover.domain.model.farm.RecordOrigin
import com.farmvision.agrirover.ui.farm.components.FarmVisionTextField
import com.farmvision.agrirover.ui.farm.management.FarmFilterState
import com.farmvision.agrirover.ui.farm.management.FarmSortOption
import com.farmvision.agrirover.ui.farm.management.PlantingViewItem
import java.time.LocalDate

/**
 * Root overview screen for My Farm destination.
 * Displays Farm parcels, plot summaries, search & facet filters, sort options,
 * and the unified crop planting catalog with simulated irrigation triggers.
 */
@Composable
fun FarmOverviewScreen(
    farms: List<Farm>,
    plots: List<Plot>,
    plantings: List<PlantingViewItem>,
    filterState: FarmFilterState,
    availableGrowthStages: List<GrowthStage>,
    availableHealthStatuses: List<PlantHealthStatus>,
    onSearchChange: (String) -> Unit,
    onGrowthStageFilterChange: (GrowthStage?) -> Unit,
    onHealthStatusFilterChange: (PlantHealthStatus?) -> Unit,
    onSortOptionChange: (FarmSortOption) -> Unit,
    onClearFilters: () -> Unit,
    onAddFarmClick: () -> Unit,
    onFarmClick: (String) -> Unit,
    onPlantingClick: (String, String, String) -> Unit,
    onSimulateIrrigation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSortMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // Top Header
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
                    text = "Farms • Plots • Crop Catalog • Irrigation",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            FarmVisionStatusBadge(status = FarmVisionStatusType.DemoData)
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        FarmVisionDemoBanner(
            message = "DEMO DATA: Farm parcels, soil hydration levels, and watering recommendations are simulated."
        )

        Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

        // Summary Metric Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
        ) {
            SummaryMetricBox(
                label = "Farms",
                value = farms.size.toString(),
                modifier = Modifier.weight(1f)
            )
            SummaryMetricBox(
                label = "Plots",
                value = plots.size.toString(),
                modifier = Modifier.weight(1f)
            )
            SummaryMetricBox(
                label = "Plantings",
                value = plantings.size.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

        // Farms Section Header & Add Farm Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "FARM PARCELS (${farms.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FarmVisionOutlinedButton(
                text = "Add Farm",
                onClick = onAddFarmClick,
                leadingIcon = Icons.Rounded.Add
            )
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

        // Farm Cards
        if (farms.isEmpty()) {
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No farm parcels registered yet. Tap \"Add Farm\" to define your first agricultural field.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
            ) {
                farms.forEach { farm ->
                    val farmPlots = plots.filter { it.farmId == farm.id }
                    FarmOverviewCard(
                        farm = farm,
                        plotCount = farmPlots.size,
                        onClick = { onFarmClick(farm.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

        // Crops Section Header & Sorting Trigger
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "CROP CATALOG & FIELD PLANTINGS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box {
                Surface(
                    onClick = { isSortMenuOpen = true },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = "Sort crops",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = filterState.sortOption.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DropdownMenu(
                    expanded = isSortMenuOpen,
                    onDismissRequest = { isSortMenuOpen = false }
                ) {
                    FarmSortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(text = option.label) },
                            onClick = {
                                onSortOptionChange(option)
                                isSortMenuOpen = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

        // Search Bar
        FarmVisionTextField(
            value = filterState.searchQuery,
            onValueChange = onSearchChange,
            label = "Search crops, varieties, plots, or farms",
            placeholder = "e.g. Roma, Sweet Corn, Sector A",
            leadingIcon = Icons.Rounded.Search
        )

        Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

        // Horizontal Facet Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
        ) {
            // All Chip
            FilterChip(
                selected = !filterState.isFilterActive,
                onClick = onClearFilters,
                label = { Text("All Plantings") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            // Dynamic Growth Stage Chips
            availableGrowthStages.forEach { stage ->
                val isSelected = filterState.selectedGrowthStage == stage
                FilterChip(
                    selected = isSelected,
                    onClick = { onGrowthStageFilterChange(if (isSelected) null else stage) },
                    label = { Text(stage.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            // Dynamic Health Status Chips
            availableHealthStatuses.forEach { health ->
                val isSelected = filterState.selectedHealthStatus == health
                FilterChip(
                    selected = isSelected,
                    onClick = { onHealthStatusFilterChange(if (isSelected) null else health) },
                    label = { Text(health.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        // Filtered Plantings List
        if (plantings.isEmpty()) {
            FarmVisionEmptyState(
                title = "No Crops Found",
                message = if (filterState.isFilterActive) "No plants match the active search or filter criteria." else "No crop plantings exist in any plot.",
                actionText = if (filterState.isFilterActive) "Clear Filters" else null,
                onActionClick = if (filterState.isFilterActive) onClearFilters else null
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FarmVisionSpacing.medium)
            ) {
                plantings.forEach { item ->
                    PlantingCard(
                        item = item,
                        onClick = { onPlantingClick(item.farmId, item.planting.plotId, item.planting.id) },
                        onSimulateIrrigation = { onSimulateIrrigation(item.planting.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
    }
}

@Composable
private fun SummaryMetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FarmOverviewCard(
    farm: Farm,
    plotCount: Int,
    onClick: () -> Unit
) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = farm.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (farm.origin == RecordOrigin.SAMPLE) {
                        Spacer(modifier = Modifier.width(6.dp))
                        FarmVisionStatusBadge(status = FarmVisionStatusType.DemoData, customLabel = "SAMPLE")
                    }
                }

                if (farm.locationDescription != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = farm.locationDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "$plotCount Plot${if (plotCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        if (farm.areaSquareMeters != null) {
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            val ha = AreaUnit.Hectares.fromSquareMeters(farm.areaSquareMeters)
            Text(
                text = "Total Area: ${String.format(java.util.Locale.US, "%.2f", ha)} ha (${String.format(java.util.Locale.US, "%,.0f", farm.areaSquareMeters)} m²)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlantingCard(
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
                Text(
                    text = "${item.farmName} • ${item.plotName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
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
