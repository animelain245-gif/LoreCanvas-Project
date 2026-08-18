package com.lorecanvas.repository

import com.lorecanvas.events.DomainEvent
import java.time.Instant

/**
 * Node Events, mirroring [ProjectEvent]'s shape. LCD-009's Node workflows
 * (Chapters 6-8) each end with "Publish Event," and LCD-009 Chapter 6
 * additionally lists what should react to a Node event: Explorer, Search,
 * Graph, and Timeline references all refresh — none of those subsystems
 * exist yet (Phases 7/8/6), so for now these events simply exist and are
 * published; nothing subscribes yet except the smoke tests and, in the
 * `app` module, the UI's own refresh-the-list logic.
 */
sealed class NodeEvent : DomainEvent {
    abstract val nodeId: String

    data class NodeCreated(override val nodeId: String) : NodeEvent() {
        override val type = "NodeCreated"
        override val occurredAt: String = Instant.now().toString()
    }

    data class NodeUpdated(override val nodeId: String) : NodeEvent() {
        override val type = "NodeUpdated"
        override val occurredAt: String = Instant.now().toString()
    }

    data class NodeDeleted(override val nodeId: String) : NodeEvent() {
        override val type = "NodeDeleted"
        override val occurredAt: String = Instant.now().toString()
    }
}
