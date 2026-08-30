package com.lorecanvas.repository

import com.lorecanvas.commands.Command
import com.lorecanvas.common.LcResult
import com.lorecanvas.domain.Card
import com.lorecanvas.domain.CardTypes
import com.lorecanvas.domain.Node
import com.lorecanvas.domain.NodeTypes

/**
 * Story-specific Commands (Phase 9C).
 *
 * Implements the "Writing-First" requirement and "Cascading Delete" logic.
 * These commands wrap standard Node/Card operations into atomic units
 * that preserve Story hierarchy (Story -> Chapter -> Scene -> Prose).
 */

/**
 * Atomic creation of the initial Story structure for "Start Writing".
 * Story -> Chapter 1 -> Scene 1 -> Prose Card.
 */
class CreateStoryHierarchyCommand(
    private val nodeRepository: NodeRepository,
    private val cardRepository: CardRepository,
    private val storyName: String = "Untitled Story"
) : Command {
    override val label: String = "Start Writing"
    
    var createdStory: Node? = null
        private set
    var createdChapter: Node? = null
        private set
    var createdScene: Node? = null
        private set
    var createdProseCard: Card? = null
        private set

    private var hasCreatedOnce = false
    private val createdNodeIds = mutableListOf<String>()
    private val createdCardIds = mutableListOf<String>()

    override fun execute() {
        if (!hasCreatedOnce) {
            createdNodeIds.clear()
            createdCardIds.clear()
            // 1. Create Story Node
            val storyResult = nodeRepository.create(storyName, NodeTypes.STORY)
            if (storyResult is LcResult.Ok) {
                createdStory = storyResult.value
                createdNodeIds.add(createdStory!!.id)
                
                // 2. Create Chapter 1
                val chapterResult = nodeRepository.create("Chapter 1", NodeTypes.CHAPTER, parentNodeId = createdStory?.id, displayOrder = 0)
                if (chapterResult is LcResult.Ok) {
                    createdChapter = chapterResult.value
                    createdNodeIds.add(createdChapter!!.id)
                    
                    // 3. Create Scene 1
                    val sceneResult = nodeRepository.create("Scene 1", NodeTypes.SCENE, parentNodeId = createdChapter?.id, displayOrder = 0)
                    if (sceneResult is LcResult.Ok) {
                        createdScene = sceneResult.value
                        createdNodeIds.add(createdScene!!.id)
                        
                        // 4. Create Prose Card
                        val cardResult = cardRepository.create(createdScene!!.id, "Prose", CardTypes.PROSE)
                        if (cardResult is LcResult.Ok) {
                            createdProseCard = cardResult.value
                            createdCardIds.add(createdProseCard!!.id)
                            hasCreatedOnce = true
                        } else {
                            rollback()
                        }
                    } else {
                        rollback()
                    }
                } else {
                    rollback()
                }
            }
        } else {
            // Restore entire hierarchy on Redo
            createdStory?.let { nodeRepository.restore(it) }
            createdChapter?.let { nodeRepository.restore(it) }
            createdScene?.let { nodeRepository.restore(it) }
            createdProseCard?.let { cardRepository.restore(it) }
        }
    }

    private fun rollback() {
        createdCardIds.reversed().forEach { cardRepository.delete(it) }
        createdNodeIds.reversed().forEach { nodeRepository.delete(it) }
        createdStory = null
        createdChapter = null
        createdScene = null
        createdProseCard = null
    }

    override fun undo() {
        // Delete in reverse order to satisfy dependency checks (if any)
        createdProseCard?.let { cardRepository.delete(it.id) }
        createdScene?.let { nodeRepository.delete(it.id) }
        createdChapter?.let { nodeRepository.delete(it.id) }
        createdStory?.let { nodeRepository.delete(it.id) }
    }
}

/**
 * Creates a Chapter Node under a Story.
 */
class CreateChapterCommand(
    private val nodeRepository: NodeRepository,
    private val storyId: String,
    private val name: String
) : Command {
    override val label: String = "Add Chapter"
    var createdChapter: Node? = null
        private set
    private var hasCreatedOnce = false

    override fun execute() {
        if (!hasCreatedOnce) {
            val order = nodeRepository.getNextOrder(storyId)
            val result = nodeRepository.create(name, NodeTypes.CHAPTER, parentNodeId = storyId, displayOrder = order)
            if (result is LcResult.Ok) {
                createdChapter = result.value
                hasCreatedOnce = true
            }
        } else {
            createdChapter?.let { nodeRepository.restore(it) }
        }
    }

    override fun undo() {
        createdChapter?.let { nodeRepository.delete(it.id) }
    }
}

/**
 * Creates a Scene Node and its initial Prose Card.
 */
class CreateSceneCommand(
    private val nodeRepository: NodeRepository,
    private val cardRepository: CardRepository,
    private val chapterId: String,
    private val name: String
) : Command {
    override val label: String = "Add Scene"
    var createdScene: Node? = null
        private set
    var createdProseCard: Card? = null
        private set
    private var hasCreatedOnce = false

    override fun execute() {
        if (!hasCreatedOnce) {
            val order = nodeRepository.getNextOrder(chapterId)
            val sceneResult = nodeRepository.create(name, NodeTypes.SCENE, parentNodeId = chapterId, displayOrder = order)
            if (sceneResult is LcResult.Ok) {
                createdScene = sceneResult.value
                val cardResult = cardRepository.create(createdScene!!.id, "Prose", CardTypes.PROSE)
                if (cardResult is LcResult.Ok) {
                    createdProseCard = cardResult.value
                    hasCreatedOnce = true
                } else {
                    nodeRepository.delete(createdScene!!.id)
                    createdScene = null
                }
            }
        } else {
            createdScene?.let { nodeRepository.restore(it) }
            createdProseCard?.let { cardRepository.restore(it) }
        }
    }

    override fun undo() {
        createdProseCard?.let { cardRepository.delete(it.id) }
        createdScene?.let { nodeRepository.delete(it.id) }
    }
}

/**
 * Cascading Delete for Story hierarchy (Story -> Chapter -> Scene).
 * Captures all descendants to allow full undo (resurrection).
 */
class DeleteStoryHierarchyCommand(
    private val nodeRepository: NodeRepository,
    private val cardRepository: CardRepository,
    private val targetNodeId: String
) : Command {
    override val label: String = "Delete Hierarchy"
    
    private var capturedNodes: List<Node> = emptyList()
    private var capturedCards: List<Card> = emptyList()
    private var parentIdToNormalize: String? = null

    override fun execute() {
        if (capturedNodes.isEmpty()) {
            val targetNode = nodeRepository.get(targetNodeId)
            parentIdToNormalize = targetNode?.parentNodeId
            
            // First time: capture and delete
            val nodesToDelete = mutableListOf<Node>()
            val cardsToDelete = mutableListOf<Card>()
            
            fun collect(id: String) {
                nodeRepository.get(id)?.let { node ->
                    nodesToDelete.add(node)
                    cardsToDelete.addAll(cardRepository.listForNode(id))
                    nodeRepository.listByParent(id).forEach { collect(it.id) }
                }
            }
            
            collect(targetNodeId)
            capturedNodes = nodesToDelete
            capturedCards = cardsToDelete
        }

        // Delete in order: Children first (actually Repo blocks if parents deleted first)
        capturedCards.forEach { cardRepository.delete(it.id) }
        capturedNodes.reversed().forEach { nodeRepository.delete(it.id) }
        
        nodeRepository.normalizeOrders(parentIdToNormalize)
    }

    override fun undo() {
        // Restore in order: Parents first
        capturedNodes.forEach { nodeRepository.restore(it) }
        capturedCards.forEach { cardRepository.restore(it) }
    }
}

/**
 * Reorders a Node among its siblings while maintaining a contiguous deterministic sequence.
 */
class ReorderStoryNodeCommand(
    private val nodeRepository: NodeRepository,
    private val previousOrder: List<String>,
    private val newOrder: List<String>
) : Command {
    override val label: String = "Reorder"

    override fun execute() {
        applyOrder(newOrder)
    }

    override fun undo() {
        applyOrder(previousOrder)
    }

    private fun applyOrder(ids: List<String>) {
        ids.forEachIndexed { index, id ->
            nodeRepository.get(id)?.let { node ->
                node.reorder(index)
                nodeRepository.save(node)
            }
        }
    }
}

/**
 * Changes a Node's parent and re-normalizes orders (Phase 9C closure).
 */
class MoveNodeCommand(
    private val nodeRepository: NodeRepository,
    private val nodeId: String,
    private val newParentId: String?
) : Command {
    override val label: String = "Move Node"
    private var previousParentId: String? = null
    private var previousOrder: Int = 0
    private var newOrder: Int = 0

    override fun execute() {
        val node = nodeRepository.get(nodeId) ?: return
        previousParentId = node.parentNodeId
        previousOrder = node.displayOrder
        
        newOrder = nodeRepository.getNextOrder(newParentId)
        node.move(newParentId, newOrder)
        nodeRepository.save(node)
        
        nodeRepository.normalizeOrders(previousParentId)
        nodeRepository.normalizeOrders(newParentId)
    }

    override fun undo() {
        val node = nodeRepository.get(nodeId) ?: return
        node.move(previousParentId, previousOrder)
        nodeRepository.save(node)
        
        nodeRepository.normalizeOrders(previousParentId)
        nodeRepository.normalizeOrders(newParentId)
    }
}
