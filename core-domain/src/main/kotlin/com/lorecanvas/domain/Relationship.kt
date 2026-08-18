package com.lorecanvas.domain

import com.lorecanvas.common.UuidService
import java.time.Instant

enum class RelationshipDirection { DIRECTED, BIDIRECTIONAL }

object RelationshipTypes {
    val SUGGESTED: List<String> = listOf(
        "Parent", "Child", "Friend", "Enemy", "Leader", "Member",
        "Lives In", "Owns", "Created", "Controls", "Allied With", "Rival Of"
    )
}

/**
 * Relationship Context (LCD-005 Ch.12) — "relationships often change over
 * time... instead of deleting relationships, LoreCanvas preserves their
 * history." Embedded inside its owning [Relationship] rather than stored
 * as a separate file, since a context has no meaning outside its
 * relationship (mirrors how Timeline embeds its Events).
 */
data class RelationshipContext(
    val id: String,
    val startDate: String,
    val endDate: String?,
    val description: String,
    val timelineEventIds: List<String> = emptyList()
) {
    companion object {
        fun create(startDate: String, description: String, endDate: String? = null, timelineEventIds: List<String> = emptyList()) =
            RelationshipContext(UuidService.generate(), startDate, endDate, description, timelineEventIds)
    }
}

/**
 * Relationship — "explicit, structured connections between Nodes" (LCD-005
 * Ch.11), forming the backbone of the knowledge graph (LCD-006 Ch.8).
 */
class Relationship private constructor(
    id: String,
    createdAt: String,
    modifiedAt: String,
    val sourceNodeId: String,
    val targetNodeId: String,
    initialType: String,
    initialDirection: RelationshipDirection,
    initialDescription: String,
    initialStatus: String,
    initialContexts: List<RelationshipContext>
) : Entity(id = id, createdAt = createdAt, modifiedAt = modifiedAt) {

    var type: String = initialType
        private set

    var direction: RelationshipDirection = initialDirection
        private set

    var description: String = initialDescription
        private set

    var status: String = initialStatus
        private set

    private val mutableContexts: MutableList<RelationshipContext> = initialContexts.toMutableList()
    val contexts: List<RelationshipContext> get() = mutableContexts.toList()

    init {
        require(type.isNotBlank()) { "Relationship type must not be empty." }
        require(sourceNodeId.isNotBlank() && targetNodeId.isNotBlank()) { "Relationship must reference both nodes." }
    }

    fun changeType(newType: String) {
        require(newType.isNotBlank()) { "Relationship type must not be empty." }
        type = newType.trim()
        touch()
    }

    /**
     * True if [nodeId] is either endpoint. Every caller that needs to ask
     * "does this relationship touch this node" (Node deletion dependency
     * checks, the Node editor's relationship list, listForNode queries)
     * should go through this rather than re-deriving the same
     * source-or-target check independently — that duplication is exactly
     * how "bidirectional consistency" quietly drifts inconsistent.
     */
    fun involves(nodeId: String): Boolean = sourceNodeId == nodeId || targetNodeId == nodeId

    /**
     * The endpoint that *isn't* [nodeId]. For a [RelationshipDirection.BIDIRECTIONAL]
     * relationship this is symmetric by construction — it doesn't matter
     * which side is stored as "source" — since this always returns
     * whichever id isn't the one you already have.
     */
    fun otherNodeId(nodeId: String): String = if (sourceNodeId == nodeId) targetNodeId else sourceNodeId

    fun updateDescription(newDescription: String) {
        description = newDescription
        touch()
    }

    fun updateStatus(newStatus: String) {
        status = newStatus.ifBlank { "Active" }
        touch()
    }

    /** LCD-005 Ch.12 — records a new chapter of history rather than overwriting the last one. */
    fun addContext(context: RelationshipContext) {
        mutableContexts.add(context)
        touch()
    }

    companion object {
        fun create(
            sourceNodeId: String,
            targetNodeId: String,
            type: String,
            direction: RelationshipDirection = RelationshipDirection.DIRECTED,
            description: String = ""
        ): Relationship {
            val now = Instant.now().toString()
            return Relationship(
                UuidService.generate(), now, now, sourceNodeId, targetNodeId,
                type.trim(), direction, description, "Active", emptyList()
            )
        }

        fun restore(
            id: String,
            createdAt: String,
            modifiedAt: String,
            sourceNodeId: String,
            targetNodeId: String,
            type: String,
            direction: RelationshipDirection,
            description: String,
            status: String,
            contexts: List<RelationshipContext>
        ): Relationship = Relationship(
            id, createdAt, modifiedAt, sourceNodeId, targetNodeId, type.trim(), direction, description, status, contexts
        )
    }
}
