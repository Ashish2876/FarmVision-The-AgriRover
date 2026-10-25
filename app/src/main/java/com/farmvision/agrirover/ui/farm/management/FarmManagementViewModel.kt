package com.farmvision.agrirover.ui.farm.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmvision.agrirover.domain.model.farm.AreaUnit
import com.farmvision.agrirover.domain.model.farm.DeletionReport
import com.farmvision.agrirover.domain.model.farm.Farm
import com.farmvision.agrirover.domain.model.farm.FarmManagementError
import com.farmvision.agrirover.domain.model.farm.FarmOperationResult
import com.farmvision.agrirover.domain.model.farm.GrowthStage
import com.farmvision.agrirover.domain.model.farm.PlantHealthStatus
import com.farmvision.agrirover.domain.model.farm.Planting
import com.farmvision.agrirover.domain.model.farm.Plot
import com.farmvision.agrirover.domain.repository.FarmManagementRepository
import com.farmvision.agrirover.domain.validation.FarmValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Production ViewModel managing agricultural CRUD workflows, multi-facet filtering,
 * derived search state, form draft preservation across configuration changes,
 * double-submission prevention, and atomic cascading deletion confirmation dialogs.
 */
class FarmManagementViewModel(
    private val repository: FarmManagementRepository,
    coroutineScope: kotlinx.coroutines.CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    // --- Navigation Sub-Route Stack inside My Farm ---
    private val _routeStack = MutableStateFlow<List<MyFarmSubRoute>>(listOf(MyFarmSubRoute.FarmListOverview))
    private val _currentRoute = MutableStateFlow<MyFarmSubRoute>(MyFarmSubRoute.FarmListOverview)
    val currentRoute: StateFlow<MyFarmSubRoute> = _currentRoute.asStateFlow()

    // --- Search, Filter & Sort State ---
    private val _filterState = MutableStateFlow(FarmFilterState())
    val filterState: StateFlow<FarmFilterState> = _filterState.asStateFlow()

    // --- Form Drafts ---
    private val _farmDraft = MutableStateFlow(FarmFormDraft())
    val farmDraft: StateFlow<FarmFormDraft> = _farmDraft.asStateFlow()

    private val _plotDraft = MutableStateFlow(PlotFormDraft())
    val plotDraft: StateFlow<PlotFormDraft> = _plotDraft.asStateFlow()

    private val _plantingDraft = MutableStateFlow(PlantingFormDraft())
    val plantingDraft: StateFlow<PlantingFormDraft> = _plantingDraft.asStateFlow()

    // --- Submission & Deletion State ---
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _activeDeletionTarget = MutableStateFlow<DeletionTarget?>(null)
    val activeDeletionTarget: StateFlow<DeletionTarget?> = _activeDeletionTarget.asStateFlow()

    private val _activeDeletionReport = MutableStateFlow<DeletionReport?>(null)
    val activeDeletionReport: StateFlow<DeletionReport?> = _activeDeletionReport.asStateFlow()

    private val _showDiscardPrompt = MutableStateFlow(false)
    val showDiscardPrompt: StateFlow<Boolean> = _showDiscardPrompt.asStateFlow()

    // --- Raw Repository Streams ---
    val farms: StateFlow<List<Farm>> = repository.farmsFlow
    val plots: StateFlow<List<Plot>> = repository.plotsFlow
    val plantings: StateFlow<List<Planting>> = repository.plantingsFlow

    // --- Derived Filtered Plantings Stream ---
    val filteredPlantings: StateFlow<List<PlantingViewItem>> = combine(
        repository.plantingsFlow,
        repository.plotsFlow,
        repository.farmsFlow,
        _filterState
    ) { allPlantings, allPlots, allFarms, filter ->
        val plotMap = allPlots.associateBy { it.id }
        val farmMap = allFarms.associateBy { it.id }

        // Join plantings with plot and farm
        val viewItems = allPlantings.map { planting ->
            val plot = plotMap[planting.plotId]
            val farm = plot?.let { farmMap[it.farmId] }
            PlantingViewItem(
                planting = planting,
                plotName = plot?.name ?: "Unknown Plot",
                farmName = farm?.name ?: "Unknown Farm",
                farmId = farm?.id ?: ""
            )
        }

        // Apply Search
        val query = filter.searchQuery.trim().lowercase()
        val searchFiltered = if (query.isEmpty()) {
            viewItems
        } else {
            viewItems.filter { item ->
                item.planting.cropName.lowercase().contains(query) ||
                    (item.planting.variety?.lowercase()?.contains(query) == true) ||
                    item.plotName.lowercase().contains(query) ||
                    item.farmName.lowercase().contains(query)
            }
        }

        // Apply Facet Filters
        val facetFiltered = searchFiltered.filter { item ->
            val matchesFarm = filter.selectedFarmId == null || item.farmId == filter.selectedFarmId
            val matchesStage = filter.selectedGrowthStage == null || item.planting.growthStage == filter.selectedGrowthStage
            val matchesHealth = filter.selectedHealthStatus == null || item.planting.userHealthStatus == filter.selectedHealthStatus
            matchesFarm && matchesStage && matchesHealth
        }

        // Apply Sorting (Unknown dates sort last; deterministic tie-breaker on ID)
        when (filter.sortOption) {
            FarmSortOption.NameAsc -> facetFiltered.sortedWith(
                compareBy<PlantingViewItem> { it.planting.cropName.lowercase() }
                    .thenBy { it.planting.id }
            )
            FarmSortOption.NameDesc -> facetFiltered.sortedWith(
                compareByDescending<PlantingViewItem> { it.planting.cropName.lowercase() }
                    .thenBy { it.planting.id }
            )
            FarmSortOption.PlantingDateDesc -> facetFiltered.sortedWith { a, b ->
                val dateA = a.planting.plantingDate
                val dateB = b.planting.plantingDate
                when {
                    dateA == null && dateB == null -> a.planting.id.compareTo(b.planting.id)
                    dateA == null -> 1 // Unknown dates sort last
                    dateB == null -> -1
                    else -> {
                        val cmp = dateB.compareTo(dateA) // Newest first
                        if (cmp != 0) cmp else a.planting.id.compareTo(b.planting.id)
                    }
                }
            }
            FarmSortOption.PlantingDateAsc -> facetFiltered.sortedWith { a, b ->
                val dateA = a.planting.plantingDate
                val dateB = b.planting.plantingDate
                when {
                    dateA == null && dateB == null -> a.planting.id.compareTo(b.planting.id)
                    dateA == null -> 1 // Unknown dates sort last
                    dateB == null -> -1
                    else -> {
                        val cmp = dateA.compareTo(dateB) // Oldest first
                        if (cmp != 0) cmp else a.planting.id.compareTo(b.planting.id)
                    }
                }
            }
            FarmSortOption.RecentlyUpdated -> facetFiltered.sortedWith(
                compareByDescending<PlantingViewItem> { it.planting.updatedAtEpochMillis }
                    .thenBy { it.planting.id }
            )
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    // --- Dynamic Available Filter Options (Only what exists in data) ---
    val availableGrowthStages: StateFlow<List<GrowthStage>> = repository.plantingsFlow.combine(MutableStateFlow(Unit)) { plantings, _ ->
        plantings.map { it.growthStage }.distinct().sortedBy { it.ordinal }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val availableHealthStatuses: StateFlow<List<PlantHealthStatus>> = repository.plantingsFlow.combine(MutableStateFlow(Unit)) { plantings, _ ->
        plantings.map { it.userHealthStatus }
            .filter { it != PlantHealthStatus.InsufficientData }
            .distinct()
            .sortedBy { it.ordinal }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    // --- Navigation Operations ---

    fun navigateTo(route: MyFarmSubRoute) {
        val nextStack = _routeStack.value + route
        _routeStack.value = nextStack
        _currentRoute.value = route
    }

    fun navigateUp(): Boolean {
        // If a deletion dialog is open, close it first
        if (_activeDeletionTarget.value != null) {
            dismissDeletionDialog()
            return true
        }

        // If in a form with unsaved changes, show confirmation prompt
        if (isCurrentRouteInForm() && hasActiveDraftUnsavedChanges()) {
            _showDiscardPrompt.value = true
            return true
        }

        // Pop sub-route
        if (_routeStack.value.size > 1) {
            val nextStack = _routeStack.value.dropLast(1)
            _routeStack.value = nextStack
            _currentRoute.value = nextStack.last()
            resetDrafts()
            return true
        }

        return false // At root of My Farm
    }

    fun confirmDiscardDraft() {
        _showDiscardPrompt.value = false
        resetDrafts()
        if (_routeStack.value.size > 1) {
            val nextStack = _routeStack.value.dropLast(1)
            _routeStack.value = nextStack
            _currentRoute.value = nextStack.last()
        }
    }

    fun dismissDiscardPrompt() {
        _showDiscardPrompt.value = false
    }

    private fun isCurrentRouteInForm(): Boolean {
        return when (currentRoute.value) {
            is MyFarmSubRoute.AddEditFarm,
            is MyFarmSubRoute.AddEditPlot,
            is MyFarmSubRoute.AddEditPlanting -> true
            else -> false
        }
    }

    private fun hasActiveDraftUnsavedChanges(): Boolean {
        return when (currentRoute.value) {
            is MyFarmSubRoute.AddEditFarm -> _farmDraft.value.hasUnsavedChanges
            is MyFarmSubRoute.AddEditPlot -> _plotDraft.value.hasUnsavedChanges
            is MyFarmSubRoute.AddEditPlanting -> _plantingDraft.value.hasUnsavedChanges
            else -> false
        }
    }

    // --- Filter & Search Operations ---

    fun setSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun setSelectedFarmFilter(farmId: String?) {
        _filterState.value = _filterState.value.copy(selectedFarmId = farmId)
    }

    fun setSelectedGrowthStageFilter(stage: GrowthStage?) {
        _filterState.value = _filterState.value.copy(selectedGrowthStage = stage)
    }

    fun setSelectedHealthStatusFilter(status: PlantHealthStatus?) {
        _filterState.value = _filterState.value.copy(selectedHealthStatus = status)
    }

    fun setSortOption(option: FarmSortOption) {
        _filterState.value = _filterState.value.copy(sortOption = option)
    }

    fun clearAllFilters() {
        _filterState.value = FarmFilterState()
    }

    fun clearSearchQuery() {
        _filterState.value = _filterState.value.copy(searchQuery = "")
    }

    // --- Farm Form Operations ---

    fun initFarmForm(farmId: String? = null) {
        if (farmId == null) {
            _farmDraft.value = FarmFormDraft()
        } else {
            val farm = repository.getFarm(farmId)
            if (farm != null) {
                _farmDraft.value = FarmFormDraft(
                    farmId = farm.id,
                    name = farm.name,
                    description = farm.description ?: "",
                    locationDescription = farm.locationDescription ?: "",
                    areaInput = farm.areaSquareMeters?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "",
                    areaUnit = AreaUnit.SquareMeters,
                    notes = farm.notes ?: ""
                )
            }
        }
        navigateTo(MyFarmSubRoute.AddEditFarm(farmId))
    }

    fun updateFarmDraftName(name: String) {
        _farmDraft.value = _farmDraft.value.copy(name = name, nameError = null)
    }

    fun updateFarmDraftDescription(description: String) {
        _farmDraft.value = _farmDraft.value.copy(description = description)
    }

    fun updateFarmDraftLocation(location: String) {
        _farmDraft.value = _farmDraft.value.copy(locationDescription = location)
    }

    fun updateFarmDraftArea(input: String, unit: AreaUnit = _farmDraft.value.areaUnit) {
        _farmDraft.value = _farmDraft.value.copy(areaInput = input, areaUnit = unit, areaError = null)
    }

    fun updateFarmDraftNotes(notes: String) {
        _farmDraft.value = _farmDraft.value.copy(notes = notes)
    }

    fun submitFarmDraft(onSuccess: (Farm) -> Unit = {}) {
        if (_isSubmitting.value) return
        val draft = _farmDraft.value

        val nameErr = FarmValidator.validateName("Farm Name", draft.name)
        if (nameErr != null) {
            _farmDraft.value = draft.copy(nameError = mapErrorToMessage(nameErr))
            return
        }

        val areaM2 = if (draft.areaInput.isNotBlank()) {
            val raw = draft.areaInput.toDoubleOrNull()
            if (raw == null || raw <= 0.0) {
                _farmDraft.value = draft.copy(areaError = "Enter a valid positive number")
                return
            }
            draft.areaUnit.toSquareMeters(raw)
        } else {
            null
        }

        val areaErr = FarmValidator.validateAreaSquareMeters(areaM2)
        if (areaErr != null) {
            _farmDraft.value = draft.copy(areaError = mapErrorToMessage(areaErr))
            return
        }

        _isSubmitting.value = true
        val result = if (draft.farmId == null) {
            repository.createFarm(
                name = draft.name,
                description = draft.description.ifBlank { null },
                locationDescription = draft.locationDescription.ifBlank { null },
                areaSquareMeters = areaM2,
                notes = draft.notes.ifBlank { null }
            )
        } else {
            repository.updateFarm(
                id = draft.farmId,
                name = draft.name,
                description = draft.description.ifBlank { null },
                locationDescription = draft.locationDescription.ifBlank { null },
                areaSquareMeters = areaM2,
                notes = draft.notes.ifBlank { null }
            )
        }
        _isSubmitting.value = false

        when (result) {
            is FarmOperationResult.Success -> {
                _farmDraft.value = FarmFormDraft()
                navigateUp()
                onSuccess(result.data)
            }
            is FarmOperationResult.Failure -> {
                when (val err = result.error) {
                    is FarmManagementError.DuplicateName -> _farmDraft.value = draft.copy(nameError = "A farm with this name already exists")
                    is FarmManagementError.InvalidArea -> _farmDraft.value = draft.copy(areaError = err.reason)
                    else -> _farmDraft.value = draft.copy(nameError = mapErrorToMessage(err))
                }
            }
        }
    }

    // --- Plot Form Operations ---

    fun initPlotForm(farmId: String, plotId: String? = null) {
        if (plotId == null) {
            _plotDraft.value = PlotFormDraft(farmId = farmId)
        } else {
            val plot = repository.getPlot(plotId)
            if (plot != null) {
                _plotDraft.value = PlotFormDraft(
                    plotId = plot.id,
                    farmId = plot.farmId,
                    name = plot.name,
                    description = plot.description ?: "",
                    areaInput = plot.areaSquareMeters?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "",
                    areaUnit = AreaUnit.SquareMeters,
                    notes = plot.notes ?: ""
                )
            }
        }
        navigateTo(MyFarmSubRoute.AddEditPlot(farmId, plotId))
    }

    fun updatePlotDraftName(name: String) {
        _plotDraft.value = _plotDraft.value.copy(name = name, nameError = null)
    }

    fun updatePlotDraftDescription(description: String) {
        _plotDraft.value = _plotDraft.value.copy(description = description)
    }

    fun updatePlotDraftArea(input: String, unit: AreaUnit = _plotDraft.value.areaUnit) {
        _plotDraft.value = _plotDraft.value.copy(areaInput = input, areaUnit = unit, areaError = null)
    }

    fun updatePlotDraftNotes(notes: String) {
        _plotDraft.value = _plotDraft.value.copy(notes = notes)
    }

    fun submitPlotDraft(onSuccess: (Plot) -> Unit = {}) {
        if (_isSubmitting.value) return
        val draft = _plotDraft.value

        val nameErr = FarmValidator.validateName("Plot Name", draft.name)
        if (nameErr != null) {
            _plotDraft.value = draft.copy(nameError = mapErrorToMessage(nameErr))
            return
        }

        val areaM2 = if (draft.areaInput.isNotBlank()) {
            val raw = draft.areaInput.toDoubleOrNull()
            if (raw == null || raw <= 0.0) {
                _plotDraft.value = draft.copy(areaError = "Enter a valid positive number")
                return
            }
            draft.areaUnit.toSquareMeters(raw)
        } else {
            null
        }

        val areaErr = FarmValidator.validateAreaSquareMeters(areaM2)
        if (areaErr != null) {
            _plotDraft.value = draft.copy(areaError = mapErrorToMessage(areaErr))
            return
        }

        _isSubmitting.value = true
        val result = if (draft.plotId == null) {
            repository.createPlot(
                farmId = draft.farmId,
                name = draft.name,
                description = draft.description.ifBlank { null },
                areaSquareMeters = areaM2,
                notes = draft.notes.ifBlank { null }
            )
        } else {
            repository.updatePlot(
                id = draft.plotId,
                name = draft.name,
                description = draft.description.ifBlank { null },
                areaSquareMeters = areaM2,
                notes = draft.notes.ifBlank { null }
            )
        }
        _isSubmitting.value = false

        when (result) {
            is FarmOperationResult.Success -> {
                _plotDraft.value = PlotFormDraft()
                navigateUp()
                onSuccess(result.data)
            }
            is FarmOperationResult.Failure -> {
                when (val err = result.error) {
                    is FarmManagementError.DuplicateName -> _plotDraft.value = draft.copy(nameError = "A plot with this name already exists in this farm")
                    is FarmManagementError.InvalidArea -> _plotDraft.value = draft.copy(areaError = err.reason)
                    else -> _plotDraft.value = draft.copy(nameError = mapErrorToMessage(err))
                }
            }
        }
    }

    // --- Planting Form Operations ---

    fun initPlantingForm(farmId: String, plotId: String, plantingId: String? = null) {
        if (plantingId == null) {
            _plantingDraft.value = PlantingFormDraft(farmId = farmId, plotId = plotId)
        } else {
            val planting = repository.getPlanting(plantingId)
            if (planting != null) {
                _plantingDraft.value = PlantingFormDraft(
                    plantingId = planting.id,
                    farmId = farmId,
                    plotId = planting.plotId,
                    cropName = planting.cropName,
                    variety = planting.variety ?: "",
                    plantingDate = planting.plantingDate,
                    growthStage = planting.growthStage,
                    userNotes = planting.userNotes ?: "",
                    userHealthStatus = planting.userHealthStatus,
                    daysToHarvestInput = planting.daysToHarvest?.toString() ?: ""
                )
            }
        }
        navigateTo(MyFarmSubRoute.AddEditPlanting(farmId, plotId, plantingId))
    }

    fun updatePlantingDraftFarm(newFarmId: String) {
        val currentDraft = _plantingDraft.value
        val validPlots = repository.getPlots(newFarmId)
        val resetPlotId = if (validPlots.any { it.id == currentDraft.plotId }) currentDraft.plotId else (validPlots.firstOrNull()?.id ?: "")
        _plantingDraft.value = currentDraft.copy(farmId = newFarmId, plotId = resetPlotId)
    }

    fun updatePlantingDraftPlot(newPlotId: String) {
        _plantingDraft.value = _plantingDraft.value.copy(plotId = newPlotId)
    }

    fun updatePlantingDraftCropName(name: String) {
        _plantingDraft.value = _plantingDraft.value.copy(cropName = name, nameError = null)
    }

    fun updatePlantingDraftVariety(variety: String) {
        _plantingDraft.value = _plantingDraft.value.copy(variety = variety, varietyError = null)
    }

    fun updatePlantingDraftDate(date: LocalDate?) {
        _plantingDraft.value = _plantingDraft.value.copy(plantingDate = date)
    }

    fun updatePlantingDraftStage(stage: GrowthStage) {
        _plantingDraft.value = _plantingDraft.value.copy(growthStage = stage)
    }

    fun updatePlantingDraftHealth(health: PlantHealthStatus) {
        _plantingDraft.value = _plantingDraft.value.copy(userHealthStatus = health)
    }

    fun updatePlantingDraftNotes(notes: String) {
        _plantingDraft.value = _plantingDraft.value.copy(userNotes = notes, notesError = null)
    }

    fun updatePlantingDraftDaysToHarvest(days: String) {
        _plantingDraft.value = _plantingDraft.value.copy(daysToHarvestInput = days)
    }

    fun submitPlantingDraft(onSuccess: (Planting) -> Unit = {}) {
        if (_isSubmitting.value) return
        val draft = _plantingDraft.value

        val nameErr = FarmValidator.validateName("Crop Name", draft.cropName)
        if (nameErr != null) {
            _plantingDraft.value = draft.copy(nameError = mapErrorToMessage(nameErr))
            return
        }

        val varietyErr = FarmValidator.validateVariety(draft.variety.ifBlank { null })
        if (varietyErr != null) {
            _plantingDraft.value = draft.copy(varietyError = mapErrorToMessage(varietyErr))
            return
        }

        val notesErr = FarmValidator.validateNotes(draft.userNotes.ifBlank { null })
        if (notesErr != null) {
            _plantingDraft.value = draft.copy(notesError = mapErrorToMessage(notesErr))
            return
        }

        val daysInt = draft.daysToHarvestInput.toIntOrNull()

        _isSubmitting.value = true
        val result = if (draft.plantingId == null) {
            repository.createPlanting(
                plotId = draft.plotId,
                cropName = draft.cropName,
                variety = draft.variety.ifBlank { null },
                plantingDate = draft.plantingDate,
                growthStage = draft.growthStage,
                userNotes = draft.userNotes.ifBlank { null },
                userHealthStatus = draft.userHealthStatus,
                daysToHarvest = daysInt
            )
        } else {
            repository.updatePlanting(
                id = draft.plantingId,
                cropName = draft.cropName,
                variety = draft.variety.ifBlank { null },
                plantingDate = draft.plantingDate,
                growthStage = draft.growthStage,
                userNotes = draft.userNotes.ifBlank { null },
                userHealthStatus = draft.userHealthStatus,
                daysToHarvest = daysInt,
                targetPlotId = draft.plotId
            )
        }
        _isSubmitting.value = false

        when (result) {
            is FarmOperationResult.Success -> {
                _plantingDraft.value = PlantingFormDraft()
                navigateUp()
                onSuccess(result.data)
            }
            is FarmOperationResult.Failure -> {
                _plantingDraft.value = draft.copy(nameError = mapErrorToMessage(result.error))
            }
        }
    }

    // --- Deletion Dialog Workflows ---

    fun requestDeleteFarm(farm: Farm) {
        val preview = repository.previewFarmDeletion(farm.id)
        _activeDeletionTarget.value = DeletionTarget.FarmTarget(farm)
        _activeDeletionReport.value = preview
    }

    fun requestDeletePlot(plot: Plot, farmName: String) {
        val preview = repository.previewPlotDeletion(plot.id)
        _activeDeletionTarget.value = DeletionTarget.PlotTarget(plot, farmName)
        _activeDeletionReport.value = preview
    }

    fun requestDeletePlanting(planting: Planting, plotName: String) {
        val preview = repository.previewPlantingDeletion(planting.id)
        _activeDeletionTarget.value = DeletionTarget.PlantingTarget(planting, plotName)
        _activeDeletionReport.value = preview
    }

    fun confirmDeletion(onComplete: (DeletionReport) -> Unit = {}) {
        val target = _activeDeletionTarget.value ?: return

        val result = when (target) {
            is DeletionTarget.FarmTarget -> repository.deleteFarm(target.farm.id)
            is DeletionTarget.PlotTarget -> repository.deletePlot(target.plot.id)
            is DeletionTarget.PlantingTarget -> repository.deletePlanting(target.planting.id)
        }

        dismissDeletionDialog()

        if (result is FarmOperationResult.Success) {
            navigateUp()
            onComplete(result.data)
        }
    }

    fun dismissDeletionDialog() {
        _activeDeletionTarget.value = null
        _activeDeletionReport.value = null
    }

    private fun resetDrafts() {
        _farmDraft.value = FarmFormDraft()
        _plotDraft.value = PlotFormDraft()
        _plantingDraft.value = PlantingFormDraft()
    }

    private fun mapErrorToMessage(error: FarmManagementError): String = when (error) {
        is FarmManagementError.BlankName -> "${error.fieldName} cannot be blank"
        is FarmManagementError.NameTooLong -> "${error.fieldName} cannot exceed ${error.maxLen} characters"
        is FarmManagementError.DuplicateName -> "${error.entityType} with this name already exists"
        is FarmManagementError.InvalidArea -> error.reason
        is FarmManagementError.ParentNotFound -> "Selected ${error.parentType} was not found"
        is FarmManagementError.EntityNotFound -> "${error.entityType} not found"
        is FarmManagementError.NotesTooLong -> "Notes cannot exceed ${error.maxLen} characters"
        is FarmManagementError.VarietyTooLong -> "Variety cannot exceed ${error.maxLen} characters"
        is FarmManagementError.StorageIoError -> error.message
    }
}
