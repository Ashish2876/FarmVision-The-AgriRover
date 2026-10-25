package com.farmvision.agrirover.data.repository

import com.farmvision.agrirover.data.seed.SampleFarmSeed
import com.farmvision.agrirover.data.time.SystemTimeProvider
import com.farmvision.agrirover.data.time.TimeProvider
import com.farmvision.agrirover.domain.id.IdGenerator
import com.farmvision.agrirover.domain.id.UuidGenerator
import com.farmvision.agrirover.domain.model.farm.DeletionReport
import com.farmvision.agrirover.domain.model.farm.Farm
import com.farmvision.agrirover.domain.model.farm.FarmManagementError
import com.farmvision.agrirover.domain.model.farm.FarmOperationResult
import com.farmvision.agrirover.domain.model.farm.GrowthStage
import com.farmvision.agrirover.domain.model.farm.PlantHealthStatus
import com.farmvision.agrirover.domain.model.farm.Planting
import com.farmvision.agrirover.domain.model.farm.Plot
import com.farmvision.agrirover.domain.model.farm.RecordOrigin
import com.farmvision.agrirover.domain.repository.FarmManagementRepository
import com.farmvision.agrirover.domain.validation.FarmValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * High-performance, in-memory implementation of [FarmManagementRepository].
 * Serves as both an independent runtime store and the contract test verification baseline.
 */
class InMemoryFarmManagementRepository(
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val idGenerator: IdGenerator = UuidGenerator(),
    autoSeed: Boolean = true
) : FarmManagementRepository {

    private val lock = Any()

    private val _farmsFlow = MutableStateFlow<List<Farm>>(emptyList())
    override val farmsFlow: StateFlow<List<Farm>> = _farmsFlow.asStateFlow()

    private val _plotsFlow = MutableStateFlow<List<Plot>>(emptyList())
    override val plotsFlow: StateFlow<List<Plot>> = _plotsFlow.asStateFlow()

    private val _plantingsFlow = MutableStateFlow<List<Planting>>(emptyList())
    override val plantingsFlow: StateFlow<List<Planting>> = _plantingsFlow.asStateFlow()

    private var seeded: Boolean = false

    init {
        if (autoSeed) {
            seedInitialData(force = false)
        }
    }

    override fun getFarms(): List<Farm> = synchronized(lock) { _farmsFlow.value }
    override fun getFarm(id: String): Farm? = synchronized(lock) { _farmsFlow.value.firstOrNull { it.id == id } }

    override fun getPlots(farmId: String?): List<Plot> = synchronized(lock) {
        if (farmId == null) _plotsFlow.value else _plotsFlow.value.filter { it.farmId == farmId }
    }

    override fun getPlot(id: String): Plot? = synchronized(lock) { _plotsFlow.value.firstOrNull { it.id == id } }

    override fun getPlantings(plotId: String?): List<Planting> = synchronized(lock) {
        if (plotId == null) _plantingsFlow.value else _plantingsFlow.value.filter { it.plotId == plotId }
    }

    override fun getPlanting(id: String): Planting? = synchronized(lock) { _plantingsFlow.value.firstOrNull { it.id == id } }

    override fun previewFarmDeletion(farmId: String): DeletionReport? = synchronized(lock) {
        val farm = _farmsFlow.value.firstOrNull { it.id == farmId } ?: return null
        val plots = _plotsFlow.value.filter { it.farmId == farm.id }
        val plotIds = plots.map { it.id }.toSet()
        val plantings = _plantingsFlow.value.filter { it.plotId in plotIds }
        DeletionReport(
            deletedFarmsCount = 1,
            deletedPlotsCount = plots.size,
            deletedPlantingsCount = plantings.size
        )
    }

    override fun previewPlotDeletion(plotId: String): DeletionReport? = synchronized(lock) {
        val plot = _plotsFlow.value.firstOrNull { it.id == plotId } ?: return null
        val plantings = _plantingsFlow.value.filter { it.plotId == plot.id }
        DeletionReport(
            deletedFarmsCount = 0,
            deletedPlotsCount = 1,
            deletedPlantingsCount = plantings.size
        )
    }

    override fun previewPlantingDeletion(plantingId: String): DeletionReport? = synchronized(lock) {
        val planting = _plantingsFlow.value.firstOrNull { it.id == plantingId } ?: return null
        DeletionReport(
            deletedFarmsCount = 0,
            deletedPlotsCount = 0,
            deletedPlantingsCount = 1
        )
    }

    override fun createFarm(
        name: String,
        description: String?,
        locationDescription: String?,
        areaSquareMeters: Double?,
        notes: String?,
        origin: RecordOrigin
    ): FarmOperationResult<Farm> = synchronized(lock) {
        val nameErr = FarmValidator.validateName("Farm Name", name)
        if (nameErr != null) return FarmOperationResult.Failure(nameErr)

        val normName = FarmValidator.normalizeWhitespace(name)
        if (_farmsFlow.value.any { it.name.equals(normName, ignoreCase = true) }) {
            return FarmOperationResult.Failure(FarmManagementError.DuplicateName("Farm", normName))
        }

        val areaErr = FarmValidator.validateAreaSquareMeters(areaSquareMeters)
        if (areaErr != null) return FarmOperationResult.Failure(areaErr)

        val notesErr = FarmValidator.validateNotes(notes)
        if (notesErr != null) return FarmOperationResult.Failure(notesErr)

        val now = timeProvider.nowEpochMillis()
        val farm = Farm(
            id = idGenerator.generateId("farm"),
            name = normName,
            description = description?.let { FarmValidator.normalizeWhitespace(it) },
            locationDescription = locationDescription?.let { FarmValidator.normalizeWhitespace(it) },
            areaSquareMeters = areaSquareMeters,
            notes = notes?.let { FarmValidator.normalizeWhitespace(it) },
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            origin = origin
        )

        _farmsFlow.value = _farmsFlow.value + farm
        FarmOperationResult.Success(farm)
    }

    override fun updateFarm(
        id: String,
        name: String,
        description: String?,
        locationDescription: String?,
        areaSquareMeters: Double?,
        notes: String?
    ): FarmOperationResult<Farm> = synchronized(lock) {
        val existing = _farmsFlow.value.firstOrNull { it.id == id }
            ?: return FarmOperationResult.Failure(FarmManagementError.EntityNotFound("Farm", id))

        val nameErr = FarmValidator.validateName("Farm Name", name)
        if (nameErr != null) return FarmOperationResult.Failure(nameErr)

        val normName = FarmValidator.normalizeWhitespace(name)
        if (_farmsFlow.value.any { it.id != id && it.name.equals(normName, ignoreCase = true) }) {
            return FarmOperationResult.Failure(FarmManagementError.DuplicateName("Farm", normName))
        }

        val areaErr = FarmValidator.validateAreaSquareMeters(areaSquareMeters)
        if (areaErr != null) return FarmOperationResult.Failure(areaErr)

        val notesErr = FarmValidator.validateNotes(notes)
        if (notesErr != null) return FarmOperationResult.Failure(notesErr)

        val now = timeProvider.nowEpochMillis()
        val updated = existing.copy(
            name = normName,
            description = description?.let { FarmValidator.normalizeWhitespace(it) },
            locationDescription = locationDescription?.let { FarmValidator.normalizeWhitespace(it) },
            areaSquareMeters = areaSquareMeters,
            notes = notes?.let { FarmValidator.normalizeWhitespace(it) },
            updatedAtEpochMillis = now
        )

        _farmsFlow.value = _farmsFlow.value.map { if (it.id == id) updated else it }
        FarmOperationResult.Success(updated)
    }

    override fun deleteFarm(id: String): FarmOperationResult<DeletionReport> = synchronized(lock) {
        val existing = _farmsFlow.value.firstOrNull { it.id == id }
            ?: return FarmOperationResult.Failure(FarmManagementError.EntityNotFound("Farm", id))

        val affectedPlots = _plotsFlow.value.filter { it.farmId == id }
        val affectedPlotIds = affectedPlots.map { it.id }.toSet()
        val affectedPlantings = _plantingsFlow.value.filter { it.plotId in affectedPlotIds }

        // Atomic cascade
        _plantingsFlow.value = _plantingsFlow.value.filterNot { it.plotId in affectedPlotIds }
        _plotsFlow.value = _plotsFlow.value.filterNot { it.farmId == id }
        _farmsFlow.value = _farmsFlow.value.filterNot { it.id == id }

        FarmOperationResult.Success(
            DeletionReport(
                deletedFarmsCount = 1,
                deletedPlotsCount = affectedPlots.size,
                deletedPlantingsCount = affectedPlantings.size
            )
        )
    }

    override fun createPlot(
        farmId: String,
        name: String,
        description: String?,
        areaSquareMeters: Double?,
        notes: String?,
        origin: RecordOrigin
    ): FarmOperationResult<Plot> = synchronized(lock) {
        if (_farmsFlow.value.none { it.id == farmId }) {
            return FarmOperationResult.Failure(FarmManagementError.ParentNotFound("Farm", farmId))
        }

        val nameErr = FarmValidator.validateName("Plot Name", name)
        if (nameErr != null) return FarmOperationResult.Failure(nameErr)

        val normName = FarmValidator.normalizeWhitespace(name)
        if (_plotsFlow.value.any { it.farmId == farmId && it.name.equals(normName, ignoreCase = true) }) {
            return FarmOperationResult.Failure(FarmManagementError.DuplicateName("Plot", normName))
        }

        val areaErr = FarmValidator.validateAreaSquareMeters(areaSquareMeters)
        if (areaErr != null) return FarmOperationResult.Failure(areaErr)

        val notesErr = FarmValidator.validateNotes(notes)
        if (notesErr != null) return FarmOperationResult.Failure(notesErr)

        val now = timeProvider.nowEpochMillis()
        val plot = Plot(
            id = idGenerator.generateId("plot"),
            farmId = farmId,
            name = normName,
            description = description?.let { FarmValidator.normalizeWhitespace(it) },
            areaSquareMeters = areaSquareMeters,
            notes = notes?.let { FarmValidator.normalizeWhitespace(it) },
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            origin = origin
        )

        _plotsFlow.value = _plotsFlow.value + plot
        FarmOperationResult.Success(plot)
    }

    override fun updatePlot(
        id: String,
        name: String,
        description: String?,
        areaSquareMeters: Double?,
        notes: String?,
        targetFarmId: String?
    ): FarmOperationResult<Plot> = synchronized(lock) {
        val existing = _plotsFlow.value.firstOrNull { it.id == id }
            ?: return FarmOperationResult.Failure(FarmManagementError.EntityNotFound("Plot", id))

        val parentFarmId = targetFarmId ?: existing.farmId
        if (_farmsFlow.value.none { it.id == parentFarmId }) {
            return FarmOperationResult.Failure(FarmManagementError.ParentNotFound("Farm", parentFarmId))
        }

        val nameErr = FarmValidator.validateName("Plot Name", name)
        if (nameErr != null) return FarmOperationResult.Failure(nameErr)

        val normName = FarmValidator.normalizeWhitespace(name)
        if (_plotsFlow.value.any { it.id != id && it.farmId == parentFarmId && it.name.equals(normName, ignoreCase = true) }) {
            return FarmOperationResult.Failure(FarmManagementError.DuplicateName("Plot", normName))
        }

        val areaErr = FarmValidator.validateAreaSquareMeters(areaSquareMeters)
        if (areaErr != null) return FarmOperationResult.Failure(areaErr)

        val notesErr = FarmValidator.validateNotes(notes)
        if (notesErr != null) return FarmOperationResult.Failure(notesErr)

        val now = timeProvider.nowEpochMillis()
        val updated = existing.copy(
            farmId = parentFarmId,
            name = normName,
            description = description?.let { FarmValidator.normalizeWhitespace(it) },
            areaSquareMeters = areaSquareMeters,
            notes = notes?.let { FarmValidator.normalizeWhitespace(it) },
            updatedAtEpochMillis = now
        )

        _plotsFlow.value = _plotsFlow.value.map { if (it.id == id) updated else it }
        FarmOperationResult.Success(updated)
    }

    override fun deletePlot(id: String): FarmOperationResult<DeletionReport> = synchronized(lock) {
        val existing = _plotsFlow.value.firstOrNull { it.id == id }
            ?: return FarmOperationResult.Failure(FarmManagementError.EntityNotFound("Plot", id))

        val affectedPlantings = _plantingsFlow.value.filter { it.plotId == id }

        // Atomic cascade
        _plantingsFlow.value = _plantingsFlow.value.filterNot { it.plotId == id }
        _plotsFlow.value = _plotsFlow.value.filterNot { it.id == id }

        FarmOperationResult.Success(
            DeletionReport(
                deletedFarmsCount = 0,
                deletedPlotsCount = 1,
                deletedPlantingsCount = affectedPlantings.size
            )
        )
    }

    override fun createPlanting(
        plotId: String,
        cropName: String,
        variety: String?,
        plantingDate: LocalDate?,
        growthStage: GrowthStage,
        userNotes: String?,
        userHealthStatus: PlantHealthStatus,
        daysToHarvest: Int?,
        origin: RecordOrigin
    ): FarmOperationResult<Planting> = synchronized(lock) {
        if (_plotsFlow.value.none { it.id == plotId }) {
            return FarmOperationResult.Failure(FarmManagementError.ParentNotFound("Plot", plotId))
        }

        val nameErr = FarmValidator.validateName("Crop Name", cropName)
        if (nameErr != null) return FarmOperationResult.Failure(nameErr)

        val varietyErr = FarmValidator.validateVariety(variety)
        if (varietyErr != null) return FarmOperationResult.Failure(varietyErr)

        val notesErr = FarmValidator.validateNotes(userNotes)
        if (notesErr != null) return FarmOperationResult.Failure(notesErr)

        val now = timeProvider.nowEpochMillis()
        val planting = Planting(
            id = idGenerator.generateId("crop"),
            plotId = plotId,
            cropName = FarmValidator.normalizeWhitespace(cropName),
            variety = variety?.let { FarmValidator.normalizeWhitespace(it) },
            plantingDate = plantingDate,
            growthStage = growthStage,
            userNotes = userNotes?.let { FarmValidator.normalizeWhitespace(it) },
            userHealthStatus = userHealthStatus,
            daysToHarvest = daysToHarvest,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            origin = origin
        )

        _plantingsFlow.value = _plantingsFlow.value + planting
        FarmOperationResult.Success(planting)
    }

    override fun updatePlanting(
        id: String,
        cropName: String,
        variety: String?,
        plantingDate: LocalDate?,
        growthStage: GrowthStage,
        userNotes: String?,
        userHealthStatus: PlantHealthStatus,
        daysToHarvest: Int?,
        targetPlotId: String?
    ): FarmOperationResult<Planting> = synchronized(lock) {
        val existing = _plantingsFlow.value.firstOrNull { it.id == id }
            ?: return FarmOperationResult.Failure(FarmManagementError.EntityNotFound("Planting", id))

        val parentPlotId = targetPlotId ?: existing.plotId
        if (_plotsFlow.value.none { it.id == parentPlotId }) {
            return FarmOperationResult.Failure(FarmManagementError.ParentNotFound("Plot", parentPlotId))
        }

        val nameErr = FarmValidator.validateName("Crop Name", cropName)
        if (nameErr != null) return FarmOperationResult.Failure(nameErr)

        val varietyErr = FarmValidator.validateVariety(variety)
        if (varietyErr != null) return FarmOperationResult.Failure(varietyErr)

        val notesErr = FarmValidator.validateNotes(userNotes)
        if (notesErr != null) return FarmOperationResult.Failure(notesErr)

        val now = timeProvider.nowEpochMillis()
        val updated = existing.copy(
            plotId = parentPlotId,
            cropName = FarmValidator.normalizeWhitespace(cropName),
            variety = variety?.let { FarmValidator.normalizeWhitespace(it) },
            plantingDate = plantingDate,
            growthStage = growthStage,
            userNotes = userNotes?.let { FarmValidator.normalizeWhitespace(it) },
            userHealthStatus = userHealthStatus,
            daysToHarvest = daysToHarvest,
            updatedAtEpochMillis = now
        )

        _plantingsFlow.value = _plantingsFlow.value.map { if (it.id == id) updated else it }
        FarmOperationResult.Success(updated)
    }

    override fun deletePlanting(id: String): FarmOperationResult<DeletionReport> = synchronized(lock) {
        val existing = _plantingsFlow.value.firstOrNull { it.id == id }
            ?: return FarmOperationResult.Failure(FarmManagementError.EntityNotFound("Planting", id))

        _plantingsFlow.value = _plantingsFlow.value.filterNot { it.id == id }
        FarmOperationResult.Success(
            DeletionReport(
                deletedFarmsCount = 0,
                deletedPlotsCount = 0,
                deletedPlantingsCount = 1
            )
        )
    }

    override fun seedInitialData(force: Boolean): Boolean = synchronized(lock) {
        if (seeded && !force) return false
        if (!force && (_farmsFlow.value.isNotEmpty() || _plotsFlow.value.isNotEmpty() || _plantingsFlow.value.isNotEmpty())) {
            seeded = true
            return false
        }

        val now = timeProvider.nowEpochMillis()
        val sampleFarm = SampleFarmSeed.createSampleFarm(now)
        val samplePlots = SampleFarmSeed.createSamplePlots(now)
        val samplePlantings = SampleFarmSeed.createSamplePlantings(now)

        _farmsFlow.value = listOf(sampleFarm)
        _plotsFlow.value = samplePlots
        _plantingsFlow.value = samplePlantings
        seeded = true
        true
    }

    override fun isSeeded(): Boolean = synchronized(lock) { seeded }
}
