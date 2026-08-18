package com.lorecanvas.repository

import com.lorecanvas.events.DomainEvent
import java.time.Instant

sealed class RelationshipEvent : DomainEvent {
    abstract val relationshipId: String

    data class RelationshipCreated(override val relationshipId: String) : RelationshipEvent() {
        override val type = "RelationshipCreated"
        override val occurredAt: String = Instant.now().toString()
    }
    data class RelationshipUpdated(override val relationshipId: String) : RelationshipEvent() {
        override val type = "RelationshipUpdated"
        override val occurredAt: String = Instant.now().toString()
    }
    data class RelationshipDeleted(override val relationshipId: String) : RelationshipEvent() {
        override val type = "RelationshipDeleted"
        override val occurredAt: String = Instant.now().toString()
    }
}
