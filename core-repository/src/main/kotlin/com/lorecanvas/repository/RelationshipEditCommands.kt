package com.lorecanvas.repository

import com.lorecanvas.commands.Command
import com.lorecanvas.commands.CompoundCommand
import com.lorecanvas.domain.Relationship

/**
 * Relationship's counterpart to [NodeEditCommands] — see its class doc
 * for the full rationale. Only covers `type` and `description`, the two
 * fields [RelationshipEditorScreen] currently exposes as live-edited.
 * `addContext` deliberately has no diff/undo entry here, matching
 * [RelationshipCommands.kt]'s documented decision not to wrap it — so the
 * caller (the UI wiring) must still persist unconditionally on Save even
 * when this returns null, or a context-only edit would silently be lost.
 */
object RelationshipEditCommands {

    data class Snapshot(val type: String, val description: String) {
        companion object {
            fun of(relationship: Relationship): Snapshot = Snapshot(relationship.type, relationship.description)
        }
    }

    fun buildSaveCommand(relationshipRepository: RelationshipRepository, relationship: Relationship, snapshot: Snapshot): Command? {
        val commands = mutableListOf<Command>()

        if (relationship.type != snapshot.type) {
            val newType = relationship.type
            relationship.changeType(snapshot.type)
            commands.add(ChangeRelationshipTypeCommand(relationshipRepository, relationship, newType))
        }
        if (relationship.description != snapshot.description) {
            val newDescription = relationship.description
            relationship.updateDescription(snapshot.description)
            commands.add(UpdateRelationshipDescriptionCommand(relationshipRepository, relationship, newDescription))
        }

        return when (commands.size) {
            0 -> null
            1 -> commands[0]
            else -> CompoundCommand("Edit Relationship", commands)
        }
    }
}
