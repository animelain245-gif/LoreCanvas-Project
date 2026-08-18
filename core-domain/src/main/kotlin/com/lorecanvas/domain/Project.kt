package com.lorecanvas.domain

import com.lorecanvas.common.UuidService
import java.time.Instant

/**
 * Project — the root container for a LoreCanvas world (LCD-005, Chapter 4).
 *
 * "A Project acts as the root container for all other entities... Deleting
 * a Project removes every entity contained within it."
 *
 * Phase 1 implements the Project shell only (identity + metadata), matching
 * PEP-001's Phase 1 scope ("Domain base classes... nothing visual yet").
 * Node/Card/Relationship/Timeline/Template collections are introduced by
 * their own phases (2-6) rather than stubbed here as dead weight.
 */
class Project private constructor(
    id: String,
    createdAt: String,
    modifiedAt: String,
    initialName: String,
    initialDescription: String,
    initialVersion: String,
    initialAuthor: String,
    initialTags: List<String>,
    initialSettings: Map<String, Any?>
) : Entity(id = id, createdAt = createdAt, modifiedAt = modifiedAt) {

    var name: String = initialName
        private set

    var description: String = initialDescription
        private set

    var version: String = initialVersion
        private set

    var author: String = initialAuthor
        private set

    private val mutableTags: MutableList<String> = initialTags.toMutableList()
    val tags: List<String> get() = mutableTags.toList()

    private val mutableSettings: MutableMap<String, Any?> = initialSettings.toMutableMap()
    val settings: Map<String, Any?> get() = mutableSettings.toMap()

    init {
        require(name.isNotBlank()) { "Project name must not be empty." }
    }

    fun rename(newName: String) {
        require(newName.isNotBlank()) { "Project name must not be empty." }
        name = newName.trim()
        touch()
    }

    fun updateDescription(newDescription: String) {
        description = newDescription
        touch()
    }

    fun addTag(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isEmpty()) return
        if (!mutableTags.contains(trimmed)) {
            mutableTags.add(trimmed)
            touch()
        }
    }

    fun removeTag(tag: String) {
        if (mutableTags.remove(tag)) {
            touch()
        }
    }

    companion object {
        /** Creates a brand-new Project (Phase 2's "Create Project" workflow will call this). */
        fun create(
            name: String,
            description: String = "",
            version: String = "1.0",
            author: String = "",
            tags: List<String> = emptyList(),
            settings: Map<String, Any?> = emptyMap()
        ): Project {
            val now = Instant.now().toString()
            return Project(
                id = UuidService.generate(),
                createdAt = now,
                modifiedAt = now,
                initialName = name.trim(),
                initialDescription = description,
                initialVersion = version,
                initialAuthor = author,
                initialTags = tags,
                initialSettings = settings
            )
        }

        /** Reconstructs a Project from persisted data (Phase 2's "Open Project" workflow will call this). */
        fun restore(
            id: String,
            createdAt: String,
            modifiedAt: String,
            name: String,
            description: String,
            version: String,
            author: String,
            tags: List<String>,
            settings: Map<String, Any?>
        ): Project = Project(
            id = id,
            createdAt = createdAt,
            modifiedAt = modifiedAt,
            initialName = name.trim(),
            initialDescription = description,
            initialVersion = version,
            initialAuthor = author,
            initialTags = tags,
            initialSettings = settings
        )
    }
}
