package com.farmvision.agrirover.domain.repository

import com.farmvision.agrirover.domain.model.farm.DeletionReport
import com.farmvision.agrirover.domain.model.farm.Farm
import com.farmvision.agrirover.domain.model.farm.FarmOperationResult
import com.farmvision.agrirover.domain.model.farm.GrowthStage
import com.farmvision.agrirover.domain.model.farm.PlantHealthStatus
import com.farmvision.agrirover.domain.model.farm.Planting
import com.farmvision.agrirover.domain.model.farm.Plot
import com.farmvision.agrirover.domain.model.farm.RecordOrigin
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/**
 * Unified Agricultural Farm Management Repository Contract.
 * Single source of truth for Farm -> Plot -> Planting hierarchy,
 * reactive StateFlow streams, validation enforcement, atomic cascading deletions,
 * and deterministic sample data seeding.
 */
interface FarmManagementRepository {

    /** Reactive stream of all farms. */
    val farmsFlow: StateFlow<List<Farm>>

    /** Reactive stream of all plots. */
    val plotsFlow: StateFlow<List<Plot>>

    /** Reactive stream of all plantings (crop batches). */
    val plantingsFlow: StateFlow<List<Planting>>

    // --- Queries ---

    fun getFarms(): List<Farm>
    fun getFarm(id: String): Farm?

    fun getPlots(farmId: String? = null): List<Plot>
    fun getPlot(id: String): Plot?

    fun getPlantings(plotId: String? = null): List<Planting>
    fun getPlanting(id: String): Planting?

    // --- Deletion Previews (Exact Dependent Counts for Confirmation) ---

    fun previewFarmDeletion(farmId: String): DeletionReport?
    fun previewPlotDeletion(plotId: String): DeletionReport?
    fun previewPlantingDeletion(plantingId: String): DeletionReport?

    // --- Farm CRUD ---

    fun createFarm(
        name: String,
        description: String? = null,
        locationDescription: String? = null,
        areaSquareMeters: Double? = null,
        notes: String? = null,
        origin: RecordOrigin = RecordOrigin.USER
    ): FarmOperationResult<Farm>

    fun updateFarm(
        id: String,
        name: String,
        description: String? = null,
        locationDescription: String? = null,
        areaSquareMeters: Double? = null,
        notes: String? = null
    ): FarmOperationResult<Farm>

    fun deleteFarm(id: String): FarmOperationResult<DeletionReport>

    // --- Plot CRUD ---

    fun createPlot(
        farmId: String,
        name: String,
        description: String? = null,
        areaSquareMeters: Double? = null,
        notes: String? = null,
        origin: RecordOrigin = RecordOrigin.USER
    ): FarmOperationResult<Plot>

    fun updatePlot(
        id: String,
        name: String,
        description: String? = null,
        areaSquareMeters: Double? = null,
        notes: String? = null,
        targetFarmId: String? = null
    ): FarmOperationResult<Plot>

    fun deletePlot(id: String): FarmOperationResult<DeletionReport>

    // --- Planting CRUD ---

    fun createPlanting(
        plotId: String,
        cropName: String,
        variety: String? = null,
        plantingDate: LocalDate? = null,
        growthStage: GrowthStage = GrowthStage.Unspecified,
        userNotes: String? = null,
        userHealthStatus: PlantHealthStatus = PlantHealthStatus.InsufficientData,
        daysToHarvest: Int? = null,
        origin: RecordOrigin = RecordOrigin.USER
    ): FarmOperationResult<Planting>

    fun updatePlanting(
        id: String,
        cropName: String,
        variety: String? = null,
        plantingDate: LocalDate? = null,
        growthStage: GrowthStage = GrowthStage.Unspecified,
        userNotes: String? = null,
        userHealthStatus: PlantHealthStatus = PlantHealthStatus.InsufficientData,
        daysToHarvest: Int? = null,
        targetPlotId: String? = null
    ): FarmOperationResult<Planting>

    fun deletePlanting(id: String): FarmOperationResult<DeletionReport>

    // --- Seeding ---

    fun seedInitialData(force: Boolean = false): Boolean
    fun isSeeded(): Boolean
}
