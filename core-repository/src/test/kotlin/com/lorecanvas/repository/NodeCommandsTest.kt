package com.lorecanvas.repository

import com.lorecanvas.domain.NodeStatus
import kotlin.test.*

class NodeCommandsTest : CommandTestFixture() {

    @BeforeTest fun setUp() = setUpFixture()
    @AfterTest fun tearDown() = tearDownFixture()

    @Test fun `create then undo removes the node`() {
        val cmd = CreateNodeCommand(nodeRepo, "Alice", "Character", "A wandering scholar.")
        history.execute(cmd)
        val node = cmd.createdNode!!
        assertNotNull(nodeRepo.get(node.id), "Node should exist on disk after create")

        history.undo()
        assertNull(nodeRepo.get(node.id), "Undoing create should delete the node")
    }

    @Test fun `redo after undo of create restores the SAME id, not a new one`() {
        val cmd = CreateNodeCommand(nodeRepo, "Bob", "Character")
        history.execute(cmd)
        val originalId = cmd.createdNode!!.id

        history.undo()
        assertNull(nodeRepo.get(originalId))

        history.redo()
        assertNotNull(nodeRepo.get(originalId), "Redo must restore the exact same id via restore(), not mint a fresh one")
        assertEquals(originalId, cmd.createdNode!!.id, "The command's tracked reference must still point at the original id")
    }

    @Test fun `rename undo and redo`() {
        val cmd = CreateNodeCommand(nodeRepo, "Carol", "Character")
        history.execute(cmd)
        val node = cmd.createdNode!!

        history.execute(RenameNodeCommand(nodeRepo, node, "Carol the Bold"))
        assertEquals("Carol the Bold", node.name)
        assertEquals("Carol the Bold", nodeRepo.get(node.id)!!.name)

        history.undo()
        assertEquals("Carol", node.name)
        assertEquals("Carol", nodeRepo.get(node.id)!!.name, "Undo must persist the reverted state, not just mutate in-memory")

        history.redo()
        assertEquals("Carol the Bold", node.name)
    }

    @Test fun `change type, update summary, add tag, toggle archive each undo correctly`() {
        val cmd = CreateNodeCommand(nodeRepo, "Dave", "Character", "Original summary.")
        history.execute(cmd)
        val node = cmd.createdNode!!

        history.execute(ChangeNodeTypeCommand(nodeRepo, node, "Location"))
        assertEquals("Location", node.type)
        history.undo()
        assertEquals("Character", node.type)

        history.execute(UpdateNodeSummaryCommand(nodeRepo, node, "New summary."))
        history.undo()
        assertEquals("Original summary.", node.summary)

        history.execute(AddNodeTagCommand(nodeRepo, node, "important"))
        assertTrue(node.tags.contains("important"))
        history.undo()
        assertFalse(node.tags.contains("important"))

        assertEquals(NodeStatus.ACTIVE, node.status)
        history.execute(ToggleNodeArchiveCommand(nodeRepo, node))
        assertEquals(NodeStatus.ARCHIVED, node.status)
        history.undo()
        assertEquals(NodeStatus.ACTIVE, node.status)
    }

    @Test fun `adding a tag that already exists does not remove it on undo`() {
        val cmd = CreateNodeCommand(nodeRepo, "Eve", "Character")
        history.execute(cmd)
        val node = cmd.createdNode!!

        history.execute(AddNodeTagCommand(nodeRepo, node, "hero"))
        history.execute(AddNodeTagCommand(nodeRepo, node, "hero")) // duplicate add
        history.undo() // undoes the second (no-op) add
        assertTrue(node.tags.contains("hero"), "Undoing a duplicate add-tag must not remove a tag that was already present before it")
    }
}
