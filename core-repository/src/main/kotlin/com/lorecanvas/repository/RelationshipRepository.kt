package com.lorecanvas.repository

import com.lorecanvas.common.LcResult
import com.lorecanvas.common.Logger
import com.lorecanvas.common.createLogger
import com.lorecanvas.domain.Relationship
import com.lorecanvas.domain.RelationshipContext
import com.lorecanvas.domain.RelationshipDirection
import com.lorecanvas.events.EventBus
import com.lorecanvas.storage.NodeStorage
import com.lorecanvas.storage.RelationshipStorage
import com.lorecanvas.validation.RelationshipValidator
import com.lorecanvas.validation.ValidationResult
import java.io.File

/**
 * RelationshipRepository — Phase 5 "Relationship System" (PEP-001),
 * implementing LCD-006 Ch.8 and LCD-009 Ch.11. Validation, per LCD-006's
 * "Validation Examples": rejects relationships pointing to Nodes that
 * don't exist ("references to unknown UUIDs" / "pointing to deleted
 * Nodes"). Duplicate relationships and circular references are
 * deliberately *not* blocked — LCD-006 only prohibits them "where
 * required," and nothing in LCD-005's Relationship Types list (Friend,
 * Rival Of, etc.) suggests two Nodes can only ever have one relationship
 * between them.
 */
class RelationshipRepository(
    private val projectDirectory: File,
    private val relationshipStorage: RelationshipStorage,
    private val nodeStorage: NodeStorage,
    private val eventBus: EventBus,
    private val logger: Logger = createLogger("RelationshipRepository")
) {

    fun create(
        sourceNodeId: String,
        targetNodeId: String,
        type: String,
        direction: RelationshipDirection = RelationshipDirection.DIRECTED,
        description: String = ""
    ): LcResult<Relationship, RepositoryError> {
        val typeValidation = RelationshipValidator.validateForCreate(type)
        if (typeValidation is ValidationResult.Invalid) {
            return LcResult.fail(RepositoryError.ValidationFailed(typeValidation.errors.joinToString { it.message }))
        }
        val endpointValidation = RelationshipValidator.validateEndpoints(sourceNodeId, targetNodeId) { id ->
            nodeStorage.nodeExists(projectDirectory, id)
        }
        if (endpointValidation is ValidationResult.Invalid) {
            return LcResult.fail(RepositoryError.ValidationFailed(endpointValidation.errors.joinToString { it.message }))
        }

        val relationship = Relationship.create(sourceNodeId, targetNodeId, type, direction, description)
        return when (val result = relationshipStorage.createRelationship(projectDirectory, relationship)) {
            is LcResult.Fail -> LcResult.fail(RepositoryError.Storage(result.error))
            is LcResult.Ok -> {
                logger.info("Relationship created", relationship.id)
                eventBus.publish(RelationshipEvent.RelationshipCreated(relationship.id))
                LcResult.ok(relationship)
            }
        }
    }

    fun save(relationship: Relationship): LcResult<Unit, RepositoryError> {
        val validation = RelationshipValidator.validateForSave(relationship)
        if (validation is ValidationResult.Invalid) {
            return LcResult.fail(RepositoryError.ValidationFailed(validation.errors.joinToString { it.message }))
        }
        return when (val result = relationshipStorage.saveRelationship(projectDirectory, relationship)) {
            is LcResult.Fail -> LcResult.fail(RepositoryError.Storage(result.error))
            is LcResult.Ok -> {
                eventBus.publish(RelationshipEvent.RelationshipUpdated(relationship.id))
                LcResult.ok(Unit)
            }
        }
    }

    /** LCD-005 Ch.12 — adds a new RelationshipContext entry (history), then persists. */
    fun addContext(relationship: Relationship, context: RelationshipContext): LcResult<Unit, RepositoryError> {
        relationship.addContext(context)
        return save(relationship)
    }

    fun delete(relationshipId: String): LcResult<Unit, RepositoryError> =
        when (val result = relationshipStorage.deleteRelationship(projectDirectory, relationshipId)) {
            is LcResult.Fail -> LcResult.fail(RepositoryError.Storage(result.error))
            is LcResult.Ok -> {
                eventBus.publish(RelationshipEvent.RelationshipDeleted(relationshipId))
                LcResult.ok(Unit)
            }
        }

    /**
     * Restore (redo-of-create support for [CreateRelationshipCommand]) —
     * see [NodeRepository.restore]'s doc comment for the full rationale.
     * Re-inserts an already-constructed [Relationship] at its existing
     * id, rather than minting a fresh one ([create]) or requiring the id
     * already exist ([save]).
     */
    fun restore(relationship: Relationship): LcResult<Unit, RepositoryError> =
        when (val result = relationshipStorage.createRelationship(projectDirectory, relationship)) {
            is LcResult.Fail -> LcResult.fail(RepositoryError.Storage(result.error))
            is LcResult.Ok -> {
                logger.info("Relationship restored", relationship.id)
                eventBus.publish(RelationshipEvent.RelationshipCreated(relationship.id))
                LcResult.ok(Unit)
            }
        }

    fun list(): List<Relationship> = relationshipStorage.listRelationships(projectDirectory)

    fun listForNode(nodeId: String): List<Relationship> = list().filter { it.involves(nodeId) }

    fun get(relationshipId: String): Relationship? =
        (relationshipStorage.loadRelationship(projectDirectory, relationshipId) as? LcResult.Ok)?.value
}
