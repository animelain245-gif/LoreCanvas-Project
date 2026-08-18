package com.lorecanvas.validation

import com.lorecanvas.domain.Card
import com.lorecanvas.domain.Node
import com.lorecanvas.domain.Project
import com.lorecanvas.domain.Relationship
import com.lorecanvas.domain.Template
import com.lorecanvas.domain.Timeline
import com.lorecanvas.domain.TimelineEvent

/**
 * Centralized entity validators (Phase 6, Milestone 4). Previously each
 * Repository called `Rules.notBlank(...)` inline and hand-rolled the same
 * "combine results, extract the first error message" pattern itself —
 * this collects that per-entity logic in one place so Repositories ask
 * "is this valid?" instead of each reimplementing the answer. `Rules`
 * itself (the low-level field checks) is unchanged; these are the
 * per-entity policies built from it.
 */
private fun combine(vararg results: ValidationResult): ValidationResult {
    val errors = results.filterIsInstance<ValidationResult.Invalid>().flatMap { it.errors }
    return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
}

object ProjectValidator {
    fun validateForCreate(name: String): ValidationResult = combine(Rules.notBlank("name", name))
    fun validateForSave(project: Project): ValidationResult = validateForCreate(project.name)

    /** LCD-006 Ch.12 — UUID Integrity check, used when opening a persisted Project. */
    fun validateId(id: String): ValidationResult = combine(Rules.isValidUuid("id", id))
}

object NodeValidator {
    fun validateForCreate(name: String, type: String): ValidationResult =
        combine(Rules.notBlank("name", name), Rules.notBlank("type", type))

    fun validateForSave(node: Node): ValidationResult = validateForCreate(node.name, node.type)
}

object CardValidator {
    fun validateForCreate(title: String, type: String): ValidationResult =
        combine(Rules.notBlank("title", title), Rules.notBlank("type", type))

    fun validateForSave(card: Card): ValidationResult = validateForCreate(card.title, card.type)
}

object RelationshipValidator {
    fun validateForCreate(type: String): ValidationResult = combine(Rules.notBlank("type", type))
    fun validateForSave(relationship: Relationship): ValidationResult = validateForCreate(relationship.type)

    /**
     * "Relationship integrity" (this phase's explicit ask): both endpoints
     * must reference Nodes that actually exist. Takes a plain existence
     * check function rather than a Storage/Repository type directly, so
     * this module doesn't need a dependency on either just to express the
     * rule.
     */
    fun validateEndpoints(sourceNodeId: String, targetNodeId: String, nodeExists: (String) -> Boolean): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        if (!nodeExists(sourceNodeId)) errors.add(ValidationError("sourceNodeId", "Source node does not exist."))
        if (!nodeExists(targetNodeId)) errors.add(ValidationError("targetNodeId", "Target node does not exist."))
        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
}

object TimelineValidator {
    fun validateForCreate(name: String): ValidationResult = combine(Rules.notBlank("name", name))
    fun validateForSave(timeline: Timeline): ValidationResult = validateForCreate(timeline.name)

    fun validateEvent(event: TimelineEvent): ValidationResult =
        combine(Rules.notBlank("date", event.date), Rules.notBlank("title", event.title))
}

object TemplateValidator {
    fun validateForCreate(name: String, targetNodeType: String): ValidationResult =
        combine(Rules.notBlank("name", name), Rules.notBlank("targetNodeType", targetNodeType))

    fun validateForSave(template: Template): ValidationResult = validateForCreate(template.name, template.targetNodeType)
}
