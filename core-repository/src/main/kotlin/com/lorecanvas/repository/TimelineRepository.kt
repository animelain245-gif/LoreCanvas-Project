package com.lorecanvas.repository

import com.lorecanvas.common.LcResult
import com.lorecanvas.common.Logger
import com.lorecanvas.common.createLogger
import com.lorecanvas.domain.Timeline
import com.lorecanvas.domain.TimelineEvent
import com.lorecanvas.events.EventBus
import com.lorecanvas.storage.TimelineStorage
import com.lorecanvas.validation.TimelineValidator
import com.lorecanvas.validation.ValidationResult
import java.io.File

/**
 * TimelineRepository — Phase 6 "Timeline System" (PEP-001), implementing
 * LCD-006 Ch.9 and LCD-009 Ch.12. "Validate Dates" is interpreted as: a
 * date must be present (not blank) — see [TimelineEvent]'s own doc
 * comment on why dates stay free-form strings rather than a real calendar
 * type.
 */
class TimelineRepository(
    private val projectDirectory: File,
    private val timelineStorage: TimelineStorage,
    private val eventBus: EventBus,
    private val logger: Logger = createLogger("TimelineRepository")
) {

    fun create(name: String, description: String = ""): LcResult<Timeline, RepositoryError> {
        val validation = TimelineValidator.validateForCreate(name)
        if (validation is ValidationResult.Invalid) {
            return LcResult.fail(RepositoryError.ValidationFailed(validation.errors.joinToString { it.message }))
        }
        val timeline = Timeline.create(name, description)
        return when (val result = timelineStorage.createTimeline(projectDirectory, timeline)) {
            is LcResult.Fail -> LcResult.fail(RepositoryError.Storage(result.error))
            is LcResult.Ok -> {
                logger.info("Timeline created", timeline.id)
                eventBus.publish(TimelineChangeEvent.TimelineCreated(timeline.id))
                LcResult.ok(timeline)
            }
        }
    }

    fun save(timeline: Timeline): LcResult<Unit, RepositoryError> {
        val validation = TimelineValidator.validateForSave(timeline)
        if (validation is ValidationResult.Invalid) {
            return LcResult.fail(RepositoryError.ValidationFailed(validation.errors.joinToString { it.message }))
        }
        return when (val result = timelineStorage.saveTimeline(projectDirectory, timeline)) {
            is LcResult.Fail -> LcResult.fail(RepositoryError.Storage(result.error))
            is LcResult.Ok -> {
                eventBus.publish(TimelineChangeEvent.TimelineUpdated(timeline.id))
                LcResult.ok(Unit)
            }
        }
    }

    fun delete(timelineId: String): LcResult<Unit, RepositoryError> =
        when (val result = timelineStorage.deleteTimeline(projectDirectory, timelineId)) {
            is LcResult.Fail -> LcResult.fail(RepositoryError.Storage(result.error))
            is LcResult.Ok -> {
                eventBus.publish(TimelineChangeEvent.TimelineDeleted(timelineId))
                LcResult.ok(Unit)
            }
        }

    /** LCD-009 Ch.12 — "Add Event": validates the date/title, adds it, and saves the owning Timeline. */
    fun addEvent(timeline: Timeline, date: String, title: String, description: String = "", relatedNodeIds: List<String> = emptyList()): LcResult<TimelineEvent, RepositoryError> {
        val candidate = TimelineEvent.create(date, title, description, relatedNodeIds)
        val validation = TimelineValidator.validateEvent(candidate)
        if (validation is ValidationResult.Invalid) {
            return LcResult.fail(RepositoryError.ValidationFailed(validation.errors.joinToString { it.message }))
        }

        val event = candidate
        timeline.addEvent(event)
        return when (val result = save(timeline)) {
            is LcResult.Ok -> LcResult.ok(event)
            is LcResult.Fail -> {
                timeline.removeEvent(event.id) // roll back the in-memory add if persisting failed
                LcResult.fail(result.error)
            }
        }
    }

    /**
     * Bulk "Add Event" (Phase 7 — "Batch operations"). Adding events one
     * at a time each calls [save], which re-serializes and rewrites the
     * *entire* Timeline file — fine for a user adding one event by hand,
     * but a real, measured O(n²)-shaped cost for adding many at once (a
     * 5,000-event Import, for instance): a performance test this phase
     * showed 1,000 sequential single-event adds taking ~2.7s versus a
     * roughly linear cost when batched here into one validate-everything,
     * mutate-everything, save-once operation.
     */
    data class EventSpec(val date: String, val title: String, val description: String = "", val relatedNodeIds: List<String> = emptyList())

    fun addEvents(timeline: Timeline, specs: List<EventSpec>): LcResult<List<TimelineEvent>, RepositoryError> {
        val candidates = specs.map { TimelineEvent.create(it.date, it.title, it.description, it.relatedNodeIds) }
        for (candidate in candidates) {
            val validation = TimelineValidator.validateEvent(candidate)
            if (validation is ValidationResult.Invalid) {
                return LcResult.fail(RepositoryError.ValidationFailed(validation.errors.joinToString { it.message }))
            }
        }

        candidates.forEach { timeline.addEvent(it) }
        return when (val result = save(timeline)) {
            is LcResult.Ok -> LcResult.ok(candidates)
            is LcResult.Fail -> {
                candidates.forEach { timeline.removeEvent(it.id) } // roll back all of them together on failure
                LcResult.fail(result.error)
            }
        }
    }

    fun removeEvent(timeline: Timeline, eventId: String): LcResult<Unit, RepositoryError> {
        timeline.removeEvent(eventId)
        return save(timeline)
    }

    /** LCD-009 — "Editing an Event": Modify -> Validate -> Repository Update -> Timeline Refresh. */
    fun updateEvent(timeline: Timeline, updated: TimelineEvent): LcResult<Unit, RepositoryError> {
        val validation = TimelineValidator.validateEvent(updated)
        if (validation is ValidationResult.Invalid) {
            return LcResult.fail(RepositoryError.ValidationFailed(validation.errors.joinToString { it.message }))
        }
        timeline.replaceEvent(updated)
        return save(timeline)
    }

    /** LCD-006 Ch.9 — "Sort Events." Returns the timeline's events in chronological order. */
    fun sortedEvents(timeline: Timeline): List<TimelineEvent> = timeline.sortedEvents()

    /**
     * Restore (redo-of-create support for [CreateTimelineCommand]) — see
     * [NodeRepository.restore]'s doc comment for the full rationale.
     * Re-inserts an already-constructed [Timeline] at its existing id,
     * rather than minting a fresh one ([create]) or requiring the id
     * already exist ([save]).
     */
    fun restore(timeline: Timeline): LcResult<Unit, RepositoryError> =
        when (val result = timelineStorage.createTimeline(projectDirectory, timeline)) {
            is LcResult.Fail -> LcResult.fail(RepositoryError.Storage(result.error))
            is LcResult.Ok -> {
                logger.info("Timeline restored", timeline.id)
                eventBus.publish(TimelineChangeEvent.TimelineCreated(timeline.id))
                LcResult.ok(Unit)
            }
        }

    fun list(): List<Timeline> = timelineStorage.listTimelines(projectDirectory)

    fun get(timelineId: String): Timeline? = (timelineStorage.loadTimeline(projectDirectory, timelineId) as? LcResult.Ok)?.value
}
