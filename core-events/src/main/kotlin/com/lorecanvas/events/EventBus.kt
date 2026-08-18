package com.lorecanvas.events

/**
 * Events skeleton (PEP-001, Phase 1).
 *
 * The Repository publishes a DomainEvent after every successful mutation
 * (LCD-006, LCD-009 — Search index, Graph View, and Timeline all refresh by
 * subscribing to these rather than being called directly). Keeping this in
 * its own module means the Repository never has a direct reference to
 * Search/Graph/Timeline — see LCD-004's "Low Coupling" principle.
 */
interface DomainEvent {
    val type: String
    val occurredAt: String
}

fun interface EventListener<in T : DomainEvent> {
    fun onEvent(event: T)
}

/**
 * A minimal, synchronous, in-process pub/sub bus. No threading guarantees
 * are made yet — Phase 1 goal is "the architecture exists," not
 * production-grade concurrency handling.
 */
class EventBus {
    private val listeners = mutableMapOf<String, MutableList<EventListener<DomainEvent>>>()

    fun <T : DomainEvent> subscribe(eventType: String, listener: EventListener<T>) {
        @Suppress("UNCHECKED_CAST")
        val list = listeners.getOrPut(eventType) { mutableListOf() } as MutableList<EventListener<T>>
        list.add(listener)
    }

    fun publish(event: DomainEvent) {
        val list = listeners[event.type] ?: return
        list.forEach { it.onEvent(event) }
    }

    fun clear() {
        listeners.clear()
    }
}
