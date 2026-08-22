package com.lorecanvas.repository

import com.lorecanvas.domain.NodeStatus
import com.lorecanvas.domain.RelationshipContext
import kotlin.test.*

/**
 * Exercises the exact call sequence [com.lorecanvas.app.ui.LoreCanvasApp]'s
 * editor screens make: open editor (snapshot taken) -> live field edits
 * (mutating the domain object directly, as the UI does on every keystroke)
 * -> Save (diff snapshot vs. live, build one Command) -> Undo/Redo.
 *
 * This is the UI-to-Command boundary the "UI Command Integration"
 * milestone asked to be tested — Compose itself can't be exercised here
 * (no Android/Compose build available in this environment), but the
 * *logic* Compose delegates to (NodeEditCommands, CardEditCommands,
 * RelationshipEditCommands, TimelineEditCommands) is plain Kotlin and
 * fully testable, and is exactly what LoreCanvasApp.kt's onSave/onBack
 * handlers call.
 */
class UiBoundaryTest : CommandTestFixture() {

    @BeforeTest fun setUp() = setUpFixture()
    @AfterTest fun tearDown() = tearDownFixture()

    @Test fun `node multi-field edit saves as one atomic undo step`() {
        val createCmd = CreateNodeCommand(nodeRepo, "Elena", "Character", "A wandering scholar.")
        history.execute(createCmd)
        val node = createCmd.createdNode!!

        // Simulate "open editor": snapshot taken
        val snapshot = NodeEditCommands.Snapshot.of(node)

        // Simulate live keystroke edits before Save (exactly what NodeEditorScreen's
        // onRename/onUpdateSummary/onAddTag/onArchiveToggle do today)
        node.rename("Elena Voss")
        node.updateSummary("A wandering scholar turned court advisor.")
        node.addTag("plot-critical")
        node.archive()

        // Simulate pressing Save — the exact call LoreCanvasApp.kt's onSave makes
        val saveCommand = NodeEditCommands.buildSaveCommand(nodeRepo, node, snapshot)
        assertNotNull(saveCommand, "Four fields changed, expected a non-null Save command")
        history.execute(saveCommand)

        assertEquals("Elena Voss", node.name)
        assertEquals(NodeStatus.ARCHIVED, node.status)
        assertTrue(node.tags.contains("plot-critical"))
        assertEquals("Elena Voss", nodeRepo.get(node.id)!!.name, "Save must persist to disk")

        // Undo restores ALL 4 fields as ONE step, since Save = one CompoundCommand
        history.undo()
        assertEquals("Elena", node.name)
        assertEquals("A wandering scholar.", node.summary)
        assertFalse(node.tags.contains("plot-critical"))
        assertEquals(NodeStatus.ACTIVE, node.status)
        assertEquals("Elena", nodeRepo.get(node.id)!!.name, "Undo must persist the reverted state too")

        // Redo reapplies all 4 fields
        history.redo()
        assertEquals("Elena Voss", node.name)
        assertEquals(NodeStatus.ARCHIVED, node.status)
        assertTrue(node.tags.contains("plot-critical"))
    }

    @Test fun `node save with no changes returns null and pushes nothing to the undo stack`() {
        val createCmd = CreateNodeCommand(nodeRepo, "Frank", "Character")
        history.execute(createCmd)
        val node = createCmd.createdNode!!
        val snapshot = NodeEditCommands.Snapshot.of(node)

        assertNull(NodeEditCommands.buildSaveCommand(nodeRepo, node, snapshot))
    }

    @Test fun `card multi-field edit saves and undoes atomically`() {
        val node = (nodeRepo.create("Elena", "Character") as com.lorecanvas.common.LcResult.Ok).value
        val createCmd = CreateCardCommand(cardRepo, node.id, "Origins", "Rich Text", "Born in the eastern hills.")
        history.execute(createCmd)
        val card = createCmd.createdCard!!
        val snapshot = CardEditCommands.Snapshot.of(card)

        card.rename("Backstory")
        card.updateContent("Born in the eastern hills, orphaned young.")
        card.addTag("core")

        val saveCommand = CardEditCommands.buildSaveCommand(cardRepo, card, snapshot)
        assertNotNull(saveCommand)
        history.execute(saveCommand)

        assertEquals("Backstory", card.title)
        assertEquals("Backstory", cardRepo.get(card.id)!!.title, "Card save must persist")

        history.undo()
        assertEquals("Origins", card.title)
        assertFalse(card.tags.contains("core"))
    }

    @Test fun `relationship field edit saves and undoes`() {
        val a = (nodeRepo.create("Elena", "Character") as com.lorecanvas.common.LcResult.Ok).value
        val b = (nodeRepo.create("Marcus", "Character") as com.lorecanvas.common.LcResult.Ok).value
        val createCmd = CreateRelationshipCommand(relRepo, a.id, b.id, "Mentor Of")
        history.execute(createCmd)
        val rel = createCmd.createdRelationship!!
        val snapshot = RelationshipEditCommands.Snapshot.of(rel)

        rel.changeType("Rival Of")
        rel.updateDescription("Once allies, now opposed.")

        val saveCommand = RelationshipEditCommands.buildSaveCommand(relRepo, rel, snapshot)
        assertNotNull(saveCommand)
        history.execute(saveCommand)
        assertEquals("Rival Of", relRepo.get(rel.id)!!.type)

        history.undo()
        assertEquals("Mentor Of", rel.type)
        assertEquals("", rel.description)
    }

    @Test fun `relationship addContext-only edit is not diffable but still persists via fallback save`() {
        val a = (nodeRepo.create("Elena", "Character") as com.lorecanvas.common.LcResult.Ok).value
        val b = (nodeRepo.create("Marcus", "Character") as com.lorecanvas.common.LcResult.Ok).value
        val createCmd = CreateRelationshipCommand(relRepo, a.id, b.id, "Mentor Of")
        history.execute(createCmd)
        val rel = createCmd.createdRelationship!!
        val snapshot = RelationshipEditCommands.Snapshot.of(rel)

        rel.addContext(RelationshipContext.create(startDate = "1000", description = "Fell out"))

        // No type/description change, so the diff finds nothing to wrap in a Command —
        // this matches RelationshipCommands.kt's documented exclusion of addContext.
        assertNull(RelationshipEditCommands.buildSaveCommand(relRepo, rel, snapshot))

        // The UI wiring's onSave always performs this fallback save regardless, so the
        // context isn't silently lost just because it isn't undoable.
        relRepo.save(rel)
        assertTrue(relRepo.get(rel.id)!!.contexts.isNotEmpty(), "addContext must still be persisted via the unconditional fallback save")
    }

    @Test fun `timeline rename now persists to disk (regression test for a real pre-existing bug)`() {
        // Prior to TimelineEditCommands, TimelineEditorScreen's onRename mutated Timeline.name
        // in memory on every keystroke but nothing ever called timelineRepository.save() for it —
        // unlike Node/Card/Relationship, Timeline's screen has no Save button. A renamed
        // Timeline was silently never persisted. This test guards against that regressing.
        val createCmd = CreateTimelineCommand(timelineRepo, "Rise of the Empire")
        history.execute(createCmd)
        val timeline = createCmd.createdTimeline!!
        val nameSnapshot = timeline.name

        timeline.rename("The Long War") // simulates live keystroke mutation, pre-Save

        val renameCommand = TimelineEditCommands.buildRenameCommand(timelineRepo, timeline, nameSnapshot)
        assertNotNull(renameCommand)
        history.execute(renameCommand)

        assertEquals("The Long War", timelineRepo.get(timeline.id)!!.name, "Timeline rename must actually persist")

        history.undo()
        assertEquals("Rise of the Empire", timelineRepo.get(timeline.id)!!.name, "Undo must persist the reverted name too")
    }

    @Test fun `timeline rename with no change returns null`() {
        val createCmd = CreateTimelineCommand(timelineRepo, "Rise of the Empire")
        history.execute(createCmd)
        val timeline = createCmd.createdTimeline!!

        assertNull(TimelineEditCommands.buildRenameCommand(timelineRepo, timeline, timeline.name))
    }
}
