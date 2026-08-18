package com.lorecanvas.storage

import com.lorecanvas.common.LcResult
import com.lorecanvas.domain.Project
import java.io.File

/**
 * ProjectStorage (LCD-007, Chapters 4-9): the real, on-disk storage format
 * for a LoreCanvas project — a directory, not a database (see LCD-007
 * Chapter 2, "Local First" / "Human-Inspectable", and PEP-001's executive
 * summary: "Storage: Offline Structured Files").
 *
 * This supersedes the generic key-value [Storage] interface from Phase 1
 * for real Project persistence — that interface was an intentionally
 * generic placeholder (see its own doc comment); now that LCD-007 defines
 * the actual format, Project persistence needs the shape below instead.
 * [Storage]/[InMemoryStorage] are left in place unmodified — nothing
 * requires removing them, and they may still be useful for simple
 * app-level keyed data (preferences, etc.) later.
 */
data class ProjectSummary(
    val id: String,
    val name: String,
    val directory: File,
    val modifiedAt: String
)

interface ProjectStorage {
    fun createProject(directory: File, project: Project): LcResult<Unit, StorageError>
    fun saveProject(directory: File, project: Project): LcResult<Unit, StorageError>
    fun loadProject(directory: File): LcResult<Project, StorageError>
    fun projectExists(directory: File): Boolean

    /**
     * Lightweight listing for the "Open Existing Project" workflow
     * (LCD-009, Chapter 4) — reads just enough of each project.json to
     * show a name and a last-modified time, without fully validating or
     * loading each project (LCD-007, Chapter 17 — "fast loading").
     */
    fun listProjects(rootDirectory: File): List<ProjectSummary>
}
