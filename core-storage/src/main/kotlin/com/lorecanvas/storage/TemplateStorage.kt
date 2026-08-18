package com.lorecanvas.storage

import com.lorecanvas.common.LcResult
import com.lorecanvas.domain.Template
import java.io.File

interface TemplateStorage {
    fun createTemplate(projectDirectory: File, template: Template): LcResult<Unit, StorageError>
    fun saveTemplate(projectDirectory: File, template: Template): LcResult<Unit, StorageError>
    fun deleteTemplate(projectDirectory: File, templateId: String): LcResult<Unit, StorageError>
    fun templateExists(projectDirectory: File, templateId: String): Boolean
    fun listTemplates(projectDirectory: File): List<Template>
}
