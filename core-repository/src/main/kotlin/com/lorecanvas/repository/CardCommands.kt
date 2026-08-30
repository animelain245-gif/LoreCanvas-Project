package com.lorecanvas.repository

import com.lorecanvas.commands.Command
import com.lorecanvas.domain.Card

/**
 * Card Commands — the Card-system counterpart to [NodeCommands], following
 * the exact same pattern proven there (LCD-009 Ch.18: "Undo Request ->
 * History Stack -> Restore Previous State -> Repository -> Publish Event
 * -> Refresh UI").
 *
 * Scope note, same reasoning as [NodeCommands]: Create/Rename/ChangeType/
 * UpdateContent/Move/AddTag are all cleanly reversible — either the
 * Command captures the prior value, or (for Create) [Card.create] mints a
 * fresh UUID so undo can simply delete the Card it just made. **Delete is
 * deliberately not wrapped in a Command here either**, for the identical
 * reason given in [NodeCommands]: there is no "restore with the original
 * id" operation, and faking one would either change the restored Card's
 * identity or require a new Storage capability this pass didn't build.
 */
class CreateCardCommand(
    private val cardRepository: CardRepository,
    private val parentNodeId: String,
    private val title: String,
    private val type: String,
    private val content: String = ""
) : Command {
    override val label: String = "Create Card"
    var createdCard: Card? = null
        private set
    var lastError: RepositoryError? = null
        private set

    /** See [CreateNodeCommand]'s doc comment on this same field — redo must re-insert the same Card via [CardRepository.restore], not mint a new UUID or call [CardRepository.save] (which requires the id already exist). */
    private var hasCreatedOnce = false

    override fun execute() {
        if (!hasCreatedOnce) {
            val result = cardRepository.create(parentNodeId, title, type, content)
            when (result) {
                is com.lorecanvas.common.LcResult.Ok -> {
                    createdCard = result.value
                    hasCreatedOnce = true
                }
                is com.lorecanvas.common.LcResult.Fail -> lastError = result.error
            }
        } else {
            createdCard?.let { cardRepository.restore(it) }
        }
    }

    /** Undoing a create removes the Card — the one case where this module does call delete(), on a Card this same command just made. */
    override fun undo() {
        createdCard?.let { cardRepository.delete(it.id) }
    }
}

class RenameCardCommand(private val cardRepository: CardRepository, private val card: Card, private val newTitle: String) : Command {
    override val label: String = "Rename Card"
    private var previousTitle: String = card.title

    override fun execute() {
        previousTitle = card.title
        card.rename(newTitle)
        cardRepository.save(card)
    }

    override fun undo() {
        card.rename(previousTitle)
        cardRepository.save(card)
    }
}

class ChangeCardTypeCommand(private val cardRepository: CardRepository, private val card: Card, private val newType: String) : Command {
    override val label: String = "Change Card Type"
    private var previousType: String = card.type

    override fun execute() {
        previousType = card.type
        card.changeType(newType)
        cardRepository.save(card)
    }

    override fun undo() {
        card.changeType(previousType)
        cardRepository.save(card)
    }
}

class UpdateCardContentCommand(private val cardRepository: CardRepository, private val card: Card, private val newContent: String) : Command {
    override val label: String = "Edit Card Content"
    private var previousContent: String = card.content

    override fun execute() {
        previousContent = card.content
        card.updateContent(newContent)
        cardRepository.save(card)
    }

    override fun undo() {
        card.updateContent(previousContent)
        cardRepository.save(card)
    }
}

class AddCardTagCommand(private val cardRepository: CardRepository, private val card: Card, private val tag: String) : Command {
    override val label: String = "Add Tag"
    private var actuallyAdded: Boolean = false

    override fun execute() {
        actuallyAdded = !card.tags.contains(tag.trim())
        card.addTag(tag)
        cardRepository.save(card)
    }

    override fun undo() {
        if (actuallyAdded) {
            card.removeTag(tag.trim())
            cardRepository.save(card)
        }
    }
}

/**
 * LCD-006 Ch.7 — "Reorder Cards." Reorder acts on the whole sibling set at
 * once (see [CardRepository.reorder]), so unlike the single-value
 * Commands above, undo here restores the *entire previous ordering* in
 * one step rather than reversing one Card's position in isolation —
 * that's the only way to undo a reorder without leaving sibling Cards in
 * an inconsistent order relative to each other.
 */
class ReorderCardsCommand(
    private val cardRepository: CardRepository,
    private val previousOrderedCardIds: List<String>,
    private val newOrderedCardIds: List<String>
) : Command {
    override val label: String = "Reorder Cards"

    override fun execute() {
        cardRepository.reorder(newOrderedCardIds)
    }

    override fun undo() {
        cardRepository.reorder(previousOrderedCardIds)
    }
}
