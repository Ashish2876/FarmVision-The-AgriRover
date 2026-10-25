package com.farmvision.agrirover

import com.farmvision.agrirover.data.repository.InMemoryFarmManagementRepository
import com.farmvision.agrirover.data.repository.JsonFileFarmManagementRepository
import com.farmvision.agrirover.data.time.FakeTimeProvider
import com.farmvision.agrirover.domain.id.SequentialIdGenerator
import com.farmvision.agrirover.domain.model.farm.AreaUnit
import com.farmvision.agrirover.domain.model.farm.FarmManagementError
import com.farmvision.agrirover.domain.model.farm.GrowthStage
import com.farmvision.agrirover.domain.model.farm.PlantHealthStatus
import com.farmvision.agrirover.domain.model.farm.RecordOrigin
import com.farmvision.agrirover.domain.repository.FarmManagementRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

class FarmRepositoryContractTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createInMemoryRepo(): FarmManagementRepository {
        return InMemoryFarmManagementRepository(
            timeProvider = FakeTimeProvider(1_700_000_000_000L),
            idGenerator = SequentialIdGenerator(1),
            autoSeed = false
        )
    }

    private fun createJsonFileRepo(file: File? = null): JsonFileFarmManagementRepository {
        val target = file ?: tempFolder.newFile("test_farmvision_${System.nanoTime()}.json")
        return JsonFileFarmManagementRepository(
            storageFile = target,
            timeProvider = FakeTimeProvider(1_700_000_000_000L),
            idGenerator = SequentialIdGenerator(1),
            autoSeedIfEmpty = false
        )
    }

    private fun executeContractTests(repoProvider: () -> FarmManagementRepository) {
        val repo = repoProvider()

        // 1. Initial State
        assertTrue(repo.getFarms().isEmpty())
        assertTrue(repo.getPlots().isEmpty())
        assertTrue(repo.getPlantings().isEmpty())

        // 2. Seeding
        val seeded = repo.seedInitialData()
        assertTrue(seeded)
        assertEquals(1, repo.getFarms().size)
        assertEquals(4, repo.getPlots().size)
        assertEquals(4, repo.getPlantings().size)
        val initialFarm = repo.getFarms().first()
        assertEquals(RecordOrigin.SAMPLE, initialFarm.origin)

        // Seeding idempotency
        val reseeded = repo.seedInitialData(force = false)
        assertFalse(reseeded)
        assertEquals(1, repo.getFarms().size)

        // 3. Farm Creation & Validation
        val farmRes = repo.createFarm(
            name = "Sunny Meadows",
            description = "Organic crop parcel",
            areaSquareMeters = 50_000.0,
            origin = RecordOrigin.USER
        )
        assertTrue(farmRes.isSuccess)
        val sunnyFarm = farmRes.getOrNull()!!
        assertEquals("Sunny Meadows", sunnyFarm.name)
        assertEquals(50_000.0, sunnyFarm.areaSquareMeters!!, 0.01)
        assertEquals(RecordOrigin.USER, sunnyFarm.origin)

        // Duplicate farm name rejected
        val dupFarmRes = repo.createFarm(name = "  sunny   meadows  ")
        assertTrue(dupFarmRes.isFailure)
        assertTrue(dupFarmRes.errorOrNull() is FarmManagementError.DuplicateName)

        // 4. Plot Creation & Referential Integrity
        val orphanPlotRes = repo.createPlot(farmId = "non-existent-farm", name = "Plot 1")
        assertTrue(orphanPlotRes.isFailure)
        assertTrue(orphanPlotRes.errorOrNull() is FarmManagementError.ParentNotFound)

        val plot1Res = repo.createPlot(
            farmId = sunnyFarm.id,
            name = "North Field 1",
            areaSquareMeters = 10_000.0
        )
        assertTrue(plot1Res.isSuccess)
        val plot1 = plot1Res.getOrNull()!!
        assertEquals(sunnyFarm.id, plot1.farmId)

        // Duplicate plot name in same farm rejected
        val dupPlotRes = repo.createPlot(farmId = sunnyFarm.id, name = "north field 1")
        assertTrue(dupPlotRes.isFailure)
        assertTrue(dupPlotRes.errorOrNull() is FarmManagementError.DuplicateName)

        // 5. Planting Creation & Referential Integrity
        val orphanPlantingRes = repo.createPlanting(plotId = "non-existent-plot", cropName = "Carrots")
        assertTrue(orphanPlantingRes.isFailure)
        assertTrue(orphanPlantingRes.errorOrNull() is FarmManagementError.ParentNotFound)

        val planting1Res = repo.createPlanting(
            plotId = plot1.id,
            cropName = "Heritage Carrots",
            variety = "Daucus carota sativus",
            plantingDate = LocalDate.of(2026, 3, 1),
            growthStage = GrowthStage.Vegetative,
            userHealthStatus = PlantHealthStatus.Nominal,
            daysToHarvest = 50
        )
        assertTrue(planting1Res.isSuccess)
        val planting1 = planting1Res.getOrNull()!!
        assertEquals(plot1.id, planting1.plotId)
        assertEquals("Heritage Carrots", planting1.cropName)

        // Multiple plantings with same crop name allowed in same plot
        val planting2Res = repo.createPlanting(
            plotId = plot1.id,
            cropName = "Heritage Carrots",
            variety = "Succession sowing batch 2"
        )
        assertTrue(planting2Res.isSuccess)

        // 6. Deletion Preview
        val farmDeletionPreview = repo.previewFarmDeletion(sunnyFarm.id)
        assertNotNull(farmDeletionPreview)
        assertEquals(1, farmDeletionPreview!!.deletedFarmsCount)
        assertEquals(1, farmDeletionPreview.deletedPlotsCount)
        assertEquals(2, farmDeletionPreview.deletedPlantingsCount)

        // 7. Atomic Cascading Plot Deletion
        val plotDelRes = repo.deletePlot(plot1.id)
        assertTrue(plotDelRes.isSuccess)
        val plotDelReport = plotDelRes.getOrNull()!!
        assertEquals(1, plotDelReport.deletedPlotsCount)
        assertEquals(2, plotDelReport.deletedPlantingsCount)

        // Verify children removed
        assertNull(repo.getPlot(plot1.id))
        assertTrue(repo.getPlantings(plot1.id).isEmpty())
        assertNotNull(repo.getFarm(sunnyFarm.id))

        // 8. Atomic Cascading Farm Deletion
        // Create fresh plot and planting in sample farm to test farm cascade
        val sampleFarm = repo.getFarms().first { it.origin == RecordOrigin.SAMPLE }
        val samplePlotsCount = repo.getPlots(sampleFarm.id).size
        val samplePlantingsCount = repo.getPlantings().count { p ->
            repo.getPlots(sampleFarm.id).any { it.id == p.plotId }
        }

        val farmDelRes = repo.deleteFarm(sampleFarm.id)
        assertTrue(farmDelRes.isSuccess)
        val farmDelReport = farmDelRes.getOrNull()!!
        assertEquals(1, farmDelReport.deletedFarmsCount)
        assertEquals(samplePlotsCount, farmDelReport.deletedPlotsCount)
        assertEquals(samplePlantingsCount, farmDelReport.deletedPlantingsCount)

        assertNull(repo.getFarm(sampleFarm.id))
        assertTrue(repo.getPlots(sampleFarm.id).isEmpty())
    }

    @Test
    fun contract_inMemoryRepository_passesAllSpecificationTests() {
        executeContractTests { createInMemoryRepo() }
    }

    @Test
    fun contract_jsonFileRepository_passesAllSpecificationTests() {
        executeContractTests { createJsonFileRepo() }
    }

    @Test
    fun jsonFileRepository_restartPersistence_survivesProcessDeath() {
        val storageFile = tempFolder.newFile("persist_test.json")
        val fakeClock = FakeTimeProvider(1_700_000_000_000L)
        val idGen = SequentialIdGenerator(10)

        // Session 1: Create user data
        val session1Repo = JsonFileFarmManagementRepository(
            storageFile = storageFile,
            timeProvider = fakeClock,
            idGenerator = idGen,
            autoSeedIfEmpty = false
        )

        session1Repo.seedInitialData()
        val farmResult = session1Repo.createFarm(
            name = "Highland Orchard",
            description = "Apple and Pear terrace",
            locationDescription = "Hill Sector B",
            areaSquareMeters = 35_000.0,
            notes = "Drip irrigation installed",
            origin = RecordOrigin.USER
        )
        val createdFarm = farmResult.getOrNull()!!

        val plotResult = session1Repo.createPlot(
            farmId = createdFarm.id,
            name = "Crisp Apple Furrow",
            areaSquareMeters = 12_000.0,
            origin = RecordOrigin.USER
        )
        val createdPlot = plotResult.getOrNull()!!

        val plantingResult = session1Repo.createPlanting(
            plotId = createdPlot.id,
            cropName = "Honeycrisp",
            variety = "Malus domestica",
            plantingDate = LocalDate.of(2025, 10, 1),
            growthStage = GrowthStage.Ripening,
            userHealthStatus = PlantHealthStatus.Thriving,
            daysToHarvest = 15,
            origin = RecordOrigin.USER
        )
        val createdPlanting = plantingResult.getOrNull()!!

        // Session 2: Instantiate new repository instance pointing to the same file (simulating cold process restart)
        val session2Repo = JsonFileFarmManagementRepository(
            storageFile = storageFile,
            timeProvider = fakeClock,
            idGenerator = idGen,
            autoSeedIfEmpty = false
        )

        // Verify all data survived intact
        val restoredFarm = session2Repo.getFarm(createdFarm.id)
        assertNotNull(restoredFarm)
        assertEquals("Highland Orchard", restoredFarm!!.name)
        assertEquals(35_000.0, restoredFarm.areaSquareMeters!!, 0.01)
        assertEquals(RecordOrigin.USER, restoredFarm.origin)

        val restoredPlot = session2Repo.getPlot(createdPlot.id)
        assertNotNull(restoredPlot)
        assertEquals("Crisp Apple Furrow", restoredPlot!!.name)
        assertEquals(createdFarm.id, restoredPlot.farmId)

        val restoredPlanting = session2Repo.getPlanting(createdPlanting.id)
        assertNotNull(restoredPlanting)
        assertEquals("Honeycrisp", restoredPlanting!!.cropName)
        assertEquals(GrowthStage.Ripening, restoredPlanting.growthStage)
        assertEquals(PlantHealthStatus.Thriving, restoredPlanting.userHealthStatus)
        assertEquals(15, restoredPlanting.daysToHarvest)
        assertEquals(LocalDate.of(2025, 10, 1), restoredPlanting.plantingDate)
        assertEquals(RecordOrigin.USER, restoredPlanting.origin)
    }

    @Test
    fun jsonFileRepository_corruptFileHandling_recoversGracefullyWithoutCrashing() {
        val storageFile = tempFolder.newFile("corrupt_test.json")
        storageFile.writeText("{ \"invalid_syntax\": [ corrupted JSON here ... }")

        val repo = JsonFileFarmManagementRepository(
            storageFile = storageFile,
            autoSeedIfEmpty = true
        )

        // Should not crash, and should auto-seed clean default state
        assertEquals(1, repo.getFarms().size)
        assertEquals(4, repo.getPlots().size)
        assertEquals(4, repo.getPlantings().size)
    }
}
