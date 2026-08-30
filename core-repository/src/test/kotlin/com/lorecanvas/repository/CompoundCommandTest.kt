package com.lorecanvas.repository

import com.lorecanvas.commands.CompoundCommand
import kotlin.test.*

class CompoundCommandTest : CommandTestFixture() {

    @BeforeTest fun setUp() = setUpFixture()
    @AfterTest fun tearDown() = tearDownFixture()

    @Test fun `execute applies all child commands, undo reverses all in one step`() {
        val node = (nodeRepo.create("Dave", "Character") as com.lorecanvas.common.LcResult.Ok).value
        val compound = CompoundCommand(
            "Rename and Retag Dave",
            listOf(
                RenameNodeCommand(nodeRepo, node, "Dave the Bold"),
                AddNodeTagCommand(nodeRepo, node, "hero")
            )
        )

        history.execute(compound)
        assertEquals("Dave the Bold", node.name)
        assertTrue(node.tags.contains("hero"))

        history.undo()
        assertEquals("Dave", node.name, "Compound undo must reverse every child command")
        assertFalse(node.tags.contains("hero"))

        history.redo()
        assertEquals("Dave the Bold", node.name)
        assertTrue(node.tags.contains("hero"))
    }

    @Test fun `child commands undo in reverse order`() {
        // Order matters when a later command depends on state an earlier one established.
        // This test just confirms reverse-order undo happens at all, via a sequence where
        // forward order is externally observable through the resulting field value.
        val node = (nodeRepo.create("Erin", "Character") as com.lorecanvas.common.LcResult.Ok).value
        val compound = CompoundCommand(
            "Two renames",
            listOf(
                RenameNodeCommand(nodeRepo, node, "Erin Two"),
                RenameNodeCommand(nodeRepo, node, "Erin Three")
            )
        )
        history.execute(compound)
        assertEquals("Erin Three", node.name)

        history.undo()
        // Second rename's undo runs first (restoring "Erin Two"), then first rename's undo
        // (restoring "Erin") — net result is back to the original, verifying full reversal.
        assertEquals("Erin", node.name)
    }
}
