package com.lorecanvas.repository

import com.lorecanvas.events.DomainEvent
import java.time.Instant

sealed class CardEvent : DomainEvent {
    abstract val cardId: String

    data class CardCreated(override val cardId: String) : CardEvent() {
        override val type = "CardCreated"
        override val occurredAt: String = Instant.now().toString()
    }
    data class CardUpdated(override val cardId: String) : CardEvent() {
        override val type = "CardUpdated"
        override val occurredAt: String = Instant.now().toString()
    }
    data class CardDeleted(override val cardId: String) : CardEvent() {
        override val type = "CardDeleted"
        override val occurredAt: String = Instant.now().toString()
    }
}
