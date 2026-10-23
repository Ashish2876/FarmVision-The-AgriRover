package com.farmvision.agrirover.data.time

/**
 * Pluggable time provider abstraction for deterministic telemetry generation,
 * staleness threshold evaluation, and reproducible unit testing.
 */
interface TimeProvider {
    /** Returns the current epoch timestamp in milliseconds. */
    fun nowEpochMillis(): Long

    /** Returns formatted relative or human-readable timestamp representation. */
    fun formatRelativeTime(timestampMillis: Long): String
}

/**
 * Standard system clock time provider for default application runtime.
 */
class SystemTimeProvider : TimeProvider {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()

    override fun formatRelativeTime(timestampMillis: Long): String {
        val diffSeconds = (System.currentTimeMillis() - timestampMillis) / 1000
        return when {
            diffSeconds < 5 -> "Just now"
            diffSeconds < 60 -> "${diffSeconds}s ago"
            diffSeconds < 3600 -> "${diffSeconds / 60}m ago"
            diffSeconds < 86400 -> "${diffSeconds / 3600}h ago"
            else -> "${diffSeconds / 86400}d ago"
        }
    }
}

/**
 * Deterministic test time provider allowing clock control in unit test suites.
 */
class FakeTimeProvider(
    private var currentMillis: Long = 1_700_000_000_000L
) : TimeProvider {
    override fun nowEpochMillis(): Long = currentMillis

    fun setTime(millis: Long) {
        currentMillis = millis
    }

    fun advanceMillis(millis: Long) {
        currentMillis += millis
    }

    override fun formatRelativeTime(timestampMillis: Long): String {
        val diffSeconds = (currentMillis - timestampMillis) / 1000
        return when {
            diffSeconds < 5 -> "Just now"
            diffSeconds < 60 -> "${diffSeconds}s ago"
            diffSeconds < 3600 -> "${diffSeconds / 60}m ago"
            diffSeconds < 86400 -> "${diffSeconds / 3600}h ago"
            else -> "${diffSeconds / 86400}d ago"
        }
    }
}
