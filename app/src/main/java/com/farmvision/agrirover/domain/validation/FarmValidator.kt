package com.farmvision.agrirover.domain.validation

import com.farmvision.agrirover.domain.model.farm.AreaUnit
import com.farmvision.agrirover.domain.model.farm.FarmManagementError

/**
 * Domain-level validation utilities enforcing data integrity, character bounds,
 * whitespace normalization, area validity, and referential non-orphan constraints.
 */
object FarmValidator {

    const val MAX_NAME_LENGTH = 60
    const val MAX_VARIETY_LENGTH = 60
    const val MAX_NOTES_LENGTH = 500

    /**
     * Normalizes text by trimming leading/trailing whitespace and collapsing
     * internal consecutive whitespace sequences into a single space.
     */
    fun normalizeWhitespace(input: String): String {
        return input.trim().replace("\\s+".toRegex(), " ")
    }

    /**
     * Validates a required name field.
     */
    fun validateName(fieldName: String, name: String): FarmManagementError? {
        val normalized = normalizeWhitespace(name)
        return when {
            normalized.isEmpty() -> FarmManagementError.BlankName(fieldName)
            normalized.length > MAX_NAME_LENGTH -> FarmManagementError.NameTooLong(fieldName, MAX_NAME_LENGTH)
            else -> null
        }
    }

    /**
     * Validates an optional variety string.
     */
    fun validateVariety(variety: String?): FarmManagementError? {
        if (variety == null) return null
        val normalized = normalizeWhitespace(variety)
        return if (normalized.length > MAX_VARIETY_LENGTH) {
            FarmManagementError.VarietyTooLong(MAX_VARIETY_LENGTH)
        } else {
            null
        }
    }

    /**
     * Validates an optional notes string.
     */
    fun validateNotes(notes: String?): FarmManagementError? {
        if (notes == null) return null
        return if (notes.length > MAX_NOTES_LENGTH) {
            FarmManagementError.NotesTooLong(MAX_NOTES_LENGTH)
        } else {
            null
        }
    }

    /**
     * Validates an optional area in square meters.
     * Must be finite, strictly positive, and <= MAX_SANITY_AREA_SQ_METERS.
     */
    fun validateAreaSquareMeters(areaSquareMeters: Double?): FarmManagementError? {
        if (areaSquareMeters == null) return null
        return when {
            areaSquareMeters.isNaN() -> FarmManagementError.InvalidArea("Area cannot be NaN")
            areaSquareMeters.isInfinite() -> FarmManagementError.InvalidArea("Area cannot be infinite")
            areaSquareMeters <= 0.0 -> FarmManagementError.InvalidArea("Area must be greater than zero")
            areaSquareMeters > AreaUnit.MAX_SANITY_AREA_SQ_METERS -> {
                FarmManagementError.InvalidArea(
                    "Area exceeds maximum sanity limit of 10,000 hectares (${AreaUnit.MAX_SANITY_AREA_SQ_METERS} m²)"
                )
            }
            else -> null
        }
    }
}
