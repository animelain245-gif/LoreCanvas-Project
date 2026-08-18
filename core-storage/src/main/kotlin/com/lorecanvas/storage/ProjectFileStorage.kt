package com.lorecanvas.storage

import com.lorecanvas.common.LcResult
import com.lorecanvas.common.Logger
import com.lorecanvas.common.createLogger
import com.lorecanvas.domain.Project
import com.lorecanvas.storage.serialization.ProjectSerializer
import java.io.File

/**
 * ProjectFileStorage — the real implementation of [ProjectStorage].
 *
 * Lays out each project exactly as LCD-007, Chapter 4 specifies:
 *
 * ```
 * <ProjectDirectory>/
 * ├── project.json
 * ├── nodes/
 * ├── cards/
 * ├── relationships/
 * ├── timelines/
 * ├── templates/
 * ├── attachments/
 * ├── cache/
 * └── backups/
 * ```
 *
 * Only `project.json` is populated in Phase 2 — the rest are created empty
 * now so the on-disk shape is right from day one; Phase 3+ will start
 * writing into `nodes/`, `cards/`, etc. as those entities are implemented.
 */
class ProjectFileStorage(
    private val fileManager: FileManager = RealFileManager(),
    private val logger: Logger = createLogger("ProjectFileStorage")
) : ProjectStorage {

    companion object {
        const val PROJECT_FILE_NAME = "project.json"
        val ENTITY_SUBDIRECTORIES = listOf(
            "nodes", "cards", "relationships", "timelines", "templates", "attachments", "cache", "backups"
        )
    }

    private fun projectFile(directory: File): File = File(directory, PROJECT_FILE_NAME)

    override fun createProject(directory: File, project: Project): LcResult<Unit, StorageError> {
        if (projectExists(directory)) {
            return LcResult.fail(
                StorageError(StorageErrorType.ALREADY_EXISTS, "A project already exists at ${directory.absolutePath}")
            )
        }

        return try {
            fileManager.createDirectories(directory)
            for (sub in ENTITY_SUBDIRECTORIES) {
                fileManager.createDirectories(File(directory, sub))
            }
            fileManager.writeTextAtomic(projectFile(directory), ProjectSerializer.toJson(project))
            logger.info("Project created on disk", directory.absolutePath)
            LcResult.ok(Unit)
        } catch (e: Exception) {
            logger.error("Failed to create project on disk", e.message)
            LcResult.fail(StorageError(StorageErrorType.IO_ERROR, e.message ?: "Failed to create project directory."))
        }
    }

    override fun saveProject(directory: File, project: Project): LcResult<Unit, StorageError> {
        if (!projectExists(directory)) {
            return LcResult.fail(
                StorageError(StorageErrorType.NOT_FOUND, "No project found at ${directory.absolutePath}")
            )
        }

        return try {
            fileManager.writeTextAtomic(projectFile(directory), ProjectSerializer.toJson(project))
            logger.info("Project saved", directory.absolutePath)
            LcResult.ok(Unit)
        } catch (e: Exception) {
            logger.error("Failed to save project", e.message)
            LcResult.fail(StorageError(StorageErrorType.IO_ERROR, e.message ?: "Failed to save project."))
        }
    }

    override fun loadProject(directory: File): LcResult<Project, StorageError> {
        // LCD-007, Chapter 15 (Corruption Detection): check for missing
        // files before attempting to parse anything.
        if (!fileManager.exists(directory)) {
            return LcResult.fail(
                StorageError(StorageErrorType.NOT_FOUND, "Project directory does not exist: ${directory.absolutePath}")
            )
        }
        val file = projectFile(directory)
        if (!fileManager.exists(file)) {
            return LcResult.fail(
                StorageError(StorageErrorType.NOT_FOUND, "Missing $PROJECT_FILE_NAME in ${directory.absolutePath}")
            )
        }

        val raw = try {
            fileManager.readText(file)
        } catch (e: Exception) {
            return LcResult.fail(StorageError(StorageErrorType.IO_ERROR, e.message ?: "Failed to read $PROJECT_FILE_NAME"))
        }

        return when (val result = ProjectSerializer.fromJson(raw)) {
            is ProjectSerializer.DeserializeResult.Success -> LcResult.ok(result.project)
            is ProjectSerializer.DeserializeResult.Malformed -> {
                logger.error("Corrupt project file", result.reason)
                LcResult.fail(StorageError(StorageErrorType.CORRUPT_DATA, result.reason))
            }
        }
    }

    override fun projectExists(directory: File): Boolean =
        fileManager.exists(directory) && fileManager.exists(projectFile(directory))

    override fun listProjects(rootDirectory: File): List<ProjectSummary> {
        if (!fileManager.exists(rootDirectory)) return emptyList()

        return fileManager.listDirectories(rootDirectory).mapNotNull { dir ->
            if (!projectExists(dir)) return@mapNotNull null
            when (val result = loadProject(dir)) {
                is LcResult.Ok -> ProjectSummary(
                    id = result.value.id,
                    name = result.value.name,
                    directory = dir,
                    modifiedAt = result.value.modifiedAt
                )
                is LcResult.Fail -> {
                    logger.warn("Skipping unreadable project during listing", "${dir.absolutePath}: ${result.error.message}")
                    null
                }
            }
        }.sortedByDescending { it.modifiedAt }
    }
}
