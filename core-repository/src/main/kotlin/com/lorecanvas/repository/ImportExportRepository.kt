package com.lorecanvas.repository

import com.lorecanvas.common.LcResult
import com.lorecanvas.common.Logger
import com.lorecanvas.common.createLogger
import com.lorecanvas.storage.CardStorage
import com.lorecanvas.storage.FileManager
import com.lorecanvas.storage.NodeStorage
import com.lorecanvas.storage.RealFileManager
import com.lorecanvas.storage.RelationshipStorage
import com.lorecanvas.storage.StorageError
import com.lorecanvas.storage.StorageErrorType
import com.lorecanvas.storage.TimelineStorage
import com.lorecanvas.storage.serialization.CURRENT_EXPORT_FORMAT_VERSION
import com.lorecanvas.storage.serialization.ExportBundle
import com.lorecanvas.storage.serialization.ExportBundleSerializer
import java.io.File
import java.time.Instant

data class ImportSummary(val nodesImported: Int, val cardsImported: Int, val relationshipsImported: Int, val timelinesImported: Int)

/**
 * ImportExportRepository — Phase "Import/Export" (PEP-001), LCD-009 Ch.15.
 *
 * Export: Choose Entities -> Serialize -> Package Files -> Export
 * Complete. Import: Select File -> Read Data -> Validate -> Create Backup
 * -> Import -> Refresh Repository ("Refresh Repository" is the caller's
 * job — this returns a summary, the UI/caller re-lists whatever it's
 * displaying).
 *
 * Conflict handling: if any imported entity's UUID already exists in the
 * current project, the whole import is refused *before* anything is
 * written or backed up — "the project must never be left in an
 * intermediate state" (LCD-006 Ch.2) applies here just as much as to a
 * single Repository transaction. Remapping colliding UUIDs and rewriting
 * every cross-reference (Card.parentNodeId, Relationship endpoints,
 * Timeline.relatedNodeIds) would avoid this, but is real added complexity
 * for a collision that's astronomically unlikely between two independently
 * generated UUID sets — refusing with a clear reason is the honest,
 * simpler choice for now.
 */
class ImportExportRepository(
    private val projectDirectory: File,
    private val nodeStorage: NodeStorage,
    private val cardStorage: CardStorage,
    private val relationshipStorage: RelationshipStorage,
    private val timelineStorage: TimelineStorage,
    private val fileManager: FileManager = RealFileManager(),
    private val logger: Logger = createLogger("ImportExportRepository")
) {

    /** Export everything in the project — Nodes, Cards, Relationships, Timelines. */
    fun exportAll(outputFile: File): LcResult<Unit, RepositoryError> {
        val bundle = ExportBundle(
            nodes = nodeStorage.listNodes(projectDirectory),
            cards = cardStorage.listCards(projectDirectory),
            relationships = relationshipStorage.listRelationships(projectDirectory),
            timelines = timelineStorage.listTimelines(projectDirectory)
        )
        return writeBundle(bundle, outputFile)
    }

    /**
     * Export a subset of Nodes plus their own Cards and any Relationship
     * whose *both* endpoints are in the selection. Timelines are
     * project-wide rather than Node-owned (LCD-005 Ch.18 — Relationships,
     * Timeline Events, and Templates are "owned by the Project while
     * referencing Nodes"), so a partial-Node export deliberately excludes
     * them; use [exportAll] for a full project export that includes
     * Timelines.
     */
    fun exportSelection(nodeIds: Set<String>, outputFile: File): LcResult<Unit, RepositoryError> {
        val nodes = nodeStorage.listNodes(projectDirectory).filter { it.id in nodeIds }
        val cards = cardStorage.listCards(projectDirectory).filter { it.parentNodeId in nodeIds }
        val relationships = relationshipStorage.listRelationships(projectDirectory)
            .filter { it.sourceNodeId in nodeIds && it.targetNodeId in nodeIds }
        val bundle = ExportBundle(nodes, cards, relationships, timelines = emptyList())
        return writeBundle(bundle, outputFile)
    }

    private fun writeBundle(bundle: ExportBundle, outputFile: File): LcResult<Unit, RepositoryError> = try {
        outputFile.parentFile?.let { fileManager.createDirectories(it) }
        fileManager.writeTextAtomic(outputFile, ExportBundleSerializer.toJson(bundle))
        logger.info("Exported bundle", "${bundle.nodes.size} nodes, ${bundle.cards.size} cards, ${bundle.relationships.size} relationships, ${bundle.timelines.size} timelines -> ${outputFile.name}")
        LcResult.ok(Unit)
    } catch (e: Exception) {
        LcResult.fail(RepositoryError.Storage(StorageError(StorageErrorType.IO_ERROR, e.message ?: "Failed to write export file.")))
    }

    fun import(inputFile: File): LcResult<ImportSummary, RepositoryError> {
        // 1. Read Data
        val raw = try {
            fileManager.readText(inputFile)
        } catch (e: Exception) {
            return LcResult.fail(RepositoryError.Storage(StorageError(StorageErrorType.IO_ERROR, e.message ?: "Failed to read import file.")))
        }

        // 2. Validate
        val bundle = when (val result = ExportBundleSerializer.fromJson(raw)) {
            is ExportBundleSerializer.DeserializeResult.Success -> result.bundle
            is ExportBundleSerializer.DeserializeResult.Malformed ->
                return LcResult.fail(RepositoryError.ValidationFailed("Import file is not a valid LoreCanvas export: ${result.reason}"))
        }

        // Version compatibility (LCD-009 Ch.15). Only one format version has
        // ever shipped, so there's nothing to migrate *from* yet — this
        // check exists so a future format bump has somewhere to plug in a
        // real migration step, rather than silently misreading old data.
        if (bundle.formatVersion != CURRENT_EXPORT_FORMAT_VERSION) {
            return LcResult.fail(
                RepositoryError.ValidationFailed(
                    "This export was made with format version '${bundle.formatVersion}', but this version of LoreCanvas " +
                        "supports '$CURRENT_EXPORT_FORMAT_VERSION'. A migration path may be added in a future version."
                )
            )
        }

        val conflicts = mutableListOf<String>()
        bundle.nodes.forEach { if (nodeStorage.nodeExists(projectDirectory, it.id)) conflicts.add("node ${it.id}") }
        bundle.cards.forEach { if (cardStorage.cardExists(projectDirectory, it.id)) conflicts.add("card ${it.id}") }
        bundle.relationships.forEach { if (relationshipStorage.relationshipExists(projectDirectory, it.id)) conflicts.add("relationship ${it.id}") }
        bundle.timelines.forEach { if (timelineStorage.timelineExists(projectDirectory, it.id)) conflicts.add("timeline ${it.id}") }
        if (conflicts.isNotEmpty()) {
            return LcResult.fail(
                RepositoryError.ValidationFailed(
                    "Import refused: ${conflicts.size} item(s) already exist in this project (e.g. ${conflicts.first()})."
                )
            )
        }

        // 3. Create Backup
        val backupDir = File(projectDirectory, "backups/import-${Instant.now().toString().replace(":", "-")}")
        try {
            listOf("nodes", "cards", "relationships", "timelines").forEach { sub ->
                val src = File(projectDirectory, sub)
                if (fileManager.exists(src)) fileManager.copyDirectory(src, File(backupDir, sub))
            }
            val projectJson = File(projectDirectory, "project.json")
            if (fileManager.exists(projectJson)) {
                fileManager.createDirectories(backupDir)
                fileManager.writeTextAtomic(File(backupDir, "project.json"), fileManager.readText(projectJson))
            }
        } catch (e: Exception) {
            return LcResult.fail(RepositoryError.Storage(StorageError(StorageErrorType.IO_ERROR, "Failed to create pre-import backup: ${e.message}")))
        }

        // 4. Import
        bundle.nodes.forEach { nodeStorage.createNode(projectDirectory, it) }
        bundle.cards.forEach { cardStorage.createCard(projectDirectory, it) }
        bundle.relationships.forEach { relationshipStorage.createRelationship(projectDirectory, it) }
        bundle.timelines.forEach { timelineStorage.createTimeline(projectDirectory, it) }

        logger.info("Import complete", "backup at ${backupDir.name}")
        return LcResult.ok(
            ImportSummary(
                nodesImported = bundle.nodes.size,
                cardsImported = bundle.cards.size,
                relationshipsImported = bundle.relationships.size,
                timelinesImported = bundle.timelines.size
            )
        )
    }
}
