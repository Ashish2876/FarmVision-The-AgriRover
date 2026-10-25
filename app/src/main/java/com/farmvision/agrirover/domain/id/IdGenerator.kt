package com.farmvision.agrirover.domain.id

import java.util.UUID

/**
 * Injectable ID generation contract to decouple domain and repository layers
 * from static UUID generation and allow deterministic IDs in unit tests.
 */
interface IdGenerator {
    fun generateId(prefix: String = ""): String
}

/**
 * Default runtime implementation utilizing random UUIDs.
 */
class UuidGenerator : IdGenerator {
    override fun generateId(prefix: String): String {
        val raw = UUID.randomUUID().toString()
        return if (prefix.isNotBlank()) "$prefix-$raw" else raw
    }
}

/**
 * Deterministic sequential ID generator for predictable unit testing.
 */
class SequentialIdGenerator(private var counter: Int = 1) : IdGenerator {
    override fun generateId(prefix: String): String {
        val current = counter++
        return if (prefix.isNotBlank()) "$prefix-$current" else current.toString()
    }

    fun reset(startValue: Int = 1) {
        counter = startValue
    }
}
