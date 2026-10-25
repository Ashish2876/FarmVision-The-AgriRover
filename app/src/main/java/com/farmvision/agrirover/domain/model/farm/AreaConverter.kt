package com.farmvision.agrirover.domain.model.farm

import java.util.Locale

/**
 * Standard measurement units for agricultural field and plot area.
 * Canonical storage unit is square meters (m²).
 */
enum class AreaUnit(val symbol: String, val label: String) {
    SquareMeters("m²", "Square Meters (m²)"),
    Hectares("ha", "Hectares (ha)"),
    Acres("ac", "Acres (ac)");

    companion object {
        const val SQ_METERS_PER_HECTARE = 10_000.0
        // International acre standard definition: 1 acre = 4046.8564224 m²
        const val SQ_METERS_PER_ACRE = 4046.8564224
        // Documented sanity upper bound: 10,000 hectares = 100,000,000 m²
        const val MAX_SANITY_AREA_SQ_METERS = 100_000_000.0

        fun fromSymbol(symbol: String): AreaUnit = when (symbol.trim().lowercase()) {
            "ha", "hectare", "hectares" -> Hectares
            "ac", "acre", "acres" -> Acres
            else -> SquareMeters
        }
    }

    /**
     * Converts a value in this unit to the canonical unit: square meters (m²).
     */
    fun toSquareMeters(value: Double): Double = when (this) {
        SquareMeters -> value
        Hectares -> value * SQ_METERS_PER_HECTARE
        Acres -> value * SQ_METERS_PER_ACRE
    }

    /**
     * Converts a canonical square-meter value to this display unit.
     */
    fun fromSquareMeters(sqMeters: Double): Double = when (this) {
        SquareMeters -> sqMeters
        Hectares -> sqMeters / SQ_METERS_PER_HECTARE
        Acres -> sqMeters / SQ_METERS_PER_ACRE
    }

    /**
     * Formats a canonical square-meter value converted to this unit with symbol.
     */
    fun format(sqMeters: Double): String {
        val converted = fromSquareMeters(sqMeters)
        return String.format(Locale.US, "%.2f %s", converted, symbol)
    }

    /**
     * Formats the converted value only without the symbol.
     */
    fun formatValueOnly(sqMeters: Double): String {
        val converted = fromSquareMeters(sqMeters)
        return String.format(Locale.US, "%.2f", converted)
    }
}
