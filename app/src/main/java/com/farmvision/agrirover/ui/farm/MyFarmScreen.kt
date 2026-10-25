package com.farmvision.agrirover.ui.farm

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.ui.farm.components.DeleteConfirmationDialog
import com.farmvision.agrirover.ui.farm.components.DiscardChangesDialog
import com.farmvision.agrirover.ui.farm.management.FarmManagementViewModel
import com.farmvision.agrirover.ui.farm.management.MyFarmSubRoute
import com.farmvision.agrirover.ui.farm.screens.AddEditFarmScreen
import com.farmvision.agrirover.ui.farm.screens.AddEditPlantingScreen
import com.farmvision.agrirover.ui.farm.screens.AddEditPlotScreen
import com.farmvision.agrirover.ui.farm.screens.FarmDetailScreen
import com.farmvision.agrirover.ui.farm.screens.FarmOverviewScreen
import com.farmvision.agrirover.ui.farm.screens.PlantingDetailScreen
import com.farmvision.agrirover.ui.farm.screens.PlotDetailScreen
import com.farmvision.agrirover.ui.viewmodel.FarmViewModel

/**
 * My Farm screen managing the Farm -> Plot -> Planting hierarchy,
 * localized sub-route navigation stack, cascading deletion confirmations,
 * form draft state tracking, and simulation hydration controls.
 */
@Composable
fun MyFarmScreen(
    viewModel: FarmViewModel,
    modifier: Modifier = Modifier,
    managementViewModel: FarmManagementViewModel = remember(viewModel) {
        FarmManagementViewModel(repository = viewModel.farmManagement)
    }
) {
    val scrollState = rememberScrollState()

    val currentRoute by managementViewModel.currentRoute.collectAsState()
    val filterState by managementViewModel.filterState.collectAsState()
    val farms by managementViewModel.farms.collectAsState()
    val plots by managementViewModel.plots.collectAsState()
    val plantings by managementViewModel.plantings.collectAsState()
    val filteredPlantings by managementViewModel.filteredPlantings.collectAsState()
    val availableGrowthStages by managementViewModel.availableGrowthStages.collectAsState()
    val availableHealthStatuses by managementViewModel.availableHealthStatuses.collectAsState()

    val farmDraft by managementViewModel.farmDraft.collectAsState()
    val plotDraft by managementViewModel.plotDraft.collectAsState()
    val plantingDraft by managementViewModel.plantingDraft.collectAsState()
    val isSubmitting by managementViewModel.isSubmitting.collectAsState()

    val activeDeletionTarget by managementViewModel.activeDeletionTarget.collectAsState()
    val activeDeletionReport by managementViewModel.activeDeletionReport.collectAsState()
    val showDiscardPrompt by managementViewModel.showDiscardPrompt.collectAsState()

    // Extended BackHandler: intercepts back presses when in sub-routes or dialogs
    val canNavigateBack = currentRoute != MyFarmSubRoute.FarmListOverview ||
        activeDeletionTarget != null ||
        showDiscardPrompt

    BackHandler(enabled = canNavigateBack) {
        managementViewModel.navigateUp()
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
            when (val route = currentRoute) {
                is MyFarmSubRoute.FarmListOverview -> {
                    FarmOverviewScreen(
                        farms = farms,
                        plots = plots,
                        plantings = filteredPlantings,
                        filterState = filterState,
                        availableGrowthStages = availableGrowthStages,
                        availableHealthStatuses = availableHealthStatuses,
                        onSearchChange = { managementViewModel.setSearchQuery(it) },
                        onGrowthStageFilterChange = { managementViewModel.setSelectedGrowthStageFilter(it) },
                        onHealthStatusFilterChange = { managementViewModel.setSelectedHealthStatusFilter(it) },
                        onSortOptionChange = { managementViewModel.setSortOption(it) },
                        onClearFilters = { managementViewModel.clearAllFilters() },
                        onAddFarmClick = { managementViewModel.initFarmForm() },
                        onFarmClick = { farmId ->
                            managementViewModel.navigateTo(MyFarmSubRoute.FarmDetail(farmId))
                        },
                        onPlantingClick = { farmId, plotId, plantingId ->
                            managementViewModel.navigateTo(MyFarmSubRoute.PlantingDetail(farmId, plotId, plantingId))
                        },
                        onSimulateIrrigation = { plantingId ->
                            viewModel.togglePlantIrrigation(plantingId)
                        }
                    )
                }

                is MyFarmSubRoute.FarmDetail -> {
                    val farm = farms.find { it.id == route.farmId }
                    if (farm != null) {
                        val farmPlots = plots.filter { it.farmId == farm.id }
                        FarmDetailScreen(
                            farm = farm,
                            plots = farmPlots,
                            allPlantings = plantings,
                            onBackClick = { managementViewModel.navigateUp() },
                            onEditClick = { managementViewModel.initFarmForm(farm.id) },
                            onDeleteClick = { managementViewModel.requestDeleteFarm(farm) },
                            onAddPlotClick = { managementViewModel.initPlotForm(farm.id) },
                            onPlotClick = { plotId ->
                                managementViewModel.navigateTo(MyFarmSubRoute.PlotDetail(farm.id, plotId))
                            }
                        )
                    } else {
                        // Fallback if deleted
                        managementViewModel.navigateUp()
                    }
                }

                is MyFarmSubRoute.PlotDetail -> {
                    val farm = farms.find { it.id == route.farmId }
                    val plot = plots.find { it.id == route.plotId }
                    if (farm != null && plot != null) {
                        val plotPlantings = filteredPlantings.filter { it.planting.plotId == plot.id }
                        PlotDetailScreen(
                            plot = plot,
                            farm = farm,
                            plantings = plotPlantings,
                            onBackClick = { managementViewModel.navigateUp() },
                            onEditClick = { managementViewModel.initPlotForm(farm.id, plot.id) },
                            onDeleteClick = { managementViewModel.requestDeletePlot(plot, farm.name) },
                            onAddPlantingClick = { managementViewModel.initPlantingForm(farm.id, plot.id) },
                            onPlantingClick = { plantingId ->
                                managementViewModel.navigateTo(MyFarmSubRoute.PlantingDetail(farm.id, plot.id, plantingId))
                            },
                            onSimulateIrrigation = { plantingId ->
                                viewModel.togglePlantIrrigation(plantingId)
                            }
                        )
                    } else {
                        managementViewModel.navigateUp()
                    }
                }

                is MyFarmSubRoute.PlantingDetail -> {
                    val item = filteredPlantings.find { it.planting.id == route.plantingId }
                    if (item != null) {
                        PlantingDetailScreen(
                            item = item,
                            onBackClick = { managementViewModel.navigateUp() },
                            onEditClick = {
                                managementViewModel.initPlantingForm(route.farmId, route.plotId, route.plantingId)
                            },
                            onDeleteClick = {
                                managementViewModel.requestDeletePlanting(item.planting, item.plotName)
                            },
                            onSimulateIrrigation = {
                                viewModel.togglePlantIrrigation(item.planting.id)
                            }
                        )
                    } else {
                        managementViewModel.navigateUp()
                    }
                }

                is MyFarmSubRoute.AddEditFarm -> {
                    AddEditFarmScreen(
                        draft = farmDraft,
                        isSubmitting = isSubmitting,
                        onNameChange = { managementViewModel.updateFarmDraftName(it) },
                        onDescriptionChange = { managementViewModel.updateFarmDraftDescription(it) },
                        onLocationChange = { managementViewModel.updateFarmDraftLocation(it) },
                        onAreaChange = { area, unit -> managementViewModel.updateFarmDraftArea(area, unit) },
                        onNotesChange = { managementViewModel.updateFarmDraftNotes(it) },
                        onSubmit = { managementViewModel.submitFarmDraft() },
                        onCancel = { managementViewModel.navigateUp() }
                    )
                }

                is MyFarmSubRoute.AddEditPlot -> {
                    val farm = farms.find { it.id == route.farmId }
                    AddEditPlotScreen(
                        draft = plotDraft,
                        farmName = farm?.name ?: "Farm",
                        isSubmitting = isSubmitting,
                        onNameChange = { managementViewModel.updatePlotDraftName(it) },
                        onDescriptionChange = { managementViewModel.updatePlotDraftDescription(it) },
                        onAreaChange = { area, unit -> managementViewModel.updatePlotDraftArea(area, unit) },
                        onNotesChange = { managementViewModel.updatePlotDraftNotes(it) },
                        onSubmit = { managementViewModel.submitPlotDraft() },
                        onCancel = { managementViewModel.navigateUp() }
                    )
                }

                is MyFarmSubRoute.AddEditPlanting -> {
                    AddEditPlantingScreen(
                        draft = plantingDraft,
                        farms = farms,
                        plots = plots,
                        isSubmitting = isSubmitting,
                        onFarmChange = { managementViewModel.updatePlantingDraftFarm(it) },
                        onPlotChange = { managementViewModel.updatePlantingDraftPlot(it) },
                        onCropNameChange = { managementViewModel.updatePlantingDraftCropName(it) },
                        onVarietyChange = { managementViewModel.updatePlantingDraftVariety(it) },
                        onPlantingDateChange = { managementViewModel.updatePlantingDraftDate(it) },
                        onGrowthStageChange = { managementViewModel.updatePlantingDraftStage(it) },
                        onHealthStatusChange = { managementViewModel.updatePlantingDraftHealth(it) },
                        onDaysToHarvestChange = { managementViewModel.updatePlantingDraftDaysToHarvest(it) },
                        onNotesChange = { managementViewModel.updatePlantingDraftNotes(it) },
                        onSubmit = { managementViewModel.submitPlantingDraft() },
                        onCancel = { managementViewModel.navigateUp() }
                    )
                }
            }
        }
    }

    // Active Deletion Confirmation Dialog
    val target = activeDeletionTarget
    val report = activeDeletionReport
    if (target != null && report != null) {
        DeleteConfirmationDialog(
            target = target,
            report = report,
            onConfirm = { managementViewModel.confirmDeletion() },
            onDismiss = { managementViewModel.dismissDeletionDialog() }
        )
    }

    // Unsaved Changes Discard Confirmation Dialog
    if (showDiscardPrompt) {
        DiscardChangesDialog(
            onConfirmDiscard = { managementViewModel.confirmDiscardDraft() },
            onDismiss = { managementViewModel.dismissDiscardPrompt() }
        )
    }
}
