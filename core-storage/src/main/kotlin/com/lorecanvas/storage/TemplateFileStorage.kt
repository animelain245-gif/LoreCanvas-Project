package com.lorecanvas.storage

import com.lorecanvas.common.LcResult
import com.lorecanvas.common.Logger
import com.lorecanvas.common.createLogger
import com.lorecanvas.domain.Template
import com.lorecanvas.storage.serialization.TemplateSerializer
import java.io.File

class TemplateFileStorage(
    private val fileManager: FileManager = RealFileManager(),
    private val logger: Logger = createLogger("TemplateFileStorage")
) : TemplateStorage {

    companion object {
        const val TEMPLATES_SUBDIRECTORY = "templates"
    }

    private fun dir(projectDirectory: File) = File(projectDirectory, TEMPLATES_SUBDIRECTORY)
    private fun file(projectDirectory: File, id: String) = File(dir(projectDirectory), "$id.json")

    override fun createTemplate(projectDirectory: File, template: Template): LcResult<Unit, StorageError> {
        if (templateExists(projectDirectory, template.id)) {
            return LcResult.fail(StorageError(StorageErrorType.ALREADY_EXISTS, "A template with id ${template.id} already exists."))
        }
        return write(projectDirectory, template)
    }

    override fun saveTemplate(projectDirectory: File, template: Template): LcResult<Unit, StorageError> {
        if (!templateExists(projectDirectory, template.id)) {
            return LcResult.fail(StorageError(StorageErrorType.NOT_FOUND, "No template with id ${template.id}."))
        }
        return write(projectDirectory, template)
    }

    private fun write(projectDirectory: File, template: Template): LcResult<Unit, StorageError> = try {
        fileManager.createDirectories(dir(projectDirectory))
        fileManager.writeTextAtomic(file(projectDirectory, template.id), TemplateSerializer.toJson(template))
        LcResult.ok(Unit)
    } catch (e: Exception) {
        LcResult.fail(StorageError(StorageErrorType.IO_ERROR, e.message ?: "Failed to write template."))
    }

    override fun deleteTemplate(projectDirectory: File, templateId: String): LcResult<Unit, StorageError> {
        val f = file(projectDirectory, templateId)
        if (!fileManager.exists(f)) {
            return LcResult.fail(StorageError(StorageErrorType.NOT_FOUND, "No template with id $templateId."))
        }
        return try {
            fileManager.deleteRecursively(f)
            LcResult.ok(Unit)
        } catch (e: Exception) {
            LcResult.fail(StorageError(StorageErrorType.IO_ERROR, e.message ?: "Failed to delete template."))
        }
    }

    override fun templateExists(projectDirectory: File, templateId: String): Boolean =
        fileManager.exists(file(projectDirectory, templateId))

    override fun listTemplates(projectDirectory: File): List<Template> {
        val directory = dir(projectDirectory)
        if (!fileManager.exists(directory)) return emptyList()
        return fileManager.listFiles(directory, ".json").mapNotNull { f ->
            val raw = try {
                fileManager.readText(f)
            } catch (e: Exception) {
                logger.warn("Skipping unreadable template file", f.name)
                return@mapNotNull null
            }
            when (val result = TemplateSerializer.fromJson(raw)) {
                is TemplateSerializer.DeserializeResult.Success -> result.template
                is TemplateSerializer.DeserializeResult.Malformed -> {
                    logger.warn("Skipping corrupt template file", "${f.name}: ${result.reason}")
                    null
                }
            }
        }
    }
}
