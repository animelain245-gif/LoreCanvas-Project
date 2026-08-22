package com.lorecanvas.repository

import com.lorecanvas.commands.Command
import com.lorecanvas.domain.Timeline
import com.lorecanvas.domain.TimelineEvent

/**
 * Timeline Commands — the Timeline-system counterpart to [NodeCommands],
 * [CardCommands], and [RelationshipCommands], same undo/redo pattern
 * (LCD-009 Ch.18). Timeline is the one domain object with two levels of
 * mutation — the Timeline itself (name/description) and the
 * [TimelineEvent]s it owns — so this file covers both.
 *
 * Scope note: **Delete Timeline is deliberately not wrapped**, for the
 * same reason established in [NodeCommands] — [Timeline.create] mints a
 * fresh UUID, so there's no identity-preserving way to undo a delete
 * without a new Storage capability this pass didn't build.
 *
 * Adding and removing *Events*, however, are both fully reversible
 * without any new capability, because [TimelineEvent] is a plain data
 * class the Command can just hold onto: undoing an add means removing
 * the exact event that was added, and undoing a remove means re-adding
 * the exact event object that was captured before removal — no new id is
 * ever minted on the undo path, so identity is never in question the way
 * it is for Delete Node/Card/Relationship/Timeline.
 */
class CreateTimelineCommand(
    private val timelineRepository: TimelineRepository,
    private val name: String,
    private val description: String = ""
) : Command {
    override val label: String = "Create Timeline"
    var createdTimeline: Timeline? = null
        private set

    /** See [CreateNodeCommand]'s doc comment on this same field — redo must re-insert the same Timeline via [TimelineRepository.restore], not mint a new UUID or call [TimelineRepository.save] (which requires the id already exist). */
    private var hasCreatedOnce = false

    override fun execute() {
        if (!hasCreatedOnce) {
            val result = timelineRepository.create(name, description)
            if (result is com.lorecanvas.common.LcResult.Ok) {
                createdTimeline = result.value
                hasCreatedOnce = true
            }
        } else {
            createdTimeline?.let { timelineRepository.restore(it) }
        }
    }

    /** Undoing a create removes the Timeline — the one case where this module does call delete(), on a Timeline this same command just made. */
    override fun undo() {
        createdTimeline?.let { timelineRepository.delete(it.id) }
    }
}

class RenameTimelineCommand(private val timelineRepository: TimelineRepository, private val timeline: Timeline, private val newName: String) : Command {
    override val label: String = "Rename Timeline"
    private var previousName: String = timeline.name

    override fun execute() {
        previousName = timeline.name
        timeline.rename(newName)
        timelineRepository.save(timeline)
    }

    override fun undo() {
        timeline.rename(previousName)
        timelineRepository.save(timeline)
    }
}

class UpdateTimelineDescriptionCommand(
    private val timelineRepository: TimelineRepository,
    private val timeline: Timeline,
    private val newDescription: String
) : Command {
    override val label: String = "Edit Timeline Description"
    private var previousDescription: String = timeline.description

    override fun execute() {
        previousDescription = timeline.description
        timeline.updateDescription(newDescription)
        timelineRepository.save(timeline)
    }

    override fun undo() {
        timeline.updateDescription(previousDescription)
        timelineRepository.save(timeline)
    }
}

/** LCD-009 Ch.12 — "Add Event." Undo removes the exact event this command created. */
class AddTimelineEventCommand(
    private val timelineRepository: TimelineRepository,
    private val timeline: Timeline,
    private val date: String,
    private val title: String,
    private val description: String = "",
    private val relatedNodeIds: List<String> = emptyList()
) : Command {
    override val label: String = "Add Timeline Event"
    var createdEvent: TimelineEvent? = null
        private set

    /**
     * See [CreateNodeCommand]'s doc comment on this same field. Here the
     * fresh-id minting happens inside [TimelineEvent.create] (called by
     * [TimelineRepository.addEvent]) — so on redo this re-adds the exact
     * same [TimelineEvent] object directly via [Timeline.addEvent] plus a
     * manual save, the same technique [RemoveTimelineEventCommand.undo]
     * already uses, instead of routing back through the repository
     * method that would mint yet another new id.
     */
    private var hasCreatedOnce = false

    override fun execute() {
        if (!hasCreatedOnce) {
            val result = timelineRepository.addEvent(timeline, date, title, description, relatedNodeIds)
            if (result is com.lorecanvas.common.LcResult.Ok) {
                createdEvent = result.value
                hasCreatedOnce = true
            }
        } else {
            createdEvent?.let {
                timeline.addEvent(it)
                timelineRepository.save(timeline)
            }
        }
    }

    override fun undo() {
        createdEvent?.let { timelineRepository.removeEvent(timeline, it.id) }
    }
}

/**
 * "Editing an Event" (LCD-009): Modify -> Validate -> Repository Update ->
 * Timeline Refresh. Captures the pre-edit [TimelineEvent] so undo can
 * replace it right back — the id never changes, so [Timeline.replaceEvent]
 * (via [TimelineRepository.updateEvent]) finds and restores it cleanly.
 */
class UpdateTimelineEventCommand(
    private val timelineRepository: TimelineRepository,
    private val timeline: Timeline,
    private val updatedEvent: TimelineEvent
) : Command {
    override val label: String = "Edit Timeline Event"
    private val previousEvent: TimelineEvent? = timeline.events.find { it.id == updatedEvent.id }

    override fun execute() {
        timelineRepository.updateEvent(timeline, updatedEvent)
    }

    override fun undo() {
        previousEvent?.let { timelineRepository.updateEvent(timeline, it) }
    }
}

/**
 * Removing an Event. Captures the exact [TimelineEvent] before removal so
 * undo can hand it straight back to [Timeline.addEvent] — bypassing
 * [TimelineRepository.addEvent]'s fresh-id creation, since restoring the
 * *original* event (same id, same content) is the whole point, not
 * minting a new one.
 */
class RemoveTimelineEventCommand(
    private val timelineRepository: TimelineRepository,
    private val timeline: Timeline,
    private val eventId: String
) : Command {
    override val label: String = "Remove Timeline Event"
    private val removedEvent: TimelineEvent? = timeline.events.find { it.id == eventId }

    override fun execute() {
        timelineRepository.removeEvent(timeline, eventId)
    }

    override fun undo() {
        removedEvent?.let {
            timeline.addEvent(it)
            timelineRepository.save(timeline)
        }
    }
}
