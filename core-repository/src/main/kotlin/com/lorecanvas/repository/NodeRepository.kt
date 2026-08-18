package com.lorecanvas.repository

import com.lorecanvas.common.LcResult
import com.lorecanvas.common.Logger
import com.lorecanvas.common.createLogger
import com.lorecanvas.domain.Node
import com.lorecanvas.events.EventBus
import com.lorecanvas.storage.CardStorage
import com.lorecanvas.storage.NodeStorage
import com.lorecanvas.storage.RelationshipStorage
import com.lorecanvas.storage.TimelineStorage
import com.lorecanvas.validation.NodeValidator
import com.lorecanvas.validation.ValidationResult
import java.io.File

/**
 * NodeRepository — Phase 3 "Node System" (PEP-001), implementing LCD-006
 * Chapter 6's Node Repository responsibilities (Create, Rename, Archive,
 * Restore, Delete, Retrieve, list) and LCD-009's Node workflows (Chapters
 * 6-8).
 *
 * Scoping decision: rather than a global singleton, a NodeRepository
 * instance is scoped to one already-open Project directory — constructed
 * when a project opens, discarded when it closes (see the `app` module's
 * `LoreCanvasApp.kt`). This keeps the dependency direction one-way (Node
 * layer doesn't need a live reference into [ProjectRepository]'s mutable
 * state) and automatically satisfies LCD-006 Chapter 12's "Parent Project
 * exists" validation check for Node creation — a NodeRepository simply
 * cannot exist without a project directory having been provided.
 *
 * Mutation pattern: matches [ProjectRepository]/[com.lorecanvas.domain.Project]
 * exactly — callers mutate a held [Node] via its own methods (`rename`,
 * `archive`, `addTag`, etc.), then call [save] to validate, persist, and
 * publish the event. This reuses the Phase 2 pattern rather than inventing
 * a new per-field-method API surface.
 */
class NodeRepository(
    private val projectDirectory: File,
    private val nodeStorage: NodeStorage,
    private val eventBus: EventBus,
    private val cardStorage: CardStorage? = null,
    private val relationshipStorage: RelationshipStorage? = null,
    private val timelineStorage: TimelineStorage? = null,
    private val logger: Logger = createLogger("NodeRepository")
) {

    /**
     * Create Node (LCD-009, Chapter 6). Validation here matches LCD-006
     * Chapter 12's "Node Creation" checks: name not empty, node type valid
     * (not empty), UUID unique (enforced by [NodeStorage.createNode]'s
     * ALREADY_EXISTS check), parent Project exists (structurally
     * guaranteed by this class's scoping — see class doc comment).
     */
    fun create(name: String, type: String, summary: String = ""): LcResult<Node, RepositoryError> {
        val validation = NodeValidator.validateForCreate(name, type)
        if (validation is ValidationResult.Invalid) {
            val reason = validation.errors.joinToString { it.message }
            logger.warn("Node creation failed validation", reason)
            return LcResult.fail(RepositoryError.ValidationFailed(reason))
        }

        val node = Node.create(name = name, type = type, summary = summary)

        return when (val storageResult = nodeStorage.createNode(projectDirectory, node)) {
            is LcResult.Fail -> {
                logger.error("Node creation failed to persist", storageResult.error.message)
                LcResult.fail(RepositoryError.Storage(storageResult.error))
            }
            is LcResult.Ok -> {
                logger.info("Node created", node.id)
                eventBus.publish(NodeEvent.NodeCreated(node.id))
                LcResult.ok(node)
            }
        }
    }

    /**
     * Save (LCD-009, Chapter 7 — Node Editing Workflow): "Select Node ->
     * Modify Fields -> Validation -> Repository Update -> Save State ->
     * Event Published." The caller has already mutated [node] via its own
     * methods; this re-validates (LCD-006 Chapter 2: "the Repository
     * never trusts external modifications") before persisting.
     */
    fun save(node: Node): LcResult<Unit, RepositoryError> {
        val validation = NodeValidator.validateForSave(node)
        if (validation is ValidationResult.Invalid) {
            val reason = validation.errors.joinToString { it.message }
            logger.warn("Node save rejected by validation", reason)
            return LcResult.fail(RepositoryError.ValidationFailed(reason))
        }

        return when (val storageResult = nodeStorage.saveNode(projectDirectory, node)) {
            is LcResult.Fail -> {
                logger.error("Node save failed", storageResult.error.message)
                LcResult.fail(RepositoryError.Storage(storageResult.error))
            }
            is LcResult.Ok -> {
                logger.info("Node saved", node.id)
                eventBus.publish(NodeEvent.NodeUpdated(node.id))
                LcResult.ok(Unit)
            }
        }
    }

    /**
     * Delete (LCD-009, Chapter 8): "Delete Request -> Dependency Check ->
     * Confirmation -> Transaction -> Delete -> Update References ->
     * Publish Event." [dependentsOf] now does the real check — Cards,
     * Relationships, and Timeline event references are all real entities
     * as of Phases 4-6, so this is no longer the placeholder it was in
     * Phase 3.
     *
     * This blocks deletion outright when dependents exist, rather than
     * cascading through a Confirmation step — a deliberately conservative
     * reading of LCD-009's workflow, since a silent cascading delete
     * across three entity types is a much easier way to lose data than a
     * blocked delete is to be annoying. A future `forceDelete` that
     * cascades after explicit UI confirmation is a reasonable follow-up,
     * not required for this to be a real (non-placeholder) implementation.
     *
     * Confirmation of the *node itself* (asking "are you sure?") remains a
     * UI-layer concern — callers still confirm before calling [delete].
     */
    fun delete(nodeId: String): LcResult<Unit, RepositoryError> {
        val blockers = dependentsOf(nodeId)
        if (blockers.isNotEmpty()) {
            return LcResult.fail(
                RepositoryError.ValidationFailed(
                    "Cannot delete: still referenced by ${blockers.joinToString()}."
                )
            )
        }

        return when (val storageResult = nodeStorage.deleteNode(projectDirectory, nodeId)) {
            is LcResult.Fail -> {
                logger.error("Node deletion failed", storageResult.error.message)
                LcResult.fail(RepositoryError.Storage(storageResult.error))
            }
            is LcResult.Ok -> {
                logger.info("Node deleted", nodeId)
                eventBus.publish(NodeEvent.NodeDeleted(nodeId))
                LcResult.ok(Unit)
            }
        }
    }

    /**
     * Real dependency check (LCD-009 Ch.8). Each dependency source is
     * optional (nullable constructor params) so this class stays testable
     * without wiring all three storages when a test only cares about Node
     * behavior — but in the running app, [com.lorecanvas.app.ui.LoreCanvasApp]
     * always provides all three, so deletion is always safe there.
     */
    fun dependentsOf(nodeId: String): List<String> {
        val blockers = mutableListOf<String>()

        cardStorage?.listCardsForNode(projectDirectory, nodeId)?.size?.let { count ->
            if (count > 0) blockers.add("$count card${if (count == 1) "" else "s"}")
        }

        relationshipStorage?.listRelationships(projectDirectory)?.count { it.involves(nodeId) }?.let { count ->
            if (count > 0) blockers.add("$count relationship${if (count == 1) "" else "s"}")
        }

        timelineStorage?.listTimelines(projectDirectory)?.sumOf { timeline ->
            timeline.events.count { event -> nodeId in event.relatedNodeIds }
        }?.let { count ->
            if (count > 0) blockers.add("$count timeline event reference${if (count == 1) "" else "s"}")
        }

        return blockers
    }

    fun list(): List<Node> = nodeStorage.listNodes(projectDirectory)

    fun get(nodeId: String): Node? = when (val result = nodeStorage.loadNode(projectDirectory, nodeId)) {
        is LcResult.Ok -> result.value
        is LcResult.Fail -> null
    }
}
