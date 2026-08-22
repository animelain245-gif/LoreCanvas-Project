package com.lorecanvas.repository

import com.lorecanvas.commands.Command
import com.lorecanvas.domain.Relationship
import com.lorecanvas.domain.RelationshipDirection

/**
 * Relationship Commands — the Relationship-system counterpart to
 * [NodeCommands] and [CardCommands], same undo/redo pattern (LCD-009
 * Ch.18).
 *
 * Scope note: Create/ChangeType/UpdateDescription/UpdateStatus are
 * cleanly reversible the same way as the other Command sets. **Two
 * operations are deliberately left unwrapped, both for reasons already
 * established by [NodeCommands]:**
 *
 * - **Delete** — no "restore with the original id" operation exists
 *   ([Relationship.create] always mints a fresh UUID), so undo would have
 *   to fake identity-preserving resurrection or require a new Storage
 *   capability this pass didn't build.
 * - **Add Context** — LCD-005 Ch.12 is explicit that "instead of deleting
 *   relationships, LoreCanvas preserves their history" by adding a new
 *   [com.lorecanvas.domain.RelationshipContext] entry rather than
 *   overwriting the last one. Undoing that would need a
 *   `removeContext(id)` domain capability that doesn't exist yet — adding
 *   one just to make this Command reversible would be scope creep beyond
 *   what this pass built, exactly the same call [NodeCommands] made for
 *   Delete.
 */
class CreateRelationshipCommand(
    private val relationshipRepository: RelationshipRepository,
    private val sourceNodeId: String,
    private val targetNodeId: String,
    private val type: String,
    private val direction: RelationshipDirection = RelationshipDirection.DIRECTED,
    private val description: String = ""
) : Command {
    override val label: String = "Create Relationship"
    var createdRelationship: Relationship? = null
        private set
    var lastError: RepositoryError? = null
        private set

    /** See [CreateNodeCommand]'s doc comment on this same field — redo must re-insert the same Relationship via [RelationshipRepository.restore], not mint a new UUID or call [RelationshipRepository.save] (which requires the id already exist). */
    private var hasCreatedOnce = false

    override fun execute() {
        if (!hasCreatedOnce) {
            val result = relationshipRepository.create(sourceNodeId, targetNodeId, type, direction, description)
            when (result) {
                is com.lorecanvas.common.LcResult.Ok -> {
                    createdRelationship = result.value
                    hasCreatedOnce = true
                }
                is com.lorecanvas.common.LcResult.Fail -> lastError = result.error
            }
        } else {
            createdRelationship?.let { relationshipRepository.restore(it) }
        }
    }

    /** Undoing a create removes the Relationship — the one case where this module does call delete(), on a Relationship this same command just made. */
    override fun undo() {
        createdRelationship?.let { relationshipRepository.delete(it.id) }
    }
}

class ChangeRelationshipTypeCommand(
    private val relationshipRepository: RelationshipRepository,
    private val relationship: Relationship,
    private val newType: String
) : Command {
    override val label: String = "Change Relationship Type"
    private var previousType: String = relationship.type

    override fun execute() {
        previousType = relationship.type
        relationship.changeType(newType)
        relationshipRepository.save(relationship)
    }

    override fun undo() {
        relationship.changeType(previousType)
        relationshipRepository.save(relationship)
    }
}

class UpdateRelationshipDescriptionCommand(
    private val relationshipRepository: RelationshipRepository,
    private val relationship: Relationship,
    private val newDescription: String
) : Command {
    override val label: String = "Edit Relationship Description"
    private var previousDescription: String = relationship.description

    override fun execute() {
        previousDescription = relationship.description
        relationship.updateDescription(newDescription)
        relationshipRepository.save(relationship)
    }

    override fun undo() {
        relationship.updateDescription(previousDescription)
        relationshipRepository.save(relationship)
    }
}

class UpdateRelationshipStatusCommand(
    private val relationshipRepository: RelationshipRepository,
    private val relationship: Relationship,
    private val newStatus: String
) : Command {
    override val label: String = "Change Relationship Status"
    private var previousStatus: String = relationship.status

    override fun execute() {
        previousStatus = relationship.status
        relationship.updateStatus(newStatus)
        relationshipRepository.save(relationship)
    }

    override fun undo() {
        relationship.updateStatus(previousStatus)
        relationshipRepository.save(relationship)
    }
}
