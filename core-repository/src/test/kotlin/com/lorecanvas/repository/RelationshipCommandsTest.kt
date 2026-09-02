package com.lorecanvas.repository

import kotlin.test.*

class RelationshipCommandsTest : CommandTestFixture() {

    @BeforeTest fun setUp() = setUpFixture()
    @AfterTest fun tearDown() = tearDownFixture()

    private fun aNode(name: String) = (nodeRepo.create(name, "Character") as com.lorecanvas.common.LcResult.Ok).value

    @Test fun `create then undo removes the relationship`() {
        val a = aNode("Alice"); val b = aNode("Bob")
        val cmd = CreateRelationshipCommand(relRepo, a.id, b.id, "Friend")
        history.execute(cmd)
        val rel = cmd.createdRelationship!!
        assertNotNull(relRepo.get(rel.id))

        history.undo()
        assertNull(relRepo.get(rel.id))
    }

    @Test fun `redo after undo of create restores the SAME id`() {
        val a = aNode("Alice"); val b = aNode("Bob")
        val cmd = CreateRelationshipCommand(relRepo, a.id, b.id, "Sibling")
        history.execute(cmd)
        val originalId = cmd.createdRelationship!!.id

        history.undo()
        history.redo()
        assertNotNull(relRepo.get(originalId))
        assertEquals(originalId, cmd.createdRelationship!!.id)
    }

    @Test fun `change type, update description, update status each undo correctly`() {
        val a = aNode("Alice"); val b = aNode("Bob")
        val cmd = CreateRelationshipCommand(relRepo, a.id, b.id, "Friend")
        history.execute(cmd)
        val rel = cmd.createdRelationship!!

        history.execute(ChangeRelationshipTypeCommand(relRepo, rel, "Rival"))
        history.undo()
        assertEquals("Friend", rel.type)

        history.execute(UpdateRelationshipDescriptionCommand(relRepo, rel, "Complicated history."))
        history.undo()
        assertEquals("", rel.description)

        history.execute(UpdateRelationshipStatusCommand(relRepo, rel, "Estranged"))
        history.undo()
        assertEquals("Active", rel.status)
    }

    @Test fun `addContext is not wrapped in a Command by design`() {
        val a = aNode("Alice"); val b = aNode("Bob")
        val cmd = CreateRelationshipCommand(relRepo, a.id, b.id, "Friend")
        history.execute(cmd)
        val rel = cmd.createdRelationship!!

        // Per RelationshipCommands.kt's documented decision: addContext has no Command wrapper,
        // since undoing it would need a removeContext() domain capability that doesn't exist.
        rel.addContext(com.lorecanvas.domain.RelationshipContext.create(startDate = "1000", description = "Fell out"))
        relRepo.save(rel)
        assertTrue(relRepo.get(rel.id)!!.contexts.isNotEmpty(), "addContext must still persist even though it's not undoable")
    }
}
