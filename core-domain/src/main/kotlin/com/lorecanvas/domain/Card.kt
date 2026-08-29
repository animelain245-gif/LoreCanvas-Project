package com.lorecanvas.domain

import com.lorecanvas.common.UuidService
import java.time.Instant

/** LCD-005 Ch.10 — starting suggestions; Card Type stays a free-form String, same reasoning as NodeTypes. */
object CardTypes {
    const val PROSE = "Prose"

    val SUGGESTED: List<String> = listOf(
        PROSE, "Rich Text", "Markdown", "Statistics", "Image", "Table",
        "Checklist", "Quote", "Reference", "External Link", "Notes"
    )
}

/**
 * Card — "the primary container for detailed information" (LCD-005 Ch.10).
 * A Node represents *what* something is; a Card stores *everything known*
 * about it. Always belongs to exactly one Node (`parentNodeId`) — "it
 * cannot exist independently."
 */
class Card private constructor(
    id: String,
    createdAt: String,
    modifiedAt: String,
    val parentNodeId: String,
    initialTitle: String,
    initialType: String,
    initialContent: String,
    initialOrder: Int,
    initialTags: List<String>
) : Entity(id = id, createdAt = createdAt, modifiedAt = modifiedAt) {

    var title: String = initialTitle
        private set

    var type: String = initialType
        private set

    var content: String = initialContent
        private set

    /** Position among sibling Cards on the same Node — LCD-006 Ch.7's "Reorder Cards." */
    var order: Int = initialOrder
        private set

    private val mutableTags: MutableList<String> = initialTags.toMutableList()
    val tags: List<String> get() = mutableTags.toList()

    init {
        require(title.isNotBlank()) { "Card title must not be empty." }
        require(type.isNotBlank()) { "Card type must not be empty." }
        require(parentNodeId.isNotBlank()) { "Card must have a parent node." }
    }

    fun rename(newTitle: String) {
        require(newTitle.isNotBlank()) { "Card title must not be empty." }
        title = newTitle.trim()
        touch()
    }

    fun changeType(newType: String) {
        require(newType.isNotBlank()) { "Card type must not be empty." }
        type = newType.trim()
        touch()
    }

    fun updateContent(newContent: String) {
        content = newContent
        touch()
    }

    fun moveTo(newOrder: Int) {
        order = newOrder
        touch()
    }

    fun addTag(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isEmpty()) return
        if (!mutableTags.contains(trimmed)) {
            mutableTags.add(trimmed)
            touch()
        }
    }

    fun removeTag(tag: String) {
        if (mutableTags.remove(tag)) touch()
    }

    companion object {
        fun create(parentNodeId: String, title: String, type: String, content: String = "", order: Int = 0): Card {
            val now = Instant.now().toString()
            return Card(UuidService.generate(), now, now, parentNodeId, title.trim(), type.trim(), content, order, emptyList())
        }

        fun restore(
            id: String,
            createdAt: String,
            modifiedAt: String,
            parentNodeId: String,
            title: String,
            type: String,
            content: String,
            order: Int,
            tags: List<String>
        ): Card = Card(id, createdAt, modifiedAt, parentNodeId, title.trim(), type.trim(), content, order, tags)

        /** Duplicate Card (LCD-006 Ch.7) — new identity, same content, never touches the original. */
        fun duplicateOf(source: Card, newOrder: Int): Card {
            val now = Instant.now().toString()
            return Card(
                UuidService.generate(), now, now, source.parentNodeId,
                "${source.title} (Copy)", source.type, source.content, newOrder, source.tags
            )
        }
    }
}
