package com.lorecanvas.search

import com.lorecanvas.domain.Card
import com.lorecanvas.domain.Node
import com.lorecanvas.domain.Relationship
import com.lorecanvas.domain.Template
import com.lorecanvas.domain.Timeline
import com.lorecanvas.domain.TimelineEvent

/**
 * Search (Phase 7, PEP-001), implementing LCD-009 Ch.14: "User Types ->
 * Search Index -> Rank Results -> Display Results -> Open Selection."
 *
 * This is the pure ranking function — no I/O, no Repository/Storage
 * dependency at all, just scoring already-in-memory lists. [SearchIndexCache]
 * is what keeps those lists current and reachable; this object stays a
 * plain, deterministic function so it's trivially unit-testable on its own.
 */
sealed class SearchResult {
    abstract val score: Int
    data class NodeResult(val node: Node, override val score: Int) : SearchResult()
    data class CardResult(val card: Card, override val score: Int) : SearchResult()
    data class RelationshipResult(val relationship: Relationship, override val score: Int) : SearchResult()
    data class TimelineEventResult(val timelineId: String, val event: TimelineEvent, override val score: Int) : SearchResult()
    data class TemplateResult(val template: Template, override val score: Int) : SearchResult()
}

/** One flag per [SearchResult] subtype — lets the UI offer "Nodes only," "Cards only," etc. */
enum class SearchResultKind { NODE, CARD, RELATIONSHIP, TIMELINE_EVENT, TEMPLATE }

object SearchIndex {

    /** Exact match scores highest, then prefix match, then substring match; no match scores 0 (excluded). */
    private fun fieldScore(field: String, query: String): Int {
        if (query.isBlank() || field.isBlank()) return 0
        val f = field.lowercase()
        val q = query.lowercase()
        return when {
            f == q -> 100
            f.startsWith(q) -> 70
            f.contains(q) -> 40
            else -> 0
        }
    }

    private fun bestOf(vararg scores: Int): Int = scores.maxOrNull() ?: 0

    fun search(
        query: String,
        nodes: List<Node>,
        cards: List<Card> = emptyList(),
        relationships: List<Relationship> = emptyList(),
        timelines: List<Timeline> = emptyList(),
        templates: List<Template> = emptyList(),
        filter: Set<SearchResultKind> = SearchResultKind.entries.toSet()
    ): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        val nodeResults = if (SearchResultKind.NODE !in filter) emptyList() else nodes.mapNotNull { node ->
            val score = bestOf(
                fieldScore(node.name, query),
                fieldScore(node.type, query),
                fieldScore(node.summary, query),
                node.tags.maxOfOrNull { fieldScore(it, query) } ?: 0
            )
            if (score > 0) SearchResult.NodeResult(node, score) else null
        }

        val cardResults = if (SearchResultKind.CARD !in filter) emptyList() else cards.mapNotNull { card ->
            val score = bestOf(
                fieldScore(card.title, query),
                fieldScore(card.type, query),
                fieldScore(card.content, query),
                card.tags.maxOfOrNull { fieldScore(it, query) } ?: 0
            )
            if (score > 0) SearchResult.CardResult(card, score) else null
        }

        val relationshipResults = if (SearchResultKind.RELATIONSHIP !in filter) emptyList() else relationships.mapNotNull { rel ->
            val score = bestOf(fieldScore(rel.type, query), fieldScore(rel.description, query))
            if (score > 0) SearchResult.RelationshipResult(rel, score) else null
        }

        val timelineEventResults = if (SearchResultKind.TIMELINE_EVENT !in filter) emptyList() else timelines.flatMap { timeline ->
            timeline.events.mapNotNull { event ->
                val score = bestOf(
                    fieldScore(event.title, query),
                    fieldScore(event.description, query),
                    event.tags.maxOfOrNull { fieldScore(it, query) } ?: 0
                )
                if (score > 0) SearchResult.TimelineEventResult(timeline.id, event, score) else null
            }
        }

        val templateResults = if (SearchResultKind.TEMPLATE !in filter) emptyList() else templates.mapNotNull { template ->
            val score = bestOf(fieldScore(template.name, query), fieldScore(template.targetNodeType, query))
            if (score > 0) SearchResult.TemplateResult(template, score) else null
        }

        return (nodeResults + cardResults + relationshipResults + timelineEventResults + templateResults)
            .sortedByDescending { it.score }
    }
}
