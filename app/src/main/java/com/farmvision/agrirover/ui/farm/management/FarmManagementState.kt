package com.farmvision.agrirover.ui.farm.management

import com.farmvision.agrirover.domain.model.farm.AreaUnit
import com.farmvision.agrirover.domain.model.farm.DeletionReport
import com.farmvision.agrirover.domain.model.farm.Farm
import com.farmvision.agrirover.domain.model.farm.GrowthStage
import com.farmvision.agrirover.domain.model.farm.PlantHealthStatus
import com.farmvision.agrirover.domain.model.farm.Planting
import com.farmvision.agrirover.domain.model.farm.Plot
import java.time.LocalDate

/**
 * Internal sub-navigation destination routes within the My Farm screen.
 */
sealed interface MyFarmSubRoute {
    data object FarmListOverview : MyFarmSubRoute
    data class FarmDetail(val farmId: String) : MyFarmSubRoute
    data class PlotDetail(val farmId: String, val plotId: String) : MyFarmSubRoute
    data class PlantingDetail(val farmId: String, val plotId: String, val plantingId: String) : MyFarmSubRoute
    data class AddEditFarm(val farmId: String? = null) : MyFarmSubRoute
    data class AddEditPlot(val farmId: String, val plotId: String? = null) : MyFarmSubRoute
    data class AddEditPlanting(val farmId: String, val plotId: String, val plantingId: String? = null) : MyFarmSubRoute
}

/**
 * Sort options for agricultural crop catalogs and lists.
 */
enum class FarmSortOption(val label: String) {
    NameAsc("Name (A to Z)"),
    NameDesc("Name (Z to A)"),
    PlantingDateDesc("Planting Date (Newest first)"),
    PlantingDateAsc("Planting Date (Oldest first)"),
    RecentlyUpdated("Recently Updated")
}

/**
 * Filter facets applied across farm lists and plantings.
 */
data class FarmFilterState(
    val searchQuery: String = "",
    val selectedFarmId: String? = null,
    val selectedGrowthStage: GrowthStage? = null,
    val selectedHealthStatus: PlantHealthStatus? = null,
    val sortOption: FarmSortOption = FarmSortOption.NameAsc
) {
    val isFilterActive: Boolean
        get() = searchQuery.isNotBlank() || selectedFarmId != null || selectedGrowthStage != null || selectedHealthStatus != null
}

/**
 * Target entity marked for deletion confirmation.
 */
sealed interface DeletionTarget {
    data class FarmTarget(val farm: Farm) : DeletionTarget
    data class PlotTarget(val plot: Plot, val farmName: String) : DeletionTarget
    data class PlantingTarget(val planting: Planting, val plotName: String) : DeletionTarget
}

/**
 * Draft state for Farm add/edit form.
 */
data class FarmFormDraft(
    val farmId: String? = null,
    val name: String = "",
    val description: String = "",
    val locationDescription: String = "",
    val areaInput: String = "",
    val areaUnit: AreaUnit = AreaUnit.SquareMeters,
    val notes: String = "",
    val nameError: String? = null,
    val areaError: String? = null
) {
    val hasUnsavedChanges: Boolean
        get() = name.isNotBlank() || description.isNotBlank() || locationDescription.isNotBlank() || areaInput.isNotBlank() || notes.isNotBlank()
}

/**
 * Draft state for Plot add/edit form.
 */
data class PlotFormDraft(
    val plotId: String? = null,
    val farmId: String = "",
    val name: String = "",
    val description: String = "",
    val areaInput: String = "",
    val areaUnit: AreaUnit = AreaUnit.SquareMeters,
    val notes: String = "",
    val nameError: String? = null,
    val areaError: String? = null
) {
    val hasUnsavedChanges: Boolean
        get() = name.isNotBlank() || description.isNotBlank() || areaInput.isNotBlank() || notes.isNotBlank()
}

/**
 * Draft state for Planting add/edit form.
 */
data class PlantingFormDraft(
    val plantingId: String? = null,
    val farmId: String = "",
    val plotId: String = "",
    val cropName: String = "",
    val variety: String = "",
    val plantingDate: LocalDate? = null,
    val growthStage: GrowthStage = GrowthStage.Unspecified,
    val userNotes: String = "",
    val userHealthStatus: PlantHealthStatus = PlantHealthStatus.InsufficientData,
    val daysToHarvestInput: String = "",
    val nameError: String? = null,
    val varietyError: String? = null,
    val notesError: String? = null
) {
    val hasUnsavedChanges: Boolean
        get() = cropName.isNotBlank() || variety.isNotBlank() || plantingDate != null || growthStage != GrowthStage.Unspecified || userNotes.isNotBlank()
}

/**
 * Combined view item representing a planting joined with its parent plot and farm metadata.
 */
data class PlantingViewItem(
    val planting: Planting,
    val plotName: String,
    val farmName: String,
    val farmId: String,
    val isWatered: Boolean = false,
    val currentMoisture: Float = 42.0f,
    val healthPercent: Int = 90
)
