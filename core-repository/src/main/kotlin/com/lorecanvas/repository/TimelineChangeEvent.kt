package com.lorecanvas.repository

import com.lorecanvas.events.DomainEvent
import java.time.Instant

sealed class TimelineChangeEvent : DomainEvent {
    abstract val timelineId: String

    data class TimelineCreated(override val timelineId: String) : TimelineChangeEvent() {
        override val type = "TimelineCreated"
        override val occurredAt: String = Instant.now().toString()
    }
    data class TimelineUpdated(override val timelineId: String) : TimelineChangeEvent() {
        override val type = "TimelineUpdated"
        override val occurredAt: String = Instant.now().toString()
    }
    data class TimelineDeleted(override val timelineId: String) : TimelineChangeEvent() {
        override val type = "TimelineDeleted"
        override val occurredAt: String = Instant.now().toString()
    }
}
