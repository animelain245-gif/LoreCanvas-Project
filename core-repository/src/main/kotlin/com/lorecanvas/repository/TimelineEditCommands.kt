package com.lorecanvas.repository

import com.lorecanvas.commands.Command
import com.lorecanvas.domain.Timeline

/**
 * Timeline's counterpart to [NodeEditCommands] — see its class doc for
 * the full rationale on why the revert-then-construct step is needed.
 * Only covers `name`: [TimelineEditorScreen] has no other live-edited
 * Timeline-level field (event fields are handled separately, as discrete
 * dialog actions, not this live-edit-then-batch-save pattern).
 *
 * Bug note: prior to this, [TimelineEditorScreen]'s `onRename` mutated
 * `Timeline.name` in memory on every keystroke but nothing ever called
 * [TimelineRepository.save] for it — unlike Node/Card/Relationship,
 * Timeline's screen has no Save button, so a renamed Timeline was never
 * actually persisted to disk. Wiring this through [TimelineRepository.save]
 * (here, via [RenameTimelineCommand]) at the point the UI commits the
 * rename (on navigating back) fixes that alongside making it undoable.
 */
object TimelineEditCommands {

    fun buildRenameCommand(timelineRepository: TimelineRepository, timeline: Timeline, snapshotName: String): Command? {
        if (timeline.name == snapshotName) return null
        val newName = timeline.name
        timeline.rename(snapshotName) // revert so RenameTimelineCommand captures the correct "previous"
        return RenameTimelineCommand(timelineRepository, timeline, newName)
    }
}
