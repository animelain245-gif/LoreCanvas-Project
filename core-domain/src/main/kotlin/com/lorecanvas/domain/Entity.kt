package com.lorecanvas.domain

import com.lorecanvas.common.UuidService
import java.time.Instant

/**
 * Entity — the base class every domain object extends.
 *
 * Implements two rules from LCD-005:
 *   - Chapter 8 (Identity Rules): every entity gets a UUID at creation;
 *     names are never identifiers.
 *   - Chapter 9 (Metadata): every major entity tracks created/modified
 *     timestamps.
 *
 * Entity intentionally knows nothing about storage, validation, or UI
 * (LCD-005, Chapter 2 — "Independence").
 */
abstract class Entity protected constructor(
    val id: String = UuidService.generate(),
    val createdAt: String = Instant.now().toString(),
    modifiedAt: String = createdAt
) {
    var modifiedAt: String = modifiedAt
        protected set

    /**
     * Subclasses call this whenever a mutable field changes, keeping the
     * modification timestamp accurate without every subclass reimplementing
     * timestamp bookkeeping.
     */
    protected fun touch() {
        modifiedAt = Instant.now().toString()
    }
}
