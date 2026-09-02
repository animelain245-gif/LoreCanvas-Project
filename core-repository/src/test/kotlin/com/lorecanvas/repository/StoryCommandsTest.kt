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

    @Test
    fun testAddChapterCreatesCorrectParentAndOrder() {
        val story = nodeRepo.create("Story", NodeTypes.STORY).let { (it as com.lorecanvas.common.LcResult.Ok).value }
        
        val cmd1 = CreateChapterCommand(nodeRepo, story.id, "C1")
        history.execute(cmd1)
        assertEquals(0, cmd1.createdChapter?.displayOrder)
        assertEquals(story.id, cmd1.createdChapter?.parentNodeId)

        val cmd2 = CreateChapterCommand(nodeRepo, story.id, "C2")
        history.execute(cmd2)
        assertEquals(1, cmd2.createdChapter?.displayOrder)
    }

    @Test
    fun testAddSceneCreatesSceneAndProseCard() {
        val chapter = nodeRepo.create("Chapter", NodeTypes.CHAPTER).let { (it as com.lorecanvas.common.LcResult.Ok).value }
        
        val cmd = CreateSceneCommand(nodeRepo, cardRepo, chapter.id, "S1")
        history.execute(cmd)
        
        assertNotNull(cmd.createdScene)
        assertNotNull(cmd.createdProseCard)
        assertEquals(chapter.id, cmd.createdScene?.parentNodeId)
        assertEquals(cmd.createdScene?.id, cmd.createdProseCard?.parentNodeId)
        assertEquals(CardTypes.PROSE, cmd.createdProseCard?.type)
    }

    @Test
    fun testMoveSceneBetweenChapters() {
        val s1 = nodeRepo.create("S1", NodeTypes.STORY).let { (it as com.lorecanvas.common.LcResult.Ok).value }
        val c1 = nodeRepo.create("C1", NodeTypes.CHAPTER, parentNodeId = s1.id).let { (it as com.lorecanvas.common.LcResult.Ok).value }
        val c2 = nodeRepo.create("C2", NodeTypes.CHAPTER, parentNodeId = s1.id).let { (it as com.lorecanvas.common.LcResult.Ok).value }
        
        val scene = nodeRepo.create("Scene", NodeTypes.SCENE, parentNodeId = c1.id, displayOrder = 0).let { (it as com.lorecanvas.common.LcResult.Ok).value }
        
        val moveCmd = MoveNodeCommand(nodeRepo, scene.id, c2.id)
        history.execute(moveCmd)
        
        assertEquals(c2.id, nodeRepo.get(scene.id)?.parentNodeId)
        assertEquals(0, nodeRepo.get(scene.id)?.displayOrder)
        
        history.undo()
        assertEquals(c1.id, nodeRepo.get(scene.id)?.parentNodeId)
        assertEquals(0, nodeRepo.get(scene.id)?.displayOrder)
    }

    @Test
    fun testDeleteSceneNormalizesRemainingSiblings() {
        val chapter = nodeRepo.create("Chapter", NodeTypes.CHAPTER).let { (it as com.lorecanvas.common.LcResult.Ok).value }
        val s1 = nodeRepo.create("S1", NodeTypes.SCENE, parentNodeId = chapter.id, displayOrder = 0).let { (it as com.lorecanvas.common.LcResult.Ok).value }
        val s2 = nodeRepo.create("S2", NodeTypes.SCENE, parentNodeId = chapter.id, displayOrder = 1).let { (it as com.lorecanvas.common.LcResult.Ok).value }
        val s3 = nodeRepo.create("S3", NodeTypes.SCENE, parentNodeId = chapter.id, displayOrder = 2).let { (it as com.lorecanvas.common.LcResult.Ok).value }

        val deleteCmd = DeleteStoryHierarchyCommand(nodeRepo, cardRepo, s2.id)
        history.execute(deleteCmd)

        assertEquals(0, nodeRepo.get(s1.id)?.displayOrder)
        assertEquals(1, nodeRepo.get(s3.id)?.displayOrder) // S3 shifted from 2 to 1
    }

    @Test
    fun testMoveSceneNormalizesBothChapters() {
        val s1_story = nodeRepo.create("S1_STORY", NodeTypes.STORY).let { (it as com.lorecanvas.common.LcResult.Ok).value }
        val c1 = nodeRepo.create("C1", NodeTypes.CHAPTER, parentNodeId = s1_story.id).let { (it as com.lorecanvas.common.LcResult.Ok).value }
        val c2 = nodeRepo.create("C2", NodeTypes.CHAPTER, parentNodeId = s1_story.id).let { (it as com.lorecanvas.common.LcResult.Ok).value }

        val scene1 = nodeRepo.create("S1", NodeTypes.SCENE, parentNodeId = c1.id, displayOrder = 0).let { (it as com.lorecanvas.common.LcResult.Ok).value }
        val scene2 = nodeRepo.create("S2", NodeTypes.SCENE, parentNodeId = c1.id, displayOrder = 1).let { (it as com.lorecanvas.common.LcResult.Ok).value }

        val moveCmd = MoveNodeCommand(nodeRepo, scene1.id, c2.id)
        history.execute(moveCmd)

        assertEquals(0, nodeRepo.get(scene2.id)?.displayOrder) // S2 shifted from 1 to 0 in C1
        assertEquals(0, nodeRepo.get(scene1.id)?.displayOrder) // S1 is 0 in C2
    }

    @Test
    fun testCreateStoryHierarchySequencing() {
        val story = nodeRepo.create("Story", NodeTypes.STORY).let { (it as com.lorecanvas.common.LcResult.Ok).value }
        val chapter = nodeRepo.create("Chapter", NodeTypes.CHAPTER, parentNodeId = story.id, displayOrder = 0).let { (it as com.lorecanvas.common.LcResult.Ok).value }
        
        // Add a second chapter, verify order is 1
        val cmd = CreateChapterCommand(nodeRepo, story.id, "Chapter 2")
        history.execute(cmd)
        assertEquals(1, cmd.createdChapter?.displayOrder)
        
        // Add a scene to the first chapter, verify order is 0 (as getNextOrder would return)
        val sceneCmd = CreateSceneCommand(nodeRepo, cardRepo, chapter.id, "Scene 1")
        history.execute(sceneCmd)
        assertEquals(0, sceneCmd.createdScene?.displayOrder)
    }

    @Test
    fun testCreateStoryHierarchyRollbackOnPartialFailure() {
        // Construct a CardRepository with a storage that fails on every create
        val failingCardRepo = CardRepository(
            projectDirectory = projectDir,
            cardStorage = object : com.lorecanvas.storage.CardStorage {
                override fun createCard(projectDirectory: File, card: com.lorecanvas.domain.Card) = 
                    com.lorecanvas.common.LcResult.fail(com.lorecanvas.storage.StorageError(com.lorecanvas.storage.StorageErrorType.IO_ERROR, "Simulated failure"))
                override fun saveCard(projectDirectory: File, card: com.lorecanvas.domain.Card) = com.lorecanvas.common.LcResult.ok(Unit)
                override fun loadCard(projectDirectory: File, cardId: String) = com.lorecanvas.common.LcResult.fail(com.lorecanvas.storage.StorageError(com.lorecanvas.storage.StorageErrorType.NOT_FOUND, ""))
                override fun deleteCard(projectDirectory: File, cardId: String) = com.lorecanvas.common.LcResult.ok(Unit)
                override fun cardExists(projectDirectory: File, cardId: String) = false
                override fun listCards(projectDirectory: File) = emptyList<com.lorecanvas.domain.Card>()
                override fun deleteCardsForNode(projectDirectory: File, nodeId: String) = com.lorecanvas.common.LcResult.ok(Unit)
            },
            nodeStorage = com.lorecanvas.storage.NodeFileStorage(),
            eventBus = eventBus
        )

        val command = CreateStoryHierarchyCommand(nodeRepo, failingCardRepo, "Failing Story")
        
        // This will attempt to create Story -> Chapter -> Scene -> Prose (FAIL)
        command.execute()
        
        // Created references should be cleared by rollback()
        assertTrue(command.createdStory == null, "Story reference should be cleared")
        assertTrue(command.createdScene == null, "Scene reference should be cleared")
        
        // Repository should be empty (rollback deleted the Nodes)
        assertTrue(nodeRepo.list().isEmpty(), "Node repository should be empty after rollback")
    }

    @Test
    fun testPinPersists() {
        val node = nodeRepo.create("Node", "Type").let { (it as com.lorecanvas.common.LcResult.Ok).value }
        assertFalse(node.isPinned)
        
        val cmd = ToggleNodePinCommand(nodeRepo, node)
        history.execute(cmd)
        assertTrue(nodeRepo.get(node.id)!!.isPinned)
        
        history.undo()
        assertFalse(nodeRepo.get(node.id)!!.isPinned)
    }

    @Test
    fun testProsePersistence() {
        val scene = nodeRepo.create("Scene", NodeTypes.SCENE).let { (it as com.lorecanvas.common.LcResult.Ok).value }
        val card = cardRepo.create(scene.id, "Prose", CardTypes.PROSE).let { (it as com.lorecanvas.common.LcResult.Ok).value }
        
        val newContent = "Some new prose content"
        card.updateContent(newContent)
        
        // Simulate save logic in LoreCanvasApp
        val oldSnapshot = CardEditCommands.Snapshot(card.title, card.type, "", emptyList())
        val saveCmd = CardEditCommands.buildSaveCommand(cardRepo, card, oldSnapshot)
        assertNotNull(saveCmd)
        history.execute(saveCmd)
        
        // Verify it reached the storage
        val reloaded = cardRepo.get(card.id)
        assertEquals(newContent, reloaded?.content)
    }

    private fun assertFalse(condition: Boolean) {
        if (condition) throw AssertionError("Expected false")
    }
}
