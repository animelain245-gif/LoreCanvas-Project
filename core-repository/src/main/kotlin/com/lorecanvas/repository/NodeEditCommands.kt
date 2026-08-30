package com.lorecanvas.repository

import com.lorecanvas.commands.Command
import com.lorecanvas.commands.CompoundCommand
import com.lorecanvas.domain.Node
import com.lorecanvas.domain.NodeStatus

/**
 * Bridges [NodeEditorScreen]'s existing "edit fields live, Save once"
 * pattern onto the per-field Commands in [NodeCommands.kt], **without
 * changing either side**. This is the concrete answer to the UI
 * Integration milestone's "Determine which mutations should pass through
 * CommandHistory" for Node — see the class doc below for exactly why a
 * naive `commandHistory.execute(RenameNodeCommand(repo, node, newName))`
 * called at Save time does *not* work here.
 *
 * The screen already calls `node.rename(...)` etc. directly on every
 * keystroke, for instant UI feedback and an `isDirty` flag — well before
 * Save is ever pressed. By the time Save happens, `node.name` (etc.) is
 * already the *new* value. Every existing per-field Command (e.g.
 * [RenameNodeCommand]) captures its "previous" value from the live object
 * at construction/execute time — so constructing one at Save time would
 * capture `previousName = newName`, making undo silently do nothing.
 * Changing that capture behavior would touch already-verified Command
 * code (out of scope — "do not redesign the Command architecture").
 *
 * The fix lives here instead: for each field that actually changed,
 * revert the live object back to the pre-edit snapshot value *just before
 * constructing* that field's Command (so it captures the correct
 * "previous"), then hand all constructed Commands to a single
 * [CompoundCommand] so Save becomes exactly one undo step. When
 * [CompoundCommand.execute] runs (via [com.lorecanvas.commands.CommandHistory.execute]),
 * each sub-command re-applies its own "new" value forward and persists —
 * ending in exactly the state the user already saw on screen.
 */
object NodeEditCommands {

    /** Snapshot of a Node's editable fields, taken when the editor screen is entered. */
    data class Snapshot(
        val name: String,
        val type: String,
        val summary: String,
        val tags: List<String>,
        val status: NodeStatus,
        val isPinned: Boolean
    ) {
        companion object {
            fun of(node: Node): Snapshot = Snapshot(node.name, node.type, node.summary, node.tags, node.status, node.isPinned)
        }
    }

    /**
     * Returns a single [Command] representing everything that changed
     * since [snapshot] was taken, or null if nothing changed (Save should
     * then be a no-op — nothing to persist, nothing to push onto the undo
     * stack). Only ever wraps the *specific* fields that changed, per
     * "do not blindly wrap every repository call in a Command."
     */
    fun buildSaveCommand(nodeRepository: NodeRepository, node: Node, snapshot: Snapshot): Command? {
        val commands = mutableListOf<Command>()

        if (node.name != snapshot.name) {
            val newName = node.name
            node.rename(snapshot.name) // revert so the Command captures the correct "previous"
            commands.add(RenameNodeCommand(nodeRepository, node, newName))
        }
        if (node.type != snapshot.type) {
            val newType = node.type
            node.changeType(snapshot.type)
            commands.add(ChangeNodeTypeCommand(nodeRepository, node, newType))
        }
        if (node.summary != snapshot.summary) {
            val newSummary = node.summary
            node.updateSummary(snapshot.summary)
            commands.add(UpdateNodeSummaryCommand(nodeRepository, node, newSummary))
        }
        if (node.status != snapshot.status) {
            // ToggleNodeArchiveCommand captures wasArchived only at construction, so the revert must happen first here too.
            if (snapshot.status == NodeStatus.ARCHIVED) node.archive() else node.restore()
            commands.add(ToggleNodeArchiveCommand(nodeRepository, node))
        }
        if (node.isPinned != snapshot.isPinned) {
            node.togglePin() // revert
            commands.add(ToggleNodePinCommand(nodeRepository, node))
        }
        val addedTags = node.tags.filter { it !in snapshot.tags }
        for (tag in addedTags) {
            node.removeTag(tag) // revert this one tag so AddNodeTagCommand's own "was it already present" check is correct
            commands.add(AddNodeTagCommand(nodeRepository, node, tag))
        }

        return when (commands.size) {
            0 -> null
            1 -> commands[0]
            else -> CompoundCommand("Edit Node", commands)
        }
    }
}
