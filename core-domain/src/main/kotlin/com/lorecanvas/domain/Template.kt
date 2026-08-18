package com.lorecanvas.domain

import com.lorecanvas.common.UuidService
import java.time.Instant

/** A Card to auto-generate when a Template is applied (LCD-009 Ch.13 — "Generate Default Cards"). */
data class DefaultCardSpec(val title: String, val type: String, val content: String = "")

/**
 * Template categories (Phase 7 — "Categories"). Free-form-friendly like
 * Node/Card/Relationship types (a curated starting list, not a closed
 * enum), consistent with how every other "type-ish" field in this domain
 * model works.
 */
object TemplateCategories {
    val SUGGESTED: List<String> = listOf("Character", "Location", "Organization", "Item", "Event", "Custom")
    const val DEFAULT: String = "Custom"
}

/**
 * Template (LCD-005 Ch.16) — "Instead of repeatedly creating the same
 * Cards manually, a Template defines the initial structure." Applying a
 * Template never modifies the original (LCD-009 Ch.13).
 *
 * [isBuiltIn] distinguishes the small set of templates LoreCanvas ships
 * with (see [BuiltInTemplates]) from ones a user created — built-ins are
 * never persisted to a Project's `templates/` directory and can't be
 * deleted, only ever offered as a starting point (see
 * [com.lorecanvas.repository.TemplateRepository.list]).
 */
class Template private constructor(
    id: String,
    createdAt: String,
    modifiedAt: String,
    initialName: String,
    initialTargetNodeType: String,
    initialCategory: String,
    initialDefaultCards: List<DefaultCardSpec>,
    initialDefaultMetadata: Map<String, Any?>,
    val isBuiltIn: Boolean
) : Entity(id = id, createdAt = createdAt, modifiedAt = modifiedAt) {

    var name: String = initialName
        private set

    var targetNodeType: String = initialTargetNodeType
        private set

    var category: String = initialCategory
        private set

    private val mutableDefaultCards: MutableList<DefaultCardSpec> = initialDefaultCards.toMutableList()
    val defaultCards: List<DefaultCardSpec> get() = mutableDefaultCards.toList()

    private val mutableDefaultMetadata: MutableMap<String, Any?> = initialDefaultMetadata.toMutableMap()
    val defaultMetadata: Map<String, Any?> get() = mutableDefaultMetadata.toMap()

    init {
        require(name.isNotBlank()) { "Template name must not be empty." }
        require(targetNodeType.isNotBlank()) { "Template target node type must not be empty." }
    }

    fun rename(newName: String) {
        require(newName.isNotBlank()) { "Template name must not be empty." }
        name = newName.trim()
        touch()
    }

    fun changeCategory(newCategory: String) {
        category = newCategory.ifBlank { TemplateCategories.DEFAULT }
        touch()
    }

    fun addDefaultCard(spec: DefaultCardSpec) {
        mutableDefaultCards.add(spec)
        touch()
    }

    fun removeDefaultCard(index: Int) {
        if (index in mutableDefaultCards.indices) {
            mutableDefaultCards.removeAt(index)
            touch()
        }
    }

    companion object {
        fun create(
            name: String,
            targetNodeType: String,
            category: String = TemplateCategories.DEFAULT,
            defaultCards: List<DefaultCardSpec> = emptyList(),
            defaultMetadata: Map<String, Any?> = emptyMap()
        ): Template {
            val now = Instant.now().toString()
            return Template(UuidService.generate(), now, now, name.trim(), targetNodeType.trim(), category.ifBlank { TemplateCategories.DEFAULT }, defaultCards, defaultMetadata, isBuiltIn = false)
        }

        fun restore(
            id: String,
            createdAt: String,
            modifiedAt: String,
            name: String,
            targetNodeType: String,
            category: String,
            defaultCards: List<DefaultCardSpec>,
            defaultMetadata: Map<String, Any?>
        ): Template = Template(id, createdAt, modifiedAt, name.trim(), targetNodeType.trim(), category.ifBlank { TemplateCategories.DEFAULT }, defaultCards, defaultMetadata, isBuiltIn = false)

        /** "Save As Template" (LCD-009 Ch.13) — captures an existing Node's Card structure without referencing the Node itself. */
        fun fromNode(name: String, node: Node, cards: List<Card>, category: String = TemplateCategories.DEFAULT): Template {
            val specs = cards.sortedBy { it.order }.map { DefaultCardSpec(it.title, it.type, it.content) }
            return create(name = name, targetNodeType = node.type, category = category, defaultCards = specs)
        }

        /** Built-in templates use this instead of [create] — see [BuiltInTemplates]. */
        internal fun builtIn(id: String, name: String, targetNodeType: String, category: String, defaultCards: List<DefaultCardSpec>): Template {
            val now = Instant.now().toString()
            return Template(id, now, now, name, targetNodeType, category, defaultCards, emptyMap(), isBuiltIn = true)
        }
    }
}

/**
 * A small set of templates LoreCanvas ships with, always available in
 * every project without the user having to build them first (Phase 7 —
 * "Built-in and user templates"). Fixed UUIDs so they're stable across
 * app restarts rather than regenerating (and therefore duplicating in
 * any UI that keys off template id) every time they're listed.
 */
object BuiltInTemplates {
    val ALL: List<Template> = listOf(
        Template.builtIn(
            id = "builtin-template-character",
            name = "Basic Character",
            targetNodeType = "Character",
            category = "Character",
            defaultCards = listOf(
                DefaultCardSpec("Biography", "Rich Text"),
                DefaultCardSpec("Appearance", "Notes"),
                DefaultCardSpec("Personality", "Notes")
            )
        ),
        Template.builtIn(
            id = "builtin-template-location",
            name = "Basic Location",
            targetNodeType = "Location",
            category = "Location",
            defaultCards = listOf(
                DefaultCardSpec("Description", "Rich Text"),
                DefaultCardSpec("Notable Residents", "Notes")
            )
        ),
        Template.builtIn(
            id = "builtin-template-organization",
            name = "Basic Organization",
            targetNodeType = "Organization",
            category = "Organization",
            defaultCards = listOf(
                DefaultCardSpec("Purpose", "Notes"),
                DefaultCardSpec("Leadership", "Notes"),
                DefaultCardSpec("History", "Rich Text")
            )
        )
    )
}
