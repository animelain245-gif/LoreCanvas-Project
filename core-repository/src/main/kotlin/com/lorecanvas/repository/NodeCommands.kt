package com.lorecanvas.repository

import com.lorecanvas.commands.Command
import com.lorecanvas.domain.Node

/**
 * Node Commands — the first real use of [com.lorecanvas.commands.Command]/
 * [com.lorecanvas.commands.CommandHistory] since they were built in Phase
 * 1. Every reversible Node edit becomes a Command so it's undoable by
 * construction (LCD-009 Ch.18: "Undo Request -> History Stack -> Restore
 * Previous State -> Repository -> Publish Event -> Refresh UI").
 *
 * Scope note: Create/Rename/ChangeType/UpdateSummary/AddTag/Archive are
 * all cleanly reversible — the domain object retains (or the Command
 * captures) enough state to restore exactly. **Delete is deliberately not
 * wrapped in a Command.** [Node.create] always mints a fresh UUID, so
 * there is no "restore with the original id" operation to undo into —
 * faking one would either silently change the restored Node's identity
 * (breaking any Card/Relationship/Timeline reference that pointed at the
 * old id) or require a new Storage capability this pass didn't build.
 * This matches LCD-005 Ch.7's own lifecycle distinction: Archive is the
 * reversible "soft delete," Delete is intentionally the final, permanent
 * step — Archive already gives users an undo-equivalent for "removing" a
 * Node, without a Command needing to fake identity-preserving resurrection.
 */
class CreateNodeCommand(
    private val nodeRepository: NodeRepository,
    private val name: String,
    private val type: String,
    private val summary: String = ""
) : Command {
    override val label: String = "Create Node"
    var createdNode: Node? = null
        private set

    /**
     * [CommandHistory.redo] re-invokes [execute] rather than having a
     * separate "redo" path — correct for every other Command in this
     * file, since renaming/re-tagging/etc. are naturally idempotent to
     * repeat. Create is the one exception: [NodeRepository.create] always
     * mints a *fresh* UUID via [Node.create], so calling it a second time
     * on redo would silently produce a different Node than the one undo
     * just deleted, orphaning any UI reference to the original. This flag
     * makes redo re-insert the *same* Node object via [NodeRepository.restore]
     * instead of minting a new one. (Plain [NodeRepository.save] won't
     * work here either — it requires the id already exist in storage,
     * which it won't right after undo deleted it.)
     */
    private var hasCreatedOnce = false

    override fun execute() {
        if (!hasCreatedOnce) {
            val result = nodeRepository.create(name, type, summary)
            if (result is com.lorecanvas.common.LcResult.Ok) {
                createdNode = result.value
                hasCreatedOnce = true
            }
        } else {
            createdNode?.let { nodeRepository.restore(it) }
        }
    }

    /** Undoing a create removes the Node — the one case where this module does call delete(), on a Node this same command just made. */
    override fun undo() {
        createdNode?.let { nodeRepository.delete(it.id) }
    }
}

class RenameNodeCommand(private val nodeRepository: NodeRepository, private val node: Node, private val newName: String) : Command {
    override val label: String = "Rename Node"
    private var previousName: String = node.name

    override fun execute() {
        previousName = node.name
        node.rename(newName)
        nodeRepository.save(node)
    }

    override fun undo() {
        node.rename(previousName)
        nodeRepository.save(node)
    }
}

class ChangeNodeTypeCommand(private val nodeRepository: NodeRepository, private val node: Node, private val newType: String) : Command {
    override val label: String = "Change Node Type"
    private var previousType: String = node.type

    override fun execute() {
        previousType = node.type
        node.changeType(newType)
        nodeRepository.save(node)
    }

    override fun undo() {
        node.changeType(previousType)
        nodeRepository.save(node)
    }
}

class UpdateNodeSummaryCommand(private val nodeRepository: NodeRepository, private val node: Node, private val newSummary: String) : Command {
    override val label: String = "Edit Node Summary"
    private var previousSummary: String = node.summary

    override fun execute() {
        previousSummary = node.summary
        node.updateSummary(newSummary)
        nodeRepository.save(node)
    }

    override fun undo() {
        node.updateSummary(previousSummary)
        nodeRepository.save(node)
    }
}

class AddNodeTagCommand(private val nodeRepository: NodeRepository, private val node: Node, private val tag: String) : Command {
    override val label: String = "Add Tag"
    private var actuallyAdded: Boolean = false

    override fun execute() {
        actuallyAdded = !node.tags.contains(tag.trim())
        node.addTag(tag)
        nodeRepository.save(node)
    }

    override fun undo() {
        if (actuallyAdded) {
            node.removeTag(tag.trim())
            nodeRepository.save(node)
        }
    }
}

class ToggleNodeArchiveCommand(private val nodeRepository: NodeRepository, private val node: Node) : Command {
    override val label: String = if (node.status == com.lorecanvas.domain.NodeStatus.ARCHIVED) "Restore Node" else "Archive Node"
    private val wasArchived = node.status == com.lorecanvas.domain.NodeStatus.ARCHIVED

    override fun execute() {
        if (wasArchived) node.restore() else node.archive()
        nodeRepository.save(node)
    }

    override fun undo() {
        if (wasArchived) node.archive() else node.restore()
        nodeRepository.save(node)
    }
}
