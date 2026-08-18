package com.lorecanvas.graph

import com.lorecanvas.events.DomainEvent
import com.lorecanvas.events.EventBus
import com.lorecanvas.events.EventListener
import com.lorecanvas.repository.NodeRepository
import com.lorecanvas.repository.RelationshipRepository

/**
 * GraphCache — "Graph updates must react automatically to Repository
 * events" (Phase 6, Milestone 1). Mirrors `core-search`'s
 * `SearchIndexCache` exactly: builds the [GraphModel] once via
 * [GraphBuilder], then rebuilds it whenever a Node or Relationship event
 * fires, rather than requiring the UI to remember to ask for a fresh
 * graph after every edit.
 *
 * Depends on Repositories (not Storage) for the same reason
 * `SearchIndexCache` does — Graph reads through the same layer everything
 * else does.
 */
class GraphCache(
    private val nodeRepository: NodeRepository,
    private val relationshipRepository: RelationshipRepository,
    eventBus: EventBus
) {
    @Volatile private var currentModel: GraphModel = GraphModel(emptyList(), emptyList())

    init {
        rebuild()
        val rebuildListener = EventListener<DomainEvent> { rebuild() }
        listOf("NodeCreated", "NodeUpdated", "NodeDeleted").forEach { eventBus.subscribe(it, rebuildListener) }
        listOf("RelationshipCreated", "RelationshipUpdated", "RelationshipDeleted").forEach { eventBus.subscribe(it, rebuildListener) }
    }

    private fun rebuild() {
        currentModel = GraphBuilder.build(nodeRepository.list(), relationshipRepository.list())
    }

    fun current(): GraphModel = currentModel
}
