package com.lorecanvas.storage

import com.lorecanvas.common.LcResult

/**
 * Storage skeleton (PEP-001, Phase 1).
 *
 * Phase 1 scope is the *interface* only — PEP-001 lists "Storage format" as
 * a Phase 2 ("Project System") deliverable, not Phase 1. This keeps the
 * Repository layer (which depends on this interface) buildable and
 * testable now against an in-memory fake, while the real implementation
 * (Room database, per the Android architecture decision) is added when
 * Phase 2 begins — without Repository code changing at all.
 *
 * Kept synchronous for now rather than `suspend`, to avoid pulling in a
 * kotlinx-coroutines dependency before Phase 2 actually needs real,
 * asynchronous disk I/O. Room's DAOs are suspend-friendly, so this
 * interface's methods can become `suspend fun` in Phase 2 without changing
 * their shape.
 */
enum class StorageErrorType { NOT_FOUND, IO_ERROR, CORRUPT_DATA, ALREADY_EXISTS }

data class StorageError(val type: StorageErrorType, val message: String)

interface Storage {
    fun <T> read(key: String, deserialize: (String) -> T): LcResult<T, StorageError>
    fun write(key: String, serialized: String): LcResult<Unit, StorageError>
    fun delete(key: String): LcResult<Unit, StorageError>
    fun exists(key: String): Boolean
}

/**
 * A simple in-memory implementation used for unit tests and for the Phase 1
 * shell (which has no real project data yet). Not suitable for production —
 * nothing here survives an app restart.
 */
class InMemoryStorage : Storage {
    private val store = mutableMapOf<String, String>()

    override fun <T> read(key: String, deserialize: (String) -> T): LcResult<T, StorageError> {
        val raw = store[key]
            ?: return LcResult.fail(StorageError(StorageErrorType.NOT_FOUND, "No value stored for key '$key'."))
        return try {
            LcResult.ok(deserialize(raw))
        } catch (e: Exception) {
            LcResult.fail(StorageError(StorageErrorType.CORRUPT_DATA, e.message ?: "Failed to deserialize value."))
        }
    }

    override fun write(key: String, serialized: String): LcResult<Unit, StorageError> {
        store[key] = serialized
        return LcResult.ok(Unit)
    }

    override fun delete(key: String): LcResult<Unit, StorageError> {
        store.remove(key)
        return LcResult.ok(Unit)
    }

    override fun exists(key: String): Boolean = store.containsKey(key)
}
