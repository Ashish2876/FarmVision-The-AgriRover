package com.farmvision.agrirover

import com.farmvision.agrirover.domain.id.SequentialIdGenerator
import com.farmvision.agrirover.domain.id.UuidGenerator
import com.farmvision.agrirover.domain.model.farm.AreaUnit
import com.farmvision.agrirover.domain.model.farm.Farm
import com.farmvision.agrirover.domain.model.farm.FarmManagementError
import com.farmvision.agrirover.domain.model.farm.GrowthStage
import com.farmvision.agrirover.domain.model.farm.PlantHealthStatus
import com.farmvision.agrirover.domain.model.farm.Planting
import com.farmvision.agrirover.domain.model.farm.Plot
import com.farmvision.agrirover.domain.model.farm.RecordOrigin
import com.farmvision.agrirover.domain.validation.FarmValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FarmDomainValidationTest {

    @Test
    fun whitespaceNormalization_collapsesInternalAndTrimsExterior() {
        val raw = "   Green   Valley     Research   Farm  "
        val normalized = FarmValidator.normalizeWhitespace(raw)
        assertEquals("Green Valley Research Farm", normalized)
    }

    @Test
    fun nameValidation_rejectsBlankWhitespaceAndTooLong() {
        // Blank
        val blankErr = FarmValidator.validateName("Farm Name", "")
        assertTrue(blankErr is FarmManagementError.BlankName)

        // Whitespace only
        val whitespaceErr = FarmValidator.validateName("Farm Name", "   \t\n  ")
        assertTrue(whitespaceErr is FarmManagementError.BlankName)

        // Too long (>60 characters)
        val tooLong = "A".repeat(61)
        val tooLongErr = FarmValidator.validateName("Farm Name", tooLong)
        assertTrue(tooLongErr is FarmManagementError.NameTooLong)
        assertEquals(60, (tooLongErr as FarmManagementError.NameTooLong).maxLen)

        // Valid boundary: exactly 60 chars
        val valid60 = "A".repeat(60)
        assertNull(FarmValidator.validateName("Farm Name", valid60))

        // Valid standard
        assertNull(FarmValidator.validateName("Farm Name", "North Sector Plot 1"))
    }

    @Test
    fun varietyValidation_boundsChecking() {
        assertNull(FarmValidator.validateVariety(null))
        assertNull(FarmValidator.validateVariety("Solanum lycopersicum"))

        val tooLong = "V".repeat(61)
        val err = FarmValidator.validateVariety(tooLong)
        assertTrue(err is FarmManagementError.VarietyTooLong)
    }

    @Test
    fun notesValidation_boundsChecking() {
        assertNull(FarmValidator.validateNotes(null))
        assertNull(FarmValidator.validateNotes("Standard irrigation scheduled."))

        val tooLong = "N".repeat(501)
        val err = FarmValidator.validateNotes(tooLong)
        assertTrue(err is FarmManagementError.NotesTooLong)
    }

    @Test
    fun areaValidation_enforcesFinitePositiveAndSanityBound() {
        assertNull(FarmValidator.validateAreaSquareMeters(null))
        assertNull(FarmValidator.validateAreaSquareMeters(1000.0))

        // NaN
        val nanErr = FarmValidator.validateAreaSquareMeters(Double.NaN)
        assertTrue(nanErr is FarmManagementError.InvalidArea)

        // Infinite
        val infErr = FarmValidator.validateAreaSquareMeters(Double.POSITIVE_INFINITY)
        assertTrue(infErr is FarmManagementError.InvalidArea)

        // Zero
        val zeroErr = FarmValidator.validateAreaSquareMeters(0.0)
        assertTrue(zeroErr is FarmManagementError.InvalidArea)

        // Negative
        val negErr = FarmValidator.validateAreaSquareMeters(-15.5)
        assertTrue(negErr is FarmManagementError.InvalidArea)

        // Exceeds 10,000 hectares sanity bound (100,000,000 m²)
        val overBoundErr = FarmValidator.validateAreaSquareMeters(100_000_001.0)
        assertTrue(overBoundErr is FarmManagementError.InvalidArea)

        // Exactly at bound
        assertNull(FarmValidator.validateAreaSquareMeters(100_000_000.0))
    }

    @Test
    fun areaConversion_hectaresAndAcresRoundTripCalculations() {
        // 1 Hectare = 10,000 m²
        val oneHaInSqM = AreaUnit.Hectares.toSquareMeters(1.0)
        assertEquals(10_000.0, oneHaInSqM, 0.001)
        assertEquals(1.0, AreaUnit.Hectares.fromSquareMeters(10_000.0), 0.001)

        // 1 Acre = 4046.8564224 m²
        val oneAcInSqM = AreaUnit.Acres.toSquareMeters(1.0)
        assertEquals(4046.8564224, oneAcInSqM, 0.0001)
        assertEquals(1.0, AreaUnit.Acres.fromSquareMeters(4046.8564224), 0.0001)

        // Formatting
        val formattedM2 = AreaUnit.SquareMeters.format(2500.0)
        assertEquals("2500.00 m²", formattedM2)

        val formattedHa = AreaUnit.Hectares.format(25000.0)
        assertEquals("2.50 ha", formattedHa)

        val formattedAc = AreaUnit.Acres.format(4046.8564224 * 2.0)
        assertEquals("2.00 ac", formattedAc)
    }

    @Test
    fun idGenerator_sequentialAndUuidBehaviors() {
        val seqGen = SequentialIdGenerator()
        assertEquals("1", seqGen.generateId())
        assertEquals("2", seqGen.generateId())
        assertEquals("farm-3", seqGen.generateId("farm"))

        seqGen.reset(1)
        assertEquals("1", seqGen.generateId())

        val uuidGen = UuidGenerator()
        val id1 = uuidGen.generateId()
        val id2 = uuidGen.generateId("plot")
        assertTrue(id1.isNotBlank())
        assertTrue(id2.startsWith("plot-"))
    }

    @Test
    fun growthStage_parsingAndStandardFallbacks() {
        assertEquals(GrowthStage.Flowering, GrowthStage.fromString("Flowering"))
        assertEquals(GrowthStage.Flowering, GrowthStage.fromString("Flowering (Day 42)"))
        assertEquals(GrowthStage.Flowering, GrowthStage.fromString("Tasseling (Day 56)"))
        assertEquals(GrowthStage.FruitSetting, GrowthStage.fromString("Fruit Setting (Day 38)"))
        assertEquals(GrowthStage.FruitSetting, GrowthStage.fromString("Pod Development (Day 64)"))
        assertEquals(GrowthStage.Seedling, GrowthStage.fromString("Seedling"))
        assertEquals(GrowthStage.Vegetative, GrowthStage.fromString("Vegetative stage"))
        assertEquals(GrowthStage.Harvested, GrowthStage.fromString("Harvested"))
        assertEquals(GrowthStage.Unspecified, GrowthStage.fromString("Unknown / Unspecified"))
    }

    @Test
    fun plantHealthStatus_parsingTruthfulness() {
        assertEquals(PlantHealthStatus.Thriving, PlantHealthStatus.fromString("Thriving"))
        assertEquals(PlantHealthStatus.Nominal, PlantHealthStatus.fromString("Nominal"))
        assertEquals(PlantHealthStatus.Nominal, PlantHealthStatus.fromString("Active"))
        assertEquals(PlantHealthStatus.NeedsAttention, PlantHealthStatus.fromString("Needs Attention"))
        assertEquals(PlantHealthStatus.NeedsAttention, PlantHealthStatus.fromString("Warning"))
        assertEquals(PlantHealthStatus.Critical, PlantHealthStatus.fromString("Critical"))
        assertEquals(PlantHealthStatus.InsufficientData, PlantHealthStatus.fromString("Random String"))
    }

    @Test
    fun domainEntities_creationAndImmutability() {
        val farm = Farm(
            id = "farm-1",
            name = "Alpha Farm",
            description = "Main research parcel",
            locationDescription = "Sector 4 North Furrow",
            areaSquareMeters = 25000.0,
            notes = "Drip irrigation equipped",
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L,
            origin = RecordOrigin.USER
        )

        val updatedFarm = farm.copy(name = "Alpha Research Farm", updatedAtEpochMillis = 2000L)
        assertEquals("farm-1", updatedFarm.id)
        assertEquals("Alpha Research Farm", updatedFarm.name)
        assertEquals(2000L, updatedFarm.updatedAtEpochMillis)

        val plot = Plot(
            id = "plot-1",
            farmId = "farm-1",
            name = "Plot 1A",
            areaSquareMeters = 5000.0,
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L,
            origin = RecordOrigin.USER
        )
        assertEquals("farm-1", plot.farmId)

        val planting = Planting(
            id = "planting-1",
            plotId = "plot-1",
            cropName = "Roma Tomato",
            variety = "Solanum lycopersicum",
            plantingDate = LocalDate.of(2026, 4, 15),
            growthStage = GrowthStage.Flowering,
            userHealthStatus = PlantHealthStatus.Thriving,
            daysToHarvest = 35,
            createdAtEpochMillis = 1000L,
            updatedAtEpochMillis = 1000L,
            origin = RecordOrigin.SAMPLE
        )
        assertEquals("plot-1", planting.plotId)
        assertEquals(RecordOrigin.SAMPLE, planting.origin)
    }
}
