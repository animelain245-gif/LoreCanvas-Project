package com.lorecanvas.domain

import com.lorecanvas.common.UuidService
import java.time.Instant

/**
 * Timeline Event (LCD-005 Ch.15) — "connects chronological information
 * with Domain entities... one event may reference many Nodes." Embedded
 * inside its owning [Timeline], not a separate file (LCD-005 Ch.14:
 * "Timelines do not duplicate information, they reference existing
 * entities" — the entity being referenced is the Node, via
 * [relatedNodeIds]; the event itself only meaningfully exists on its
 * Timeline).
 *
 * [date] is a free-form string on purpose — fictional calendars rarely
 * match the Gregorian calendar. [sortKey] gives the Timeline Repository's
 * "Sort Events" responsibility (LCD-006 Ch.9) something numeric to sort
 * by when the date happens to be numeric (e.g. "1012"), falling back to
 * string order otherwise.
 */
data class TimelineEvent(
    val id: String,
    val date: String,
    val title: String,
    val description: String,
    val relatedNodeIds: List<String>,
    val tags: List<String>
) {
    val sortKey: Double get() = date.toDoubleOrNull() ?: Double.MAX_VALUE

    companion object {
        fun create(date: String, title: String, description: String = "", relatedNodeIds: List<String> = emptyList(), tags: List<String> = emptyList()) =
            TimelineEvent(UuidService.generate(), date, title, description, relatedNodeIds, tags)
    }
}

/**
 * Timeline (LCD-005 Ch.14) — "organizes events chronologically. A Project
 * may contain multiple timelines" (World History, a single character's
 * life, a war, etc).
 */
class Timeline private constructor(
    id: String,
    createdAt: String,
    modifiedAt: String,
    initialName: String,
    initialDescription: String,
    initialEvents: List<TimelineEvent>,
    initialSettings: Map<String, Any?>
) : Entity(id = id, createdAt = createdAt, modifiedAt = modifiedAt) {

    var name: String = initialName
        private set

    var description: String = initialDescription
        private set

    private val mutableEvents: MutableList<TimelineEvent> = initialEvents.toMutableList()
    val events: List<TimelineEvent> get() = mutableEvents.toList()

    private val mutableSettings: MutableMap<String, Any?> = initialSettings.toMutableMap()
    val settings: Map<String, Any?> get() = mutableSettings.toMap()

    init {
        require(name.isNotBlank()) { "Timeline name must not be empty." }
    }

    fun rename(newName: String) {
        require(newName.isNotBlank()) { "Timeline name must not be empty." }
        name = newName.trim()
        touch()
    }

    fun updateDescription(newDescription: String) {
        description = newDescription
        touch()
    }

    fun addEvent(event: TimelineEvent) {
        mutableEvents.add(event)
        touch()
    }

    fun removeEvent(eventId: String) {
        if (mutableEvents.removeIf { it.id == eventId }) touch()
    }

    fun replaceEvent(updated: TimelineEvent) {
        val index = mutableEvents.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            mutableEvents[index] = updated
            touch()
        }
    }

    /** LCD-006 Ch.9 — "Sort Events." Chronological by [TimelineEvent.sortKey], ties broken by date string. */
    fun sortedEvents(): List<TimelineEvent> = mutableEvents.sortedWith(compareBy({ it.sortKey }, { it.date }))

    companion object {
        fun create(name: String, description: String = ""): Timeline {
            val now = Instant.now().toString()
            return Timeline(UuidService.generate(), now, now, name.trim(), description, emptyList(), emptyMap())
        }

        fun restore(
            id: String,
            createdAt: String,
            modifiedAt: String,
            name: String,
            description: String,
            events: List<TimelineEvent>,
            settings: Map<String, Any?>
        ): Timeline = Timeline(id, createdAt, modifiedAt, name.trim(), description, events, settings)
    }
}
