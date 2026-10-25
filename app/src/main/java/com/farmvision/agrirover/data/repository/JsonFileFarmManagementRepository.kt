package com.farmvision.agrirover.data.repository

import android.content.Context
import com.farmvision.agrirover.data.local.FarmDataPayload
import com.farmvision.agrirover.data.local.FarmJsonSerializer
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
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate

/**
 * Production implementation of [FarmManagementRepository] backed by a lightweight,
 * versioned atomic JSON file store in application internal storage.
 *
 * Concurrency: Thread-safe, serialized single-writer updates via JVM monitor lock.
 * Durability: Atomically writes to a temporary file before renaming to prevent corrupt partial writes.
 * Fault-Tolerance: Automatically handles corrupt or invalid JSON files by isolating the corrupted file
 * and re-initializing clean state.
 */
class JsonFileFarmManagementRepository(
    val storageFile: File,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val idGenerator: IdGenerator = UuidGenerator(),
    autoSeedIfEmpty: Boolean = true
) : FarmManagementRepository {

    constructor(
        context: Context,
        timeProvider: TimeProvider = SystemTimeProvider(),
        idGenerator: IdGenerator = UuidGenerator(),
        autoSeedIfEmpty: Boolean = true
    ) : this(
        storageFile = File(context.filesDir, DEFAULT_FILENAME),
        timeProvider = timeProvider,
        idGenerator = idGenerator,
        autoSeedIfEmpty = autoSeedIfEmpty
    )

    private val lock = Any()

    private val _farmsFlow = MutableStateFlow<List<Farm>>(emptyList())
    override val farmsFlow: StateFlow<List<Farm>> = _farmsFlow.asStateFlow()

    private val _plotsFlow = MutableStateFlow<List<Plot>>(emptyList())
    override val plotsFlow: StateFlow<List<Plot>> = _plotsFlow.asStateFlow()

    private val _plantingsFlow = MutableStateFlow<List<Planting>>(emptyList())
    override val plantingsFlow: StateFlow<List<Planting>> = _plantingsFlow.asStateFlow()

    private var isSampleDataSeeded: Boolean = false

    init {
        loadFromDisk(autoSeedIfEmpty)
    }

    private fun loadFromDisk(autoSeedIfEmpty: Boolean) = synchronized(lock) {
        if (!storageFile.exists() || storageFile.length() == 0L) {
            if (autoSeedIfEmpty) {
                seedInitialData(force = false)
            }
            return
        }

        try {
            val json = storageFile.readText(StandardCharsets.UTF_8)
            val payload = FarmJsonSerializer.deserialize(json)
            _farmsFlow.value = payload.farms
            _plotsFlow.value = payload.plots
            _plantingsFlow.value = payload.plantings
            isSampleDataSeeded = payload.isSampleDataSeeded

            if (autoSeedIfEmpty && _farmsFlow.value.isEmpty() && _plotsFlow.value.isEmpty() && _plantingsFlow.value.isEmpty() && !isSampleDataSeeded) {
                seedInitialData(force = false)
            }
        } catch (e: Exception) {
            // Quarantine corrupted file to prevent crashes
            val backup = File(storageFile.parentFile, "${storageFile.name}.corrupt_${System.currentTimeMillis()}")
            try {
                if (storageFile.exists()) {
                    storageFile.renameTo(backup)
                }
            } catch (_: Exception) { }

            _farmsFlow.value = emptyList()
            _plotsFlow.value = emptyList()
            _plantingsFlow.value = emptyList()
            isSampleDataSeeded = false

            if (autoSeedIfEmpty) {
                seedInitialData(force = false)
            }
        }
    }

    private fun persistToDisk(): Boolean = synchronized(lock) {
        val payload = FarmDataPayload(
            schemaVersion = FarmJsonSerializer.CURRENT_SCHEMA_VERSION,
            lastSavedEpochMillis = timeProvider.nowEpochMillis(),
            isSampleDataSeeded = isSampleDataSeeded,
            farms = _farmsFlow.value,
            plots = _plotsFlow.value,
            plantings = _plantingsFlow.value
        )

        val jsonString = FarmJsonSerializer.serialize(payload)
        val parentDir = storageFile.parentFile
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs()
        }

        val tempFile = File(storageFile.parentFile, "${storageFile.name}.tmp")
        return try {
            val fos = FileOutputStream(tempFile)
            val writer = OutputStreamWriter(fos, StandardCharsets.UTF_8)
            try {
                writer.write(jsonString)
                writer.flush()
                try {
                    fos.fd.sync()
                } catch (_: Exception) { }
            } finally {
                try { writer.close() } catch (_: Exception) { }
                try { fos.close() } catch (_: Exception) { }
            }

            // Atomic move where supported, fallback to rename
            if (tempFile.exists()) {
                try {
                    Files.move(
                        tempFile.toPath(),
                        storageFile.toPath(),
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                    )
                } catch (_: Exception) {
                    if (storageFile.exists()) {
                        storageFile.delete()
                    }
                    tempFile.renameTo(storageFile)
                }
            }
            true
        } catch (e: Exception) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            false
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

        val previousFarms = _farmsFlow.value
        _farmsFlow.value = previousFarms + farm
        if (!persistToDisk()) {
            _farmsFlow.value = previousFarms
            return FarmOperationResult.Failure(FarmManagementError.StorageIoError("Failed to persist farm to storage."))
        }

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

        val previousFarms = _farmsFlow.value
        _farmsFlow.value = previousFarms.map { if (it.id == id) updated else it }
        if (!persistToDisk()) {
            _farmsFlow.value = previousFarms
            return FarmOperationResult.Failure(FarmManagementError.StorageIoError("Failed to persist farm updates to storage."))
        }

        FarmOperationResult.Success(updated)
    }

    override fun deleteFarm(id: String): FarmOperationResult<DeletionReport> = synchronized(lock) {
        val existing = _farmsFlow.value.firstOrNull { it.id == id }
            ?: return FarmOperationResult.Failure(FarmManagementError.EntityNotFound("Farm", id))

        val affectedPlots = _plotsFlow.value.filter { it.farmId == id }
        val affectedPlotIds = affectedPlots.map { it.id }.toSet()
        val affectedPlantings = _plantingsFlow.value.filter { it.plotId in affectedPlotIds }

        val prevFarms = _farmsFlow.value
        val prevPlots = _plotsFlow.value
        val prevPlantings = _plantingsFlow.value

        // Atomic cascade
        _plantingsFlow.value = prevPlantings.filterNot { it.plotId in affectedPlotIds }
        _plotsFlow.value = prevPlots.filterNot { it.farmId == id }
        _farmsFlow.value = prevFarms.filterNot { it.id == id }

        if (!persistToDisk()) {
            _plantingsFlow.value = prevPlantings
            _plotsFlow.value = prevPlots
            _farmsFlow.value = prevFarms
            return FarmOperationResult.Failure(FarmManagementError.StorageIoError("Failed to persist cascading deletion to storage."))
        }

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

        val prevPlots = _plotsFlow.value
        _plotsFlow.value = prevPlots + plot
        if (!persistToDisk()) {
            _plotsFlow.value = prevPlots
            return FarmOperationResult.Failure(FarmManagementError.StorageIoError("Failed to persist plot to storage."))
        }

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

        val prevPlots = _plotsFlow.value
        _plotsFlow.value = prevPlots.map { if (it.id == id) updated else it }
        if (!persistToDisk()) {
            _plotsFlow.value = prevPlots
            return FarmOperationResult.Failure(FarmManagementError.StorageIoError("Failed to persist plot updates to storage."))
        }

        FarmOperationResult.Success(updated)
    }

    override fun deletePlot(id: String): FarmOperationResult<DeletionReport> = synchronized(lock) {
        val existing = _plotsFlow.value.firstOrNull { it.id == id }
            ?: return FarmOperationResult.Failure(FarmManagementError.EntityNotFound("Plot", id))

        val affectedPlantings = _plantingsFlow.value.filter { it.plotId == id }

        val prevPlots = _plotsFlow.value
        val prevPlantings = _plantingsFlow.value

        // Atomic cascade
        _plantingsFlow.value = prevPlantings.filterNot { it.plotId == id }
        _plotsFlow.value = prevPlots.filterNot { it.id == id }

        if (!persistToDisk()) {
            _plantingsFlow.value = prevPlantings
            _plotsFlow.value = prevPlots
            return FarmOperationResult.Failure(FarmManagementError.StorageIoError("Failed to persist plot deletion to storage."))
        }

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

        val prevPlantings = _plantingsFlow.value
        _plantingsFlow.value = prevPlantings + planting
        if (!persistToDisk()) {
            _plantingsFlow.value = prevPlantings
            return FarmOperationResult.Failure(FarmManagementError.StorageIoError("Failed to persist planting to storage."))
        }

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

        val prevPlantings = _plantingsFlow.value
        _plantingsFlow.value = prevPlantings.map { if (it.id == id) updated else it }
        if (!persistToDisk()) {
            _plantingsFlow.value = prevPlantings
            return FarmOperationResult.Failure(FarmManagementError.StorageIoError("Failed to persist planting updates to storage."))
        }

        FarmOperationResult.Success(updated)
    }

    override fun deletePlanting(id: String): FarmOperationResult<DeletionReport> = synchronized(lock) {
        val existing = _plantingsFlow.value.firstOrNull { it.id == id }
            ?: return FarmOperationResult.Failure(FarmManagementError.EntityNotFound("Planting", id))

        val prevPlantings = _plantingsFlow.value
        _plantingsFlow.value = prevPlantings.filterNot { it.id == id }
        if (!persistToDisk()) {
            _plantingsFlow.value = prevPlantings
            return FarmOperationResult.Failure(FarmManagementError.StorageIoError("Failed to persist planting deletion to storage."))
        }

        FarmOperationResult.Success(
            DeletionReport(
                deletedFarmsCount = 0,
                deletedPlotsCount = 0,
                deletedPlantingsCount = 1
            )
        )
    }

    override fun seedInitialData(force: Boolean): Boolean = synchronized(lock) {
        if (isSampleDataSeeded && !force) return false
        if (!force && (_farmsFlow.value.isNotEmpty() || _plotsFlow.value.isNotEmpty() || _plantingsFlow.value.isNotEmpty())) {
            isSampleDataSeeded = true
            persistToDisk()
            return false
        }

        val now = timeProvider.nowEpochMillis()
        val sampleFarm = SampleFarmSeed.createSampleFarm(now)
        val samplePlots = SampleFarmSeed.createSamplePlots(now)
        val samplePlantings = SampleFarmSeed.createSamplePlantings(now)

        _farmsFlow.value = listOf(sampleFarm)
        _plotsFlow.value = samplePlots
        _plantingsFlow.value = samplePlantings
        isSampleDataSeeded = true
        persistToDisk()
        true
    }

    override fun isSeeded(): Boolean = synchronized(lock) { isSampleDataSeeded }

    companion object {
        const val DEFAULT_FILENAME = "farmvision_farms_v1.json"
    }
}
