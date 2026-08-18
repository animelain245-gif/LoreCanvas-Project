package com.lorecanvas.repository

import com.lorecanvas.events.DomainEvent
import java.time.Instant

/**
 * Project Events (LCD-006, Chapter 13 — Repository Events: "Project
 * Created / Opened / Saved / Closed"). The Repository publishes these
 * instead of refreshing the UI directly — Explorer, Search, and Graph
 * subscribe to the [com.lorecanvas.events.EventBus] instead of being called
 * from here.
 */
sealed class ProjectEvent : DomainEvent {
    abstract val projectId: String

    data class ProjectCreated(override val projectId: String) : ProjectEvent() {
        override val type = "ProjectCreated"
        override val occurredAt: String = Instant.now().toString()
    }

    data class ProjectOpened(override val projectId: String) : ProjectEvent() {
        override val type = "ProjectOpened"
        override val occurredAt: String = Instant.now().toString()
    }

    data class ProjectSaved(override val projectId: String) : ProjectEvent() {
        override val type = "ProjectSaved"
        override val occurredAt: String = Instant.now().toString()
    }

    data class ProjectClosed(override val projectId: String) : ProjectEvent() {
        override val type = "ProjectClosed"
        override val occurredAt: String = Instant.now().toString()
    }
}
