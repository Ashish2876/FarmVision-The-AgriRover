package com.farmvision.agrirover.domain.device.model

/**
 * Identifies the exact AST token type of a JSON field on the wire.
 */
enum class JsonTokenType {
    STRING,
    NUMBER,
    BOOLEAN,
    OBJECT,
    ARRAY
}

/**
 * Lossless representation of a JSON field on the wire.
 * Completely distinguishes:
 * 1. Key absent from payload (Absent)
 * 2. Explicit JSON null literal (ExplicitNull)
 * 3. Primitive or compound token with original AST token type and exact literal string (Present)
 */
sealed interface JsonRawField {
    /** The key was completely omitted from the enclosing JSON object. */
    data object Absent : JsonRawField

    /** The key was present in the JSON object with an explicit null literal (e.g. `"timestamp": null`). */
    data object ExplicitNull : JsonRawField

    /**
     * The key was present with a non-null JSON token.
     * Preserves both the original token type and raw character sequence from the wire.
     */
    data class Present(
        val tokenType: JsonTokenType,
        val rawValue: String
    ) : JsonRawField
}

/**
 * Deterministic quality classification of the wire timestamp.
 */
enum class TimestampQuality {
    /** Valid ISO-8601 UTC string or plausible numeric epoch millis token. */
    ValidUtc,

    /** Present token was a boolean, object, or array (strictly prohibited for timestamps). */
    TypeMismatch,

    /** String or number token that failed parsing or represents an invalid value. */
    Malformed,

    /** String token was empty (`""`). */
    EmptyString,

    /** Field was explicitly set to JSON null (`null`). */
    ExplicitNull,

    /** Key was completely omitted from the JSON payload. */
    Absent,

    /** Numeric 0 or small monotonic uptime counter (e.g. ESP32 boot millis). */
    ZeroUptimeSkew,

    /** Source timestamp parsed successfully but is > 30s ahead of local Android reception time. */
    FutureDrift
}

/**
 * Immutable domain model for telemetry timestamp provenance.
 */
data class TelemetryTimestamp(
    val sourceField: JsonRawField,
    val sourceTimestampEpochMillis: Long?,
    val receivedAtEpochMillis: Long,
    val timestampQuality: TimestampQuality
)

/**
 * Android Keystore hardware backing level.
 * Distinguishes verified StrongBox, TEE/Hardware-backed, Software-backed, and ephemeral RAM fallback.
 */
sealed interface KeystoreHardwareLevel {
    /** Verified dedicated StrongBox tamper-resistant hardware chip. */
    data object StrongBoxHardware : KeystoreHardwareLevel

    /** Verified TEE / Secure Hardware module (without asserting StrongBox on older APIs). */
    data object TeeHardware : KeystoreHardwareLevel

    /** Software-backed Android Keystore implementation. */
    data object Software : KeystoreHardwareLevel

    /** Ephemeral volatile RAM fallback when Keystore fails closed (never written to disk). */
    data object RamOnlyFallback : KeystoreHardwareLevel
}

