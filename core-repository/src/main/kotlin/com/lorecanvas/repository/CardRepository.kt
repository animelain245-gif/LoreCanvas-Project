package com.lorecanvas.repository

import com.lorecanvas.common.LcResult
import com.lorecanvas.common.Logger
import com.lorecanvas.common.createLogger
import com.lorecanvas.domain.Card
import com.lorecanvas.events.EventBus
import com.lorecanvas.storage.CardStorage
import com.lorecanvas.storage.NodeStorage
import com.lorecanvas.validation.CardValidator
import com.lorecanvas.validation.ValidationResult
import java.io.File

/**
 * CardRepository — Phase 4 "Card System" (PEP-001), implementing LCD-006
 * Ch.7 (Create/Update/Delete/Reorder/Duplicate Card) and LCD-009 Ch.9-10.
 * Scoped to an open project directory, same as [NodeRepository]. Takes a
 * [NodeStorage] dependency (read-only use) purely to enforce LCD-006's
 * rule: "A Card cannot exist without a valid parent Node."
 */
class CardRepository(
    private val projectDirectory: File,
    private val cardStorage: CardStorage,
    private val nodeStorage: NodeStorage,
    private val eventBus: EventBus,
    private val logger: Logger = createLogger("CardRepository")
) {

    fun create(parentNodeId: String, title: String, type: String, content: String = ""): LcResult<Card, RepositoryError> {
        val validation = CardValidator.validateForCreate(title, type)
        if (validation is ValidationResult.Invalid) {
            return LcResult.fail(RepositoryError.ValidationFailed(validation.errors.joinToString { it.message }))
        }
        if (!nodeStorage.nodeExists(projectDirectory, parentNodeId)) {
            return LcResult.fail(RepositoryError.ValidationFailed("Cannot create a card for a node that doesn't exist."))
        }

        val existingCount = cardStorage.listCardsForNode(projectDirectory, parentNodeId).size
        val card = Card.create(parentNodeId = parentNodeId, title = title, type = type, content = content, order = existingCount)

        return when (val result = cardStorage.createCard(projectDirectory, card)) {
            is LcResult.Fail -> LcResult.fail(RepositoryError.Storage(result.error))
            is LcResult.Ok -> {
                logger.info("Card created", card.id)
                eventBus.publish(CardEvent.CardCreated(card.id))
                LcResult.ok(card)
            }
        }
    }

    fun save(card: Card): LcResult<Unit, RepositoryError> {
        val validation = CardValidator.validateForSave(card)
        if (validation is ValidationResult.Invalid) {
            return LcResult.fail(RepositoryError.ValidationFailed(validation.errors.joinToString { it.message }))
        }
        return when (val result = cardStorage.saveCard(projectDirectory, card)) {
            is LcResult.Fail -> LcResult.fail(RepositoryError.Storage(result.error))
            is LcResult.Ok -> {
                eventBus.publish(CardEvent.CardUpdated(card.id))
                LcResult.ok(Unit)
            }
        }
    }

    fun delete(cardId: String): LcResult<Unit, RepositoryError> =
        when (val result = cardStorage.deleteCard(projectDirectory, cardId)) {
            is LcResult.Fail -> LcResult.fail(RepositoryError.Storage(result.error))
            is LcResult.Ok -> {
                eventBus.publish(CardEvent.CardDeleted(cardId))
                LcResult.ok(Unit)
            }
        }

    /** LCD-006 Ch.7 — Duplicate Card: new identity, same content, original untouched. */
    fun duplicate(cardId: String): LcResult<Card, RepositoryError> {
        val source = when (val result = cardStorage.loadCard(projectDirectory, cardId)) {
            is LcResult.Ok -> result.value
            is LcResult.Fail -> return LcResult.fail(RepositoryError.Storage(result.error))
        }
        val newOrder = cardStorage.listCardsForNode(projectDirectory, source.parentNodeId).size
        val copy = Card.duplicateOf(source, newOrder)
        return when (val result = cardStorage.createCard(projectDirectory, copy)) {
            is LcResult.Fail -> LcResult.fail(RepositoryError.Storage(result.error))
            is LcResult.Ok -> {
                eventBus.publish(CardEvent.CardCreated(copy.id))
                LcResult.ok(copy)
            }
        }
    }

    /** LCD-006 Ch.7 — Reorder Cards: reassigns `order` to match the given sequence. */
    fun reorder(orderedCardIds: List<String>): LcResult<Unit, RepositoryError> {
        orderedCardIds.forEachIndexed { index, cardId ->
            val card = when (val result = cardStorage.loadCard(projectDirectory, cardId)) {
                is LcResult.Ok -> result.value
                is LcResult.Fail -> return LcResult.fail(RepositoryError.Storage(result.error))
            }
            card.moveTo(index)
            val saveResult = cardStorage.saveCard(projectDirectory, card)
            if (saveResult is LcResult.Fail) return LcResult.fail(RepositoryError.Storage(saveResult.error))
        }
        return LcResult.ok(Unit)
    }

    /**
     * Restore (redo-of-create support for [CreateCardCommand]) — see
     * [NodeRepository.restore]'s doc comment for the full rationale.
     * Re-inserts an already-constructed [Card] at its existing id, rather
     * than minting a fresh one ([create]) or requiring the id already
     * exist ([save]).
     */
    fun restore(card: Card): LcResult<Unit, RepositoryError> =
        when (val result = cardStorage.createCard(projectDirectory, card)) {
            is LcResult.Fail -> LcResult.fail(RepositoryError.Storage(result.error))
            is LcResult.Ok -> {
                logger.info("Card restored", card.id)
                eventBus.publish(CardEvent.CardCreated(card.id))
                LcResult.ok(Unit)
            }
        }

    fun listForNode(nodeId: String): List<Card> = cardStorage.listCardsForNode(projectDirectory, nodeId)

    /**
     * All Cards in the project in one pass. Added because Search was
     * calling [listForNode] once per Node — each call itself scans every
     * Card in the project, so that pattern was O(nodes × cards): exactly
     * what the performance requirements (20,000+ Cards) warn against.
     * Callers that need "every card in the project" should call this once,
     * not loop [listForNode] across all Nodes.
     */
    fun listAll(): List<Card> = cardStorage.listCards(projectDirectory)

    fun get(cardId: String): Card? = (cardStorage.loadCard(projectDirectory, cardId) as? LcResult.Ok)?.value
}
