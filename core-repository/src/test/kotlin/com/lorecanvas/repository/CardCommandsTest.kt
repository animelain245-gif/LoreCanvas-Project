package com.lorecanvas.repository

import kotlin.test.*

class CardCommandsTest : CommandTestFixture() {

    @BeforeTest fun setUp() = setUpFixture()
    @AfterTest fun tearDown() = tearDownFixture()

    private fun aNode() = (nodeRepo.create("Alice", "Character") as com.lorecanvas.common.LcResult.Ok).value

    @Test fun `create then undo removes the card`() {
        val node = aNode()
        val cmd = CreateCardCommand(cardRepo, node.id, "Bio", "Rich Text", "Alice is brave.")
        history.execute(cmd)
        val card = cmd.createdCard!!
        assertNotNull(cardRepo.get(card.id))

        history.undo()
        assertNull(cardRepo.get(card.id))
    }

    @Test fun `redo after undo of create restores the SAME id`() {
        val node = aNode()
        val cmd = CreateCardCommand(cardRepo, node.id, "Notes", "Markdown")
        history.execute(cmd)
        val originalId = cmd.createdCard!!.id

        history.undo()
        history.redo()
        assertNotNull(cardRepo.get(originalId))
        assertEquals(originalId, cmd.createdCard!!.id)
    }

    @Test fun `rename, change type, update content, add tag each undo correctly`() {
        val node = aNode()
        val cmd = CreateCardCommand(cardRepo, node.id, "Bio", "Rich Text", "Original content.")
        history.execute(cmd)
        val card = cmd.createdCard!!

        history.execute(RenameCardCommand(cardRepo, card, "Biography"))
        history.undo()
        assertEquals("Bio", card.title)

        history.execute(ChangeCardTypeCommand(cardRepo, card, "Markdown"))
        history.undo()
        assertEquals("Rich Text", card.type)

        history.execute(UpdateCardContentCommand(cardRepo, card, "New content."))
        history.undo()
        assertEquals("Original content.", card.content)

        history.execute(AddCardTagCommand(cardRepo, card, "important"))
        history.undo()
        assertFalse(card.tags.contains("important"))
    }

    @Test fun `reorder cards undo restores the previous full ordering`() {
        val node = aNode()
        val c1 = (cardRepo.create(node.id, "First", "Note") as com.lorecanvas.common.LcResult.Ok).value
        val c2 = (cardRepo.create(node.id, "Second", "Note") as com.lorecanvas.common.LcResult.Ok).value
        val c3 = (cardRepo.create(node.id, "Third", "Note") as com.lorecanvas.common.LcResult.Ok).value
        val originalOrder = listOf(c1.id, c2.id, c3.id)
        val newOrder = listOf(c3.id, c1.id, c2.id)

        history.execute(ReorderCardsCommand(cardRepo, originalOrder, newOrder))
        val reordered = cardRepo.listForNode(node.id).sortedBy { it.order }.map { it.id }
        assertEquals(newOrder, reordered, "Reorder should apply the new ordering")

        history.undo()
        val restored = cardRepo.listForNode(node.id).sortedBy { it.order }.map { it.id }
        assertEquals(originalOrder, restored, "Undo must restore the exact previous ordering")
    }
}
