package com.lorecanvas.repository

import com.lorecanvas.commands.CommandHistory
import com.lorecanvas.events.EventBus
import com.lorecanvas.storage.CardFileStorage
import com.lorecanvas.storage.NodeFileStorage
import com.lorecanvas.storage.RelationshipFileStorage
import com.lorecanvas.storage.TimelineFileStorage
import java.nio.file.Files
import kotlin.io.path.absolutePathString

/**
 * Shared fixture for the Command test suite (PEP-001 Phase 6, "Command/
 * Undo-Redo Completion"). Each test gets its own real temp directory and
 * real file-backed repositories — these are integration tests against
 * actual [com.lorecanvas.storage] implementations, not mocks, matching
 * project rule 9: "testing must be reproducible" and real repositories,
 * not a scratch script standing in for them.
 */
abstract class CommandTestFixture {
    protected lateinit var projectDir: java.io.File
    protected lateinit var eventBus: EventBus
    protected lateinit var nodeRepo: NodeRepository
    protected lateinit var cardRepo: CardRepository
    protected lateinit var relRepo: RelationshipRepository
    protected lateinit var timelineRepo: TimelineRepository
    protected lateinit var history: CommandHistory

    protected fun setUpFixture() {
        projectDir = Files.createTempDirectory("lorecanvas_test_").toFile()
        eventBus = EventBus()
        val nodeFileStorage = NodeFileStorage()
        val cardFileStorage = CardFileStorage()
        val relationshipFileStorage = RelationshipFileStorage()
        val timelineFileStorage = TimelineFileStorage()
        // Wired identically to LoreCanvasApp.kt's real construction — NodeRepository's
        // cardStorage/relationshipStorage/timelineStorage params are optional (default
        // null), which silently disables parts of delete()'s dependents-check if left
        // unwired. Found via audit that this fixture previously omitted them, meaning
        // no test in this suite had ever actually exercised that safety check.
        nodeRepo = NodeRepository(
            projectDir, nodeFileStorage, eventBus,
            cardStorage = cardFileStorage,
            relationshipStorage = relationshipFileStorage,
            timelineStorage = timelineFileStorage
        )
        cardRepo = CardRepository(projectDir, cardFileStorage, nodeFileStorage, eventBus)
        relRepo = RelationshipRepository(projectDir, relationshipFileStorage, nodeFileStorage, eventBus)
        timelineRepo = TimelineRepository(projectDir, timelineFileStorage, eventBus)
        history = CommandHistory()
    }

    protected fun tearDownFixture() {
        projectDir.deleteRecursively()
    }
}
