package com.lorecanvas.common

import java.util.UUID

/**
 * UUID Service (LCD-005, Chapter 8 — Identity Rules: "Every Node receives a
 * globally unique identifier (UUID) at creation. Names are not identifiers.")
 *
 * The single place in the codebase allowed to generate identity, so entity
 * construction never calls [UUID.randomUUID] directly.
 */
object UuidService {

    fun generate(): String = UUID.randomUUID().toString()

    fun isValid(value: String): Boolean =
        try {
            UUID.fromString(value)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
}
