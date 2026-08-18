package com.lorecanvas.search

import com.lorecanvas.domain.Card
import com.lorecanvas.domain.Node
import com.lorecanvas.domain.Relationship
import com.lorecanvas.domain.Template
import com.lorecanvas.domain.Timeline
import com.lorecanvas.events.DomainEvent
import com.lorecanvas.events.EventBus
import com.lorecanvas.events.EventListener
import com.lorecanvas.repository.CardRepository
import com.lorecanvas.repository.NodeRepository
import com.lorecanvas.repository.RelationshipRepository
import com.lorecanvas.repository.TemplateRepository
import com.lorecanvas.repository.TimelineRepository

/**
 * SearchIndexCache — real "indexing updates," not a rescan-everything-per-
 * keystroke approach. Loads each entity list once via the Repository
 * layer, then keeps itself current by subscribing to the same [EventBus]
 * the Repository layer already publishes to on every mutation
 * (`NodeCreated`, `CardUpdated`, etc.).
 *
 * Deliberately depends on Repositories, not Storage directly — Search
 * reads through the same layer everything else does, rather than
 * reaching around it. Subscribed generically by event-type string against
 * the base [DomainEvent] rather than each Repository module's own event
 * classes, so this cache doesn't need compile-time knowledge of every
 * event type's shape, only its name.
 *
 * Each event only triggers a re-read of *its own* entity type (a Card
 * change re-lists Cards, not everything), which is both correct and far
 * cheaper than a per-keystroke full-rescan-of-everything — directly
 * addressing the "stay responsive at 50,000+ Cards" performance
 * requirement for the one place (Search) that touches every entity type
 * on every keystroke.
 */
class SearchIndexCache(
    private val nodeRepository: NodeRepository,
    private val cardRepository: CardRepository,
    private val relationshipRepository: RelationshipRepository,
    private val timelineRepository: TimelineRepository,
    private val templateRepository: TemplateRepository,
    eventBus: EventBus
) {
    @Volatile private var nodes: List<Node> = emptyList()
    @Volatile private var cards: List<Card> = emptyList()
    @Volatile private var relationships: List<Relationship> = emptyList()
    @Volatile private var timelines: List<Timeline> = emptyList()
    @Volatile private var templates: List<Template> = emptyList()

    init {
        refreshNodes()
        refreshCards()
        refreshRelationships()
        refreshTimelines()
        refreshTemplates()

        val refreshNodesListener = EventListener<DomainEvent> { refreshNodes() }
        val refreshCardsListener = EventListener<DomainEvent> { refreshCards() }
        val refreshRelationshipsListener = EventListener<DomainEvent> { refreshRelationships() }
        val refreshTimelinesListener = EventListener<DomainEvent> { refreshTimelines() }
        val refreshTemplatesListener = EventListener<DomainEvent> { refreshTemplates() }

        listOf("NodeCreated", "NodeUpdated", "NodeDeleted").forEach { eventBus.subscribe(it, refreshNodesListener) }
        listOf("CardCreated", "CardUpdated", "CardDeleted").forEach { eventBus.subscribe(it, refreshCardsListener) }
        listOf("RelationshipCreated", "RelationshipUpdated", "RelationshipDeleted").forEach { eventBus.subscribe(it, refreshRelationshipsListener) }
        listOf("TimelineCreated", "TimelineUpdated", "TimelineDeleted").forEach { eventBus.subscribe(it, refreshTimelinesListener) }
        listOf("TemplateCreated", "TemplateDeleted").forEach { eventBus.subscribe(it, refreshTemplatesListener) }
    }

    private fun refreshNodes() { nodes = nodeRepository.list() }
    private fun refreshCards() { cards = cardRepository.listAll() }
    private fun refreshRelationships() { relationships = relationshipRepository.list() }
    private fun refreshTimelines() { timelines = timelineRepository.list() }
    private fun refreshTemplates() { templates = templateRepository.list() }

    /**
     * Manual escape hatch, kept for completeness — every Repository
     * mutation already publishes an event this cache subscribes to, so
     * callers shouldn't normally need this.
     */
    fun refreshAllNow() {
        refreshNodes(); refreshCards(); refreshRelationships(); refreshTimelines(); refreshTemplates()
    }

    fun search(query: String, filter: Set<SearchResultKind> = SearchResultKind.entries.toSet()): List<SearchResult> =
        SearchIndex.search(query, nodes, cards, relationships, timelines, templates, filter)
}
