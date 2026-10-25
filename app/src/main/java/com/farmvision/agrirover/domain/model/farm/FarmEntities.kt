package com.farmvision.agrirover.domain.model.farm

import java.time.LocalDate

/**
 * Origin classification distinguishing seeded sample demonstration records
 * from operator-created records.
 */
enum class RecordOrigin {
    SAMPLE,
    USER
}

/**
 * Closed finite set of agricultural growth stages.
 * Covers general crop life cycles without assuming uniform biology across all species.
 */
enum class GrowthStage(val label: String) {
    Unspecified("Unspecified"),
    Planned("Planned"),
    Germination("Germination"),
    Seedling("Seedling"),
    Vegetative("Vegetative"),
    Flowering("Flowering"),
    FruitSetting("Fruit Setting"),
    Ripening("Ripening"),
    Harvested("Harvested");

    companion object {
        fun fromString(value: String): GrowthStage {
            val trimmed = value.trim()
            return entries.firstOrNull { it.label.equals(trimmed, ignoreCase = true) || it.name.equals(trimmed, ignoreCase = true) }
                ?: when {
                    trimmed.contains("Plan", ignoreCase = true) -> Planned
                    trimmed.contains("Flower", ignoreCase = true) -> Flowering
                    trimmed.contains("Fruit", ignoreCase = true) -> FruitSetting
                    trimmed.contains("Seedling", ignoreCase = true) -> Seedling
                    trimmed.contains("Veg", ignoreCase = true) -> Vegetative
                    trimmed.contains("Harvest", ignoreCase = true) -> Harvested
                    trimmed.contains("Tassel", ignoreCase = true) -> Flowering
                    trimmed.contains("Pod", ignoreCase = true) -> FruitSetting
                    trimmed.contains("Ripen", ignoreCase = true) -> Ripening
                    else -> Unspecified
                }
        }
    }
}

/**
 * Health assessment status.
 * Strictly user-entered or default rule-based. Displays 'Insufficient data' if unspecified.
 */
enum class PlantHealthStatus(val label: String) {
    InsufficientData("Insufficient data"),
    Thriving("Thriving"),
    Nominal("Nominal"),
    NeedsAttention("Needs Attention"),
    Critical("Critical");

    companion object {
        fun fromString(value: String): PlantHealthStatus = when (value.trim().lowercase()) {
            "thriving" -> Thriving
            "nominal", "healthy", "active" -> Nominal
            "needs attention", "warning", "attention" -> NeedsAttention
            "critical", "error" -> Critical
            else -> InsufficientData
        }
    }
}

/**
 * Top-level Farm agricultural entity.
 * Area is canonically stored in square meters (m²).
 */
data class Farm(
    val id: String,
    val name: String,
    val description: String? = null,
    val locationDescription: String? = null,
    val areaSquareMeters: Double? = null,
    val notes: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val origin: RecordOrigin = RecordOrigin.USER
)

/**
 * Sub-partitioned agricultural Plot belonging to a parent Farm.
 * Area is canonically stored in square meters (m²).
 */
data class Plot(
    val id: String,
    val farmId: String,
    val name: String,
    val description: String? = null,
    val areaSquareMeters: Double? = null,
    val notes: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val origin: RecordOrigin = RecordOrigin.USER
)

/**
 * Planting (crop batch) planted in a specific Plot.
 * Preserves legacy crop management and harvest attributes (daysToHarvest).
 */
data class Planting(
    val id: String,
    val plotId: String,
    val cropName: String,
    val variety: String? = null,
    val plantingDate: LocalDate? = null,
    val growthStage: GrowthStage = GrowthStage.Unspecified,
    val userNotes: String? = null,
    val userHealthStatus: PlantHealthStatus = PlantHealthStatus.InsufficientData,
    val daysToHarvest: Int? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val origin: RecordOrigin = RecordOrigin.USER
)
