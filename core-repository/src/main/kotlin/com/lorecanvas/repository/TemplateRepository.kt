package com.lorecanvas.repository

import com.lorecanvas.common.LcResult
import com.lorecanvas.common.Logger
import com.lorecanvas.common.createLogger
import com.lorecanvas.domain.BuiltInTemplates
import com.lorecanvas.domain.Node
import com.lorecanvas.domain.Template
import com.lorecanvas.events.EventBus
import com.lorecanvas.storage.TemplateStorage
import com.lorecanvas.validation.TemplateValidator
import com.lorecanvas.validation.ValidationResult
import java.io.File

/**
 * TemplateRepository — Phase 10 "Template System" (PEP-001), LCD-009
 * Ch.13. Depends on [NodeRepository]/[CardRepository] (not just their
 * storages) because "Applying a Template" is itself a Create-Node-then-
 * Create-Cards workflow, and those Repositories already own that
 * validation/event-publishing logic — reusing them here means a template-
 * created Node is indistinguishable from a manually-created one to every
 * other subsystem (Search, Graph, dependency checks, all just see a Node).
 */
class TemplateRepository(
    private val projectDirectory: File,
    private val templateStorage: TemplateStorage,
    private val nodeRepository: NodeRepository,
    private val cardRepository: CardRepository,
    private val eventBus: EventBus,
    private val logger: Logger = createLogger("TemplateRepository")
) {

    fun create(template: Template): LcResult<Template, RepositoryError> {
        val validation = TemplateValidator.validateForSave(template)
        if (validation is ValidationResult.Invalid) {
            return LcResult.fail(RepositoryError.ValidationFailed(validation.errors.joinToString { it.message }))
        }
        return when (val result = templateStorage.createTemplate(projectDirectory, template)) {
            is LcResult.Fail -> LcResult.fail(RepositoryError.Storage(result.error))
            is LcResult.Ok -> {
                eventBus.publish(TemplateEvent.TemplateCreated(template.id))
                LcResult.ok(template)
            }
        }
    }

    /** "Save As Template" (LCD-009 Ch.13) — captures the Node's current Cards; never touches the Node or its Cards. */
    fun createFromNode(name: String, node: Node): LcResult<Template, RepositoryError> {
        val validation = TemplateValidator.validateForCreate(name, node.type)
        if (validation is ValidationResult.Invalid) {
            return LcResult.fail(RepositoryError.ValidationFailed(validation.errors.joinToString { it.message }))
        }
        val cards = cardRepository.listForNode(node.id)
        val template = Template.fromNode(name, node, cards)
        return create(template)
    }

    /**
     * "Applying a Template" (LCD-009 Ch.13): Choose Template -> Create New
     * Node -> Generate Default Cards -> Open Editor. Never modifies the
     * template itself. If Card creation fails partway through, the
     * already-created Node and any Cards created so far are left in
     * place rather than silently rolled back — the same "cannot exist
     * without" invariants Card/Node already enforce mean a partial result
     * here is still internally consistent, just incomplete; the caller
     * can retry adding the missing Card(s) directly.
     */
    fun apply(template: Template, nodeName: String): LcResult<Node, RepositoryError> {
        val createResult = nodeRepository.create(name = nodeName, type = template.targetNodeType)
        val node = when (createResult) {
            is LcResult.Ok -> createResult.value
            is LcResult.Fail -> return createResult
        }

        for (spec in template.defaultCards) {
            val cardResult = cardRepository.create(node.id, spec.title, spec.type, spec.content)
            if (cardResult is LcResult.Fail) {
                logger.warn("Template application partially failed on card '${spec.title}'", cardResult.error.userMessage())
            }
        }

        logger.info("Template applied", "template=${template.id} node=${node.id}")
        eventBus.publish(TemplateEvent.TemplateApplied(template.id, node.id))
        return LcResult.ok(node)
    }

    fun delete(templateId: String): LcResult<Unit, RepositoryError> {
        if (BuiltInTemplates.ALL.any { it.id == templateId }) {
            return LcResult.fail(RepositoryError.ValidationFailed("Built-in templates can't be deleted."))
        }
        return when (val result = templateStorage.deleteTemplate(projectDirectory, templateId)) {
            is LcResult.Fail -> LcResult.fail(RepositoryError.Storage(result.error))
            is LcResult.Ok -> {
                eventBus.publish(TemplateEvent.TemplateDeleted(templateId))
                LcResult.ok(Unit)
            }
        }
    }

    /** Built-ins (Phase 7 — "Built-in and user templates") first, then whatever the user has saved. */
    fun list(): List<Template> = BuiltInTemplates.ALL + templateStorage.listTemplates(projectDirectory)
}
