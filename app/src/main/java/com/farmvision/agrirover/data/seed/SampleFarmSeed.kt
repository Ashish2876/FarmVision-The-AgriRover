package com.farmvision.agrirover.data.seed

import com.farmvision.agrirover.domain.model.farm.Farm
import com.farmvision.agrirover.domain.model.farm.GrowthStage
import com.farmvision.agrirover.domain.model.farm.PlantHealthStatus
import com.farmvision.agrirover.domain.model.farm.Planting
import com.farmvision.agrirover.domain.model.farm.Plot
import com.farmvision.agrirover.domain.model.farm.RecordOrigin
import java.time.LocalDate

/**
 * Deterministic seed dataset migrating the existing 4 legacy sample plants
 * into the Farm -> Plot -> Planting hierarchy under a single sample farm.
 * All records are stamped with RecordOrigin.SAMPLE.
 */
object SampleFarmSeed {

    const val SAMPLE_FARM_ID = "farm-sample-01"
    const val SAMPLE_PLOT_A2_ID = "plot-sample-01"
    const val SAMPLE_PLOT_B1_ID = "plot-sample-02"
    const val SAMPLE_PLOT_B4_ID = "plot-sample-03"
    const val SAMPLE_PLOT_C3_ID = "plot-sample-04"

    const val CROP_01_ID = "crop-01"
    const val CROP_02_ID = "crop-02"
    const val CROP_03_ID = "crop-03"
    const val CROP_04_ID = "crop-04"

    fun createSampleFarm(timestampEpochMillis: Long = 1_700_000_000_000L): Farm {
        return Farm(
            id = SAMPLE_FARM_ID,
            name = "Green Valley Research Farm",
            description = "Primary demonstration parcel for AgriRover autonomous field scouting.",
            locationDescription = "North Ridge • Sector 4",
            areaSquareMeters = 120_000.0, // 12 hectares
            notes = "Equipped with furrowed crop rows and dynamic microclimate monitoring.",
            createdAtEpochMillis = timestampEpochMillis,
            updatedAtEpochMillis = timestampEpochMillis,
            origin = RecordOrigin.SAMPLE
        )
    }

    fun createSamplePlots(timestampEpochMillis: Long = 1_700_000_000_000L): List<Plot> {
        return listOf(
            Plot(
                id = SAMPLE_PLOT_A2_ID,
                farmId = SAMPLE_FARM_ID,
                name = "Sector A - Plot 2",
                description = "Alluvial soil row with drip irrigation infrastructure.",
                areaSquareMeters = 15_000.0,
                notes = "Optimal drainage zone.",
                createdAtEpochMillis = timestampEpochMillis,
                updatedAtEpochMillis = timestampEpochMillis,
                origin = RecordOrigin.SAMPLE
            ),
            Plot(
                id = SAMPLE_PLOT_B1_ID,
                farmId = SAMPLE_FARM_ID,
                name = "Sector B - Plot 1",
                description = "Sandy loam test furrow for high-yield cereal crops.",
                areaSquareMeters = 25_000.0,
                notes = "Central furrow irrigation.",
                createdAtEpochMillis = timestampEpochMillis,
                updatedAtEpochMillis = timestampEpochMillis,
                origin = RecordOrigin.SAMPLE
            ),
            Plot(
                id = SAMPLE_PLOT_B4_ID,
                farmId = SAMPLE_FARM_ID,
                name = "Sector B - Plot 4",
                description = "Clay loam parcel; testbed for low-moisture threshold warnings.",
                areaSquareMeters = 18_000.0,
                notes = "Higher moisture retention capacity.",
                createdAtEpochMillis = timestampEpochMillis,
                updatedAtEpochMillis = timestampEpochMillis,
                origin = RecordOrigin.SAMPLE
            ),
            Plot(
                id = SAMPLE_PLOT_C3_ID,
                farmId = SAMPLE_FARM_ID,
                name = "Sector C - Plot 3",
                description = "Loamy parcel dedicated to legume crop rotation cycles.",
                areaSquareMeters = 22_000.0,
                notes = "Nitrogen-fixing rotation parcel.",
                createdAtEpochMillis = timestampEpochMillis,
                updatedAtEpochMillis = timestampEpochMillis,
                origin = RecordOrigin.SAMPLE
            )
        )
    }

    fun createSamplePlantings(timestampEpochMillis: Long = 1_700_000_000_000L): List<Planting> {
        return listOf(
            Planting(
                id = CROP_01_ID,
                plotId = SAMPLE_PLOT_A2_ID,
                cropName = "Roma Tomato",
                variety = "Solanum lycopersicum",
                plantingDate = LocalDate.now().minusDays(42),
                growthStage = GrowthStage.Flowering,
                userNotes = "Flowering stage (Day 42). Hydration optimal.",
                userHealthStatus = PlantHealthStatus.Thriving,
                daysToHarvest = 35,
                createdAtEpochMillis = timestampEpochMillis,
                updatedAtEpochMillis = timestampEpochMillis,
                origin = RecordOrigin.SAMPLE
            ),
            Planting(
                id = CROP_02_ID,
                plotId = SAMPLE_PLOT_B1_ID,
                cropName = "Sweet Corn",
                variety = "Zea mays var. saccharata",
                plantingDate = LocalDate.now().minusDays(56),
                growthStage = GrowthStage.Flowering,
                userNotes = "Tasseling stage (Day 56). Soil drying moderately.",
                userHealthStatus = PlantHealthStatus.Nominal,
                daysToHarvest = 28,
                createdAtEpochMillis = timestampEpochMillis,
                updatedAtEpochMillis = timestampEpochMillis,
                origin = RecordOrigin.SAMPLE
            ),
            Planting(
                id = CROP_03_ID,
                plotId = SAMPLE_PLOT_B4_ID,
                cropName = "Bell Pepper",
                variety = "Capsicum annuum",
                plantingDate = LocalDate.now().minusDays(38),
                growthStage = GrowthStage.FruitSetting,
                userNotes = "Fruit Setting (Day 38). Low moisture threshold advisory active.",
                userHealthStatus = PlantHealthStatus.NeedsAttention,
                daysToHarvest = 45,
                createdAtEpochMillis = timestampEpochMillis,
                updatedAtEpochMillis = timestampEpochMillis,
                origin = RecordOrigin.SAMPLE
            ),
            Planting(
                id = CROP_04_ID,
                plotId = SAMPLE_PLOT_C3_ID,
                cropName = "Golden Soybean",
                variety = "Glycine max",
                plantingDate = LocalDate.now().minusDays(64),
                growthStage = GrowthStage.FruitSetting,
                userNotes = "Pod Development (Day 64). Optimal rootzone saturation.",
                userHealthStatus = PlantHealthStatus.Nominal,
                daysToHarvest = 22,
                createdAtEpochMillis = timestampEpochMillis,
                updatedAtEpochMillis = timestampEpochMillis,
                origin = RecordOrigin.SAMPLE
            )
        )
    }
}
