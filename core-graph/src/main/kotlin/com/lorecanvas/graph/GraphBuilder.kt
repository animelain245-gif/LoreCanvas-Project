package com.lorecanvas.graph

import com.lorecanvas.domain.Node
import com.lorecanvas.domain.Relationship
import com.lorecanvas.domain.RelationshipDirection
import kotlin.math.cos
import kotlin.math.sin

/**
 * Graph View (Phase 8, PEP-001) — "a visual, explorable map of how
 * everything in a project connects, built as a *view* over the data
 * rather than a separate source of truth" (LCD-001 Ch.5). This module
 * only computes the graph structure and a simple layout; it holds no
 * state of its own and persists nothing — Nodes and Relationships remain
 * the only source of truth.
 *
 * Layout is a plain circular arrangement (positions normalized to
 * 0.0-1.0, for the UI to scale to screen size). Simple and correct beats
 * a force-directed layout algorithm nobody asked for at Version 1 —
 * revisit if/when large projects make circular layouts unreadable.
 */
data class GraphNode(val id: String, val label: String, val type: String, val x: Float, val y: Float)

data class GraphEdge(val id: String, val sourceId: String, val targetId: String, val label: String, val bidirectional: Boolean)

data class GraphModel(val nodes: List<GraphNode>, val edges: List<GraphEdge>)

object GraphBuilder {

    fun build(nodes: List<Node>, relationships: List<Relationship>): GraphModel {
        val count = nodes.size
        val positioned = nodes.mapIndexed { index, node ->
            val angle = if (count <= 1) 0.0 else 2.0 * Math.PI * index / count
            GraphNode(
                id = node.id,
                label = node.name,
                type = node.type,
                x = (0.5 + 0.42 * cos(angle)).toFloat(),
                y = (0.5 + 0.42 * sin(angle)).toFloat()
            )
        }

        val nodeIds = nodes.map { it.id }.toSet()
        val edges = relationships
            .filter { it.sourceNodeId in nodeIds && it.targetNodeId in nodeIds }
            .map { rel ->
                GraphEdge(
                    id = rel.id,
                    sourceId = rel.sourceNodeId,
                    targetId = rel.targetNodeId,
                    label = rel.type,
                    bidirectional = rel.direction == RelationshipDirection.BIDIRECTIONAL
                )
            }

        return GraphModel(positioned, edges)
    }
}
