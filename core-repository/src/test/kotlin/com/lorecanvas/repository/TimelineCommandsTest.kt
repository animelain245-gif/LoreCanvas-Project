package com.lorecanvas.repository

import kotlin.test.*

class TimelineCommandsTest : CommandTestFixture() {

    @BeforeTest fun setUp() = setUpFixture()
    @AfterTest fun tearDown() = tearDownFixture()

    @Test fun `create then undo removes the timeline`() {
        val cmd = CreateTimelineCommand(timelineRepo, "World History")
        history.execute(cmd)
        val timeline = cmd.createdTimeline!!
        assertNotNull(timelineRepo.get(timeline.id))

        history.undo()
        assertNull(timelineRepo.get(timeline.id))
    }

    @Test fun `redo after undo of create restores the SAME id`() {
        val cmd = CreateTimelineCommand(timelineRepo, "Character Arcs")
        history.execute(cmd)
        val originalId = cmd.createdTimeline!!.id

        history.undo()
        history.redo()
        assertNotNull(timelineRepo.get(originalId))
        assertEquals(originalId, cmd.createdTimeline!!.id)
    }

    @Test fun `rename and update description undo correctly`() {
        val cmd = CreateTimelineCommand(timelineRepo, "World History")
        history.execute(cmd)
        val timeline = cmd.createdTimeline!!

        history.execute(RenameTimelineCommand(timelineRepo, timeline, "History of the Realm"))
        history.undo()
        assertEquals("World History", timeline.name)

        history.execute(UpdateTimelineDescriptionCommand(timelineRepo, timeline, "A long saga."))
        history.undo()
        assertEquals("", timeline.description)
    }

    @Test fun `add event, redo restores the SAME event id`() {
        val timeline = (timelineRepo.create("World History") as com.lorecanvas.common.LcResult.Ok).value
        val cmd = AddTimelineEventCommand(timelineRepo, timeline, "1000", "The Founding")
        history.execute(cmd)
        val originalEventId = cmd.createdEvent!!.id
        assertTrue(timeline.events.any { it.id == originalEventId })

        history.undo()
        assertTrue(timeline.events.none { it.id == originalEventId })

        history.redo()
        assertTrue(timeline.events.any { it.id == originalEventId }, "Redo must restore the exact same event id")
        assertEquals("The Founding", timeline.events.find { it.id == originalEventId }!!.title)
    }

    @Test fun `remove event, undo restores the exact original event with identical content`() {
        val timeline = (timelineRepo.create("World History") as com.lorecanvas.common.LcResult.Ok).value
        val addCmd = AddTimelineEventCommand(timelineRepo, timeline, "1000", "The Founding", "City founded")
        history.execute(addCmd)
        val event = addCmd.createdEvent!!

        history.execute(RemoveTimelineEventCommand(timelineRepo, timeline, event.id))
        assertTrue(timeline.events.none { it.id == event.id })

        history.undo()
        val restored = timeline.events.find { it.id == event.id }
        assertNotNull(restored, "Undo of remove-event must restore it")
        assertEquals("The Founding", restored.title)
        assertEquals("City founded", restored.description)
    }

    @Test fun `update event, undo restores the pre-edit event`() {
        val timeline = (timelineRepo.create("World History") as com.lorecanvas.common.LcResult.Ok).value
        val addCmd = AddTimelineEventCommand(timelineRepo, timeline, "1000", "The Founding")
        history.execute(addCmd)
        val original = addCmd.createdEvent!!
        val edited = original.copy(title = "The Great Founding")

        history.execute(UpdateTimelineEventCommand(timelineRepo, timeline, edited))
        assertEquals("The Great Founding", timeline.events.find { it.id == original.id }!!.title)

        history.undo()
        assertEquals("The Founding", timeline.events.find { it.id == original.id }!!.title)
    }
}
