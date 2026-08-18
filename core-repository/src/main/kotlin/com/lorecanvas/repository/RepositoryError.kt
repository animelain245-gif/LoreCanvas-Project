package com.lorecanvas.repository

import com.lorecanvas.storage.StorageError

/**
 * Unified error type for [ProjectRepository]'s public operations. Wraps the
 * two failure sources a Repository call can hit (LCD-006, Chapter 12's
 * validation pipeline, and Storage per LCD-007) plus repository-level
 * state errors (e.g. calling save() with nothing open), so callers (the UI
 * layer) have one type to handle rather than three.
 */
sealed class RepositoryError {
    data class ValidationFailed(val reason: String) : RepositoryError()
    data class Storage(val error: StorageError) : RepositoryError()
    object NoProjectOpen : RepositoryError()

    fun userMessage(): String = when (this) {
        is ValidationFailed -> reason
        is Storage -> error.message
        is NoProjectOpen -> "No project is currently open."
    }
}
