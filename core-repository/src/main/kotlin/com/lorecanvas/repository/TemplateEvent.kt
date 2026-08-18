package com.lorecanvas.repository

import com.lorecanvas.events.DomainEvent
import java.time.Instant

sealed class TemplateEvent : DomainEvent {
    abstract val templateId: String

    data class TemplateCreated(override val templateId: String) : TemplateEvent() {
        override val type = "TemplateCreated"
        override val occurredAt: String = Instant.now().toString()
    }
    data class TemplateDeleted(override val templateId: String) : TemplateEvent() {
        override val type = "TemplateDeleted"
        override val occurredAt: String = Instant.now().toString()
    }
    data class TemplateApplied(override val templateId: String, val createdNodeId: String) : TemplateEvent() {
        override val type = "TemplateApplied"
        override val occurredAt: String = Instant.now().toString()
    }
}
