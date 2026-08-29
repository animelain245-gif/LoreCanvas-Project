package com.lorecanvas.repository

import com.lorecanvas.commands.CommandHistory
import com.lorecanvas.domain.NodeTypes
import com.lorecanvas.domain.CardTypes
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.BeforeTest
import kotlin.test.AfterTest

class StoryCommandsTest : CommandTestFixture() {

    @BeforeTest
    fun setUp() = setUpFixture()

    @AfterTest
    fun tearDown() = tearDownFixture()

    @Test
    fun testStartWritingCreatesHierarchy() {
        val command = CreateStoryHierarchyCommand(nodeRepo, cardRepo, "Test Story")
        
        history.execute(command)
        
        assertNotNull(command.createdStory)
        assertEquals(NodeTypes.STORY, command.createdStory?.type)
        assertEquals("Test Story", command.createdStory?.name)
        
        assertNotNull(command.createdChapter)
        assertEquals(NodeTypes.CHAPTER, command.createdChapter?.type)
        assertEquals(command.createdStory?.id, command.createdChapter?.parentNodeId)
        
        assertNotNull(command.createdScene)
        assertEquals(NodeTypes.SCENE, command.createdScene?.type)
        assertEquals(command.createdChapter?.id, command.createdScene?.parentNodeId)
        
        assertNotNull(command.createdProseCard)
        assertEquals(CardTypes.PROSE, command.createdProseCard?.type)
        assertEquals(command.createdScene?.id, command.createdProseCard?.parentNodeId)
    }

    @Test
    fun testDeleteStoryHierarchyCascades() {
        val createCmd = CreateStoryHierarchyCommand(nodeRepo, cardRepo, "Test Story")
        history.execute(createCmd)
        
        val storyId = createCmd.createdStory!!.id
        val chapterId = createCmd.createdChapter!!.id
        val sceneId = createCmd.createdScene!!.id
        val proseId = createCmd.createdProseCard!!.id
        
        val deleteCmd = DeleteStoryHierarchyCommand(nodeRepo, cardRepo, storyId)
        history.execute(deleteCmd)
        
        // Verify all deleted
        assertTrue(nodeRepo.list().none { it.id == storyId || it.id == chapterId || it.id == sceneId })
        assertTrue(cardRepo.listAll().none { it.id == proseId })
        
        // Undo delete
        history.undo()
        
        // Verify all restored
        assertNotNull(nodeRepo.get(storyId))
        assertNotNull(nodeRepo.get(chapterId))
        assertNotNull(nodeRepo.get(sceneId))
        assertNotNull(cardRepo.listAll().find { it.id == proseId })
    }

    @Test
    fun testReorderSiblings() {
        val story = nodeRepo.create("Story", NodeTypes.STORY).let { (it as com.lorecanvas.common.LcResult.Ok).value }
        
        val c1 = nodeRepo.create("C1", NodeTypes.CHAPTER, parentNodeId = story.id, displayOrder = 0).let { (it as com.lorecanvas.common.LcResult.Ok).value }
        val c2 = nodeRepo.create("C2", NodeTypes.CHAPTER, parentNodeId = story.id, displayOrder = 1).let { (it as com.lorecanvas.common.LcResult.Ok).value }
        
        val reorderCmd = ReorderStoryNodeCommand(nodeRepo, listOf(c1.id, c2.id), listOf(c2.id, c1.id))
        history.execute(reorderCmd)
        
        assertEquals(0, nodeRepo.get(c2.id)?.displayOrder)
        assertEquals(1, nodeRepo.get(c1.id)?.displayOrder)
        
        history.undo()
        
        assertEquals(0, nodeRepo.get(c1.id)?.displayOrder)
        assertEquals(1, nodeRepo.get(c2.id)?.displayOrder)
    }
}
