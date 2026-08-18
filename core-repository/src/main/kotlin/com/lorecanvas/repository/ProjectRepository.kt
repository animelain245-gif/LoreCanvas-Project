package com.lorecanvas.repository

import com.lorecanvas.common.LcResult
import com.lorecanvas.common.Logger
import com.lorecanvas.common.createLogger
import com.lorecanvas.domain.Project
import com.lorecanvas.events.EventBus
import com.lorecanvas.storage.ProjectStorage
import com.lorecanvas.storage.ProjectSummary
import com.lorecanvas.validation.ProjectValidator
import com.lorecanvas.validation.ValidationResult
import java.io.File

/**
 * ProjectRepository — Phase 2 "Project System" (PEP-001).
 *
 * Implements the four workflows LCD-009 defines for Projects:
 *   - Chapter 3, Project Creation:  Validate -> Repository -> Storage -> Event
 *   - Chapter 4, Project Opening:   Storage -> Validate -> Repository -> Event
 *   - Chapter 5, Save:              Repository -> Storage -> Event
 *   - Project Closing (not its own LCD-009 chapter; a small, necessary
 *     extension of the same pattern — see the `close()` doc comment below)
 *
 * Rules enforced here, from LCD-004 Chapter 6 (unchanged since Phase 1):
 *   - may access Domain objects
 *   - may access Storage
 *   - may emit Events
 *   - must NEVER access UI components directly
 *
 * Phase 1 -> Phase 2 change: this class now depends on [ProjectStorage]
 * (the real, file-based persistence from LCD-007) instead of the generic
 * Phase 1 [com.lorecanvas.storage.Storage] key-value interface. That
 * generic interface was explicitly a placeholder — see its doc comment —
 * so completing it here isn't a deviation from the Phase 1 architecture,
 * it's what Phase 1 said would happen once the real storage format
 * existed. [Transaction], [ProjectEvent], the Domain layer, and the
 * Validation layer are all reused unchanged from Phase 1.
 */
class ProjectRepository(
    private val projectStorage: ProjectStorage,
    private val eventBus: EventBus,
    private val logger: Logger = createLogger("ProjectRepository")
) {

    private val transaction = Transaction<Project?>()

    /** The Project currently open in the Workspace, if any (LCD-009's Workspace concept). */
    var currentProject: Project? = null
        private set

    var currentProjectDirectory: File? = null
        private set

    /**
     * Create Project (LCD-009, Chapter 3): "Open Workspace -> Create Node
     * Collection -> Create Timeline Collection -> Create Template
     * Collection -> Default Settings... Repository -> Validation -> Storage
     * -> Event."
     */
    fun create(directory: File, name: String): LcResult<Project, RepositoryError> {
        val nameValidation = ProjectValidator.validateForCreate(name)

        val transactionResult = transaction.run(
            previousState = null,
            validate = {
                if (nameValidation is ValidationResult.Invalid) {
                    ValidationOutcome.Invalid(nameValidation.errors.joinToString { it.message })
                } else {
                    ValidationOutcome.Valid
                }
            },
            execute = { Project.create(name = name) }
        )

        val project = when (transactionResult) {
            is LcResult.Fail -> {
                logger.warn("Project creation failed validation", transactionResult.error.reason)
                return LcResult.fail(RepositoryError.ValidationFailed(transactionResult.error.reason))
            }
            is LcResult.Ok -> transactionResult.value
                ?: return LcResult.fail(RepositoryError.ValidationFailed("Transaction produced no Project."))
        }

        return when (val storageResult = projectStorage.createProject(directory, project)) {
            is LcResult.Fail -> {
                logger.error("Project creation failed to persist", storageResult.error.message)
                LcResult.fail(RepositoryError.Storage(storageResult.error))
            }
            is LcResult.Ok -> {
                currentProject = project
                currentProjectDirectory = directory
                logger.info("Project created", project.id)
                eventBus.publish(ProjectEvent.ProjectCreated(project.id))
                LcResult.ok(project)
            }
        }
    }

    /**
     * Open Project (LCD-009, Chapter 4): "Locate Files -> Deserialize ->
     * Repository -> Validate -> Search Index -> Workspace -> Open Editors...
     * Project Version, Required Files, UUID Integrity, and Reference
     * Integrity must all be verified" before opening completes.
     *
     * Required Files and basic parse-ability are checked by
     * [ProjectStorage] itself (LCD-007, Chapter 15). Project Version and
     * UUID Integrity are checked here, since they're business rules, not
     * storage concerns (LCD-007, Chapter 10). Reference Integrity has
     * nothing to check yet — there are no Nodes or Relationships until
     * Phase 3/5 — so it's a no-op for now rather than skipped silently:
     * see the comment at the bottom of this method.
     */
    fun open(directory: File): LcResult<Project, RepositoryError> {
        val loaded = when (val storageResult = projectStorage.loadProject(directory)) {
            is LcResult.Fail -> {
                logger.warn("Failed to open project", storageResult.error.message)
                return LcResult.fail(RepositoryError.Storage(storageResult.error))
            }
            is LcResult.Ok -> storageResult.value
        }

        val uuidCheck = ProjectValidator.validateId(loaded.id)
        if (uuidCheck is ValidationResult.Invalid) {
            val reason = uuidCheck.errors.joinToString { it.message }
            logger.error("Project failed UUID integrity check", reason)
            return LcResult.fail(RepositoryError.ValidationFailed(reason))
        }

        if (loaded.version != SUPPORTED_PROJECT_VERSION) {
            val reason = "Project format version '${loaded.version}' is not supported by this version of LoreCanvas " +
                "(supported: $SUPPORTED_PROJECT_VERSION). A future version will add migration (LCD-007, Chapter 14)."
            logger.warn("Unsupported project version", reason)
            return LcResult.fail(RepositoryError.ValidationFailed(reason))
        }

        // Reference Integrity (LCD-009, Ch.4): nothing to validate until
        // Nodes/Relationships exist (Phase 3/5). Intentionally a no-op,
        // not an omission — revisit once those entities are real.

        currentProject = loaded
        currentProjectDirectory = directory
        logger.info("Project opened", loaded.id)
        eventBus.publish(ProjectEvent.ProjectOpened(loaded.id))
        return LcResult.ok(loaded)
    }

    /**
     * Save (LCD-009, Chapter 5): "Repository -> Serialize -> Temporary
     * Files -> Integrity Check -> Replace Existing Files." The temp-file
     * and integrity-check steps live in [com.lorecanvas.storage.FileManager];
     * this method is the Repository-level trigger + event publication.
     */
    fun save(): LcResult<Unit, RepositoryError> {
        val project = currentProject ?: return LcResult.fail(RepositoryError.NoProjectOpen)
        val directory = currentProjectDirectory ?: return LcResult.fail(RepositoryError.NoProjectOpen)

        return when (val storageResult = projectStorage.saveProject(directory, project)) {
            is LcResult.Fail -> {
                logger.error("Save failed", storageResult.error.message)
                LcResult.fail(RepositoryError.Storage(storageResult.error))
            }
            is LcResult.Ok -> {
                logger.info("Project saved", project.id)
                eventBus.publish(ProjectEvent.ProjectSaved(project.id))
                LcResult.ok(Unit)
            }
        }
    }

    /**
     * Close Project. LCD-009 doesn't give this its own chapter the way
     * Create/Open/Save get one, but PEP-001's Phase 2 deliverables
     * explicitly list "Close Project" alongside the other three, so this
     * fills that gap with the same pattern as the rest of this class:
     * clear the Workspace's active project and publish the corresponding
     * event from LCD-006 Chapter 13 ("Project Closed"). It does not
     * currently prompt for unsaved changes — that's a Workspace/UI-layer
     * concern for a later phase (LCD-012 Phase 9, "Workspace" — "Session
     * persistence"), not something this Repository method decides on its
     * own.
     */
    fun close(): LcResult<Unit, RepositoryError> {
        val project = currentProject ?: return LcResult.fail(RepositoryError.NoProjectOpen)

        currentProject = null
        currentProjectDirectory = null
        logger.info("Project closed", project.id)
        eventBus.publish(ProjectEvent.ProjectClosed(project.id))
        return LcResult.ok(Unit)
    }

    /** LCD-009, Chapter 4's project-picking step — see [ProjectStorage.listProjects]. */
    fun listProjects(rootDirectory: File): List<ProjectSummary> = projectStorage.listProjects(rootDirectory)

    companion object {
        const val SUPPORTED_PROJECT_VERSION = "1.0"
    }
}
