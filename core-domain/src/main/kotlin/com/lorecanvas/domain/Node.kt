package com.lorecanvas.domain

import com.lorecanvas.common.UuidService
import java.time.Instant

/**
 * Node lifecycle status (LCD-005, Chapter 7 — Node Lifecycle: "...Timeline
 * References -> Archive -> Delete"). Archiving is a soft, reversible state;
 * only an explicit delete is permanent.
 */
enum class NodeStatus { ACTIVE, ARCHIVED }

/**
 * Suggested Node Types (LCD-005, Chapter 6). This list is deliberately not
 * an enum — "Users may define additional Node Types without modifying the
 * application" — so [Node.type] stays a plain, user-extensible [String].
 * These are just the starting suggestions a picker UI can offer.
 */
object NodeTypes {
    val SUGGESTED: List<String> = listOf(
        "Character", "Kingdom", "City", "Village", "Continent", "Organization",
        "Faction", "Creature", "Species", "Religion", "Magic System",
        "Technology", "Artifact", "Historical Event"
    )
}

/**
 * Node — "the most important entity in LoreCanvas" (LCD-005, Chapter 5).
 * Every significant concept in a fictional world is represented as a Node;
 * detailed content belongs on Cards (Phase 4), not here — the Node itself
 * stays lightweight, matching its exact Core Attributes list from LCD-005:
 * ID, Name, Type, Summary, Status, Created Date, Modified Date, Tags.
 *
 * Deliberately has no `projectId` field: a Node's association with a
 * Project is implicit in which project directory it's stored under (see
 * `core-storage`'s `NodeFileStorage`), matching LCD-005's attribute list
 * exactly and how `Project` itself was built in Phase 1/2.
 */
class Node private constructor(
    id: String,
    createdAt: String,
    modifiedAt: String,
    initialName: String,
    initialType: String,
    initialSummary: String,
    initialStatus: NodeStatus,
    initialTags: List<String>
) : Entity(id = id, createdAt = createdAt, modifiedAt = modifiedAt) {

    var name: String = initialName
        private set

    var type: String = initialType
        private set

    var summary: String = initialSummary
        private set

    var status: NodeStatus = initialStatus
        private set

    private val mutableTags: MutableList<String> = initialTags.toMutableList()
    val tags: List<String> get() = mutableTags.toList()

    init {
        require(name.isNotBlank()) { "Node name must not be empty." }
        require(type.isNotBlank()) { "Node type must not be empty." }
    }

    fun rename(newName: String) {
        require(newName.isNotBlank()) { "Node name must not be empty." }
        name = newName.trim()
        touch()
    }

    fun changeType(newType: String) {
        require(newType.isNotBlank()) { "Node type must not be empty." }
        type = newType.trim()
        touch()
    }

    fun updateSummary(newSummary: String) {
        summary = newSummary
        touch()
    }

    /** LCD-005, Chapter 7 — reversible, unlike delete. */
    fun archive() {
        if (status != NodeStatus.ARCHIVED) {
            status = NodeStatus.ARCHIVED
            touch()
        }
    }

    fun restore() {
        if (status != NodeStatus.ACTIVE) {
            status = NodeStatus.ACTIVE
            touch()
        }
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
        /** Create Node (LCD-009, Chapter 6's "New Node -> Choose Type -> Enter Name -> Create"). */
        fun create(name: String, type: String, summary: String = "", tags: List<String> = emptyList()): Node {
            val now = Instant.now().toString()
            return Node(
                id = UuidService.generate(),
                createdAt = now,
                modifiedAt = now,
                initialName = name.trim(),
                initialType = type.trim(),
                initialSummary = summary,
                initialStatus = NodeStatus.ACTIVE,
                initialTags = tags
            )
        }

        /** Reconstructs a Node from persisted data (see `NodeSerializer`). */
        fun restore(
            id: String,
            createdAt: String,
            modifiedAt: String,
            name: String,
            type: String,
            summary: String,
            status: NodeStatus,
            tags: List<String>
        ): Node = Node(id, createdAt, modifiedAt, name.trim(), type.trim(), summary, status, tags)
    }
}
