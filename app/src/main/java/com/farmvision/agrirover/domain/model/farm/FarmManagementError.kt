package com.farmvision.agrirover.domain.model.farm

/**
 * Domain-level typed errors returned by repository and validation rules.
 * Never exposes raw exception messages to the presentation layer.
 */
sealed interface FarmManagementError {
    data class BlankName(val fieldName: String) : FarmManagementError
    data class NameTooLong(val fieldName: String, val maxLen: Int) : FarmManagementError
    data class DuplicateName(val entityType: String, val name: String) : FarmManagementError
    data class InvalidArea(val reason: String) : FarmManagementError
    data class ParentNotFound(val parentType: String, val id: String) : FarmManagementError
    data class EntityNotFound(val entityType: String, val id: String) : FarmManagementError
    data class NotesTooLong(val maxLen: Int) : FarmManagementError
    data class VarietyTooLong(val maxLen: Int) : FarmManagementError
    data class StorageIoError(val message: String) : FarmManagementError
}

/**
 * Monadic typed operation result container for domain and repository writes.
 */
sealed interface FarmOperationResult<out T> {
    data class Success<T>(val data: T) : FarmOperationResult<T>
    data class Failure(val error: FarmManagementError) : FarmOperationResult<Nothing>

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }

    fun errorOrNull(): FarmManagementError? = when (this) {
        is Success -> null
        is Failure -> error
    }
}

/**
 * Detailed report of atomic cascading deletion counts.
 */
data class DeletionReport(
    val deletedFarmsCount: Int = 0,
    val deletedPlotsCount: Int = 0,
    val deletedPlantingsCount: Int = 0
) {
    val totalDeleted: Int
        get() = deletedFarmsCount + deletedPlotsCount + deletedPlantingsCount
}
