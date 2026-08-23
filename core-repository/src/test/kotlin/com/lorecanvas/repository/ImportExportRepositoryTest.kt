package com.lorecanvas.repository

import com.lorecanvas.common.UuidService
import com.lorecanvas.events.EventBus
import com.lorecanvas.storage.CardFileStorage
import com.lorecanvas.storage.NodeFileStorage
import com.lorecanvas.storage.RelationshipFileStorage
import com.lorecanvas.storage.RealFileManager
import com.lorecanvas.storage.TimelineFileStorage
import java.nio.file.Files
import kotlin.test.*

/**
 * Regression tests for a real path-traversal issue found during a
 * security audit: [ImportExportRepository.import] used to pass each
 * imported entity's `id` field straight to Storage without checking it
 * was actually a UUID first — every FileStorage implementation builds its
 * file path as `File(dir, "$id.json")` with no sanitization of its own.
 * A crafted import bundle with an id like "../../../../somewhere" could
 * write a file outside the project's intended nodes/cards/relationships/
 * timelines subdirectories.
 */
class ImportExportRepositoryTest : CommandTestFixture() {

    private lateinit var importExportRepo: ImportExportRepository

    @BeforeTest
    fun setUp() {
        setUpFixture()
        importExportRepo = ImportExportRepository(
            projectDirectory = projectDir,
            nodeStorage = NodeFileStorage(),
            cardStorage = CardFileStorage(),
            relationshipStorage = RelationshipFileStorage(),
            timelineStorage = TimelineFileStorage()
        )
    }

    @AfterTest fun tearDown() = tearDownFixture()

    /** Hand-built JSON, exactly what an attacker-supplied .json import file could contain. */
    private fun maliciousBundleJson(maliciousId: String): String = """
        {
          "exportFormatVersion": "1.0",
          "nodes": [
            {
              "id": "$maliciousId",
              "name": "Evil",
              "type": "Character",
              "summary": "",
              "status": "ACTIVE",
              "tags": [],
              "createdAt": "2026-01-01T00:00:00Z",
              "modifiedAt": "2026-01-01T00:00:00Z"
            }
          ],
          "cards": [],
          "relationships": [],
          "timelines": []
        }
    """.trimIndent()

    @Test fun `import refuses a node id that is not a valid UUID`() {
        val importFile = java.io.File(Files.createTempDirectory("lc_import_test").toFile(), "malicious.json")
        importFile.writeText(maliciousBundleJson("../../../../escaped-outside-project"))

        val result = importExportRepo.import(importFile)
        assertTrue(result is com.lorecanvas.common.LcResult.Fail, "Import must be refused when an id is not a valid UUID")
        val message = result.error.userMessage()
        assertTrue(message.contains("invalid id"), "Must be refused for the invalid-id reason specifically, not a JSON parse error. Got: $message")

        // Confirm nothing escaped: no file was written anywhere above the project directory's parent.
        val escapedFile = java.io.File(projectDir.parentFile, "escaped-outside-project.json")
        assertFalse(escapedFile.exists(), "The path-traversal target file must never be created")

        // Confirm nothing was written inside the project either — the import was refused
        // before any write, exactly like the existing conflict-check does.
        assertTrue(nodeRepo.list().isEmpty(), "No node should exist after a refused import")
    }

    @Test fun `import refuses a plain non-UUID string id too (not just path traversal payloads)`() {
        val importFile = java.io.File(Files.createTempDirectory("lc_import_test").toFile(), "bad_id.json")
        importFile.writeText(maliciousBundleJson("not-a-uuid-at-all"))

        val result = importExportRepo.import(importFile)
        assertTrue(result is com.lorecanvas.common.LcResult.Fail)
    }

    @Test fun `import succeeds normally when every id is a real UUID`() {
        val validId = UuidService.generate()
        val importFile = java.io.File(Files.createTempDirectory("lc_import_test").toFile(), "valid.json")
        importFile.writeText(maliciousBundleJson(validId))

        val result = importExportRepo.import(importFile)
        assertTrue(result is com.lorecanvas.common.LcResult.Ok, "A bundle with valid UUIDs must still import successfully")
        assertNotNull(nodeRepo.get(validId), "The imported node should exist under its own valid id")
    }

    @Test fun `export then import round-trip still works after the fix`() {
        val createCmd = CreateNodeCommand(nodeRepo, "Alice", "Character", "A scholar.")
        history.execute(createCmd)
        val node = createCmd.createdNode!!

        val exportFile = java.io.File(Files.createTempDirectory("lc_export_test").toFile(), "export.json")
        val exportResult = importExportRepo.exportAll(exportFile)
        assertTrue(exportResult is com.lorecanvas.common.LcResult.Ok)

        // Import into a fresh, separate project directory (importing back into the same
        // one would hit the pre-existing conflict check, which is a different code path).
        val freshProjectDir = Files.createTempDirectory("lc_fresh_project").toFile()
        val freshImportExport = ImportExportRepository(
            projectDirectory = freshProjectDir,
            nodeStorage = NodeFileStorage(),
            cardStorage = CardFileStorage(),
            relationshipStorage = RelationshipFileStorage(),
            timelineStorage = TimelineFileStorage()
        )
        val importResult = freshImportExport.import(exportFile)
        assertTrue(importResult is com.lorecanvas.common.LcResult.Ok, "A legitimate export must still round-trip through import successfully")

        val freshNodeRepo = NodeRepository(freshProjectDir, NodeFileStorage(), EventBus())
        assertNotNull(freshNodeRepo.get(node.id))
        assertEquals("Alice", freshNodeRepo.get(node.id)!!.name)

        freshProjectDir.deleteRecursively()
    }

    @Test fun `import refuses a card that references a node not included in the bundle`() {
        val danglingId = UuidService.generate() // not present in "nodes" below
        val importFile = java.io.File(Files.createTempDirectory("lc_import_test").toFile(), "dangling_card.json")
        importFile.writeText("""
            {
              "exportFormatVersion": "1.0",
              "nodes": [],
              "cards": [
                {
                  "id": "${UuidService.generate()}",
                  "parentNodeId": "$danglingId",
                  "title": "Orphan Card",
                  "type": "Note",
                  "content": "",
                  "order": 0,
                  "tags": [],
                  "createdAt": "2026-01-01T00:00:00Z",
                  "modifiedAt": "2026-01-01T00:00:00Z"
                }
              ],
              "relationships": [],
              "timelines": []
            }
        """.trimIndent())

        val result = importExportRepo.import(importFile)
        assertTrue(result is com.lorecanvas.common.LcResult.Fail, "Import must refuse a card whose parentNodeId isn't in the bundle")
        val message = result.error.userMessage()
        assertTrue(message.contains("not included in this import"), "Must be refused for the dangling-reference reason specifically, not a JSON parse error. Got: $message")
        assertTrue(cardRepo.get(danglingId) == null)
    }

    @Test fun `import refuses a relationship whose endpoints are not included in the bundle`() {
        val importFile = java.io.File(Files.createTempDirectory("lc_import_test").toFile(), "dangling_rel.json")
        importFile.writeText("""
            {
              "exportFormatVersion": "1.0",
              "nodes": [],
              "cards": [],
              "relationships": [
                {
                  "id": "${UuidService.generate()}",
                  "sourceNodeId": "${UuidService.generate()}",
                  "targetNodeId": "${UuidService.generate()}",
                  "type": "Friend",
                  "direction": "DIRECTED",
                  "description": "",
                  "status": "Active",
                  "contexts": [],
                  "createdAt": "2026-01-01T00:00:00Z",
                  "modifiedAt": "2026-01-01T00:00:00Z"
                }
              ],
              "timelines": []
            }
        """.trimIndent())

        val result = importExportRepo.import(importFile)
        assertTrue(result is com.lorecanvas.common.LcResult.Fail, "Import must refuse a relationship whose endpoints aren't in the bundle")
        val message = result.error.userMessage()
        assertTrue(message.contains("not included in this import"), "Must be refused for the dangling-reference reason specifically, not a JSON parse error. Got: $message")
    }
}
