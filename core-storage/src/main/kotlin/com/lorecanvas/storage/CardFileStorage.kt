package com.lorecanvas.storage

import com.lorecanvas.common.LcResult
import com.lorecanvas.common.Logger
import com.lorecanvas.common.createLogger
import com.lorecanvas.domain.Card
import com.lorecanvas.storage.serialization.CardSerializer
import java.io.File

class CardFileStorage(
    private val fileManager: FileManager = RealFileManager(),
    private val logger: Logger = createLogger("CardFileStorage")
) : CardStorage {

    companion object {
        const val CARDS_SUBDIRECTORY = "cards"
    }

    private fun cardsDirectory(projectDirectory: File) = File(projectDirectory, CARDS_SUBDIRECTORY)
    private fun cardFile(projectDirectory: File, cardId: String) = File(cardsDirectory(projectDirectory), "$cardId.json")

    override fun createCard(projectDirectory: File, card: Card): LcResult<Unit, StorageError> {
        if (cardExists(projectDirectory, card.id)) {
            return LcResult.fail(StorageError(StorageErrorType.ALREADY_EXISTS, "A card with id ${card.id} already exists."))
        }
        return writeCard(projectDirectory, card)
    }

    override fun saveCard(projectDirectory: File, card: Card): LcResult<Unit, StorageError> {
        if (!cardExists(projectDirectory, card.id)) {
            return LcResult.fail(StorageError(StorageErrorType.NOT_FOUND, "No card with id ${card.id}."))
        }
        return writeCard(projectDirectory, card)
    }

    private fun writeCard(projectDirectory: File, card: Card): LcResult<Unit, StorageError> = try {
        fileManager.createDirectories(cardsDirectory(projectDirectory))
        fileManager.writeTextAtomic(cardFile(projectDirectory, card.id), CardSerializer.toJson(card))
        logger.info("Card written", card.id)
        LcResult.ok(Unit)
    } catch (e: Exception) {
        logger.error("Failed to write card", e.message)
        LcResult.fail(StorageError(StorageErrorType.IO_ERROR, e.message ?: "Failed to write card."))
    }

    override fun loadCard(projectDirectory: File, cardId: String): LcResult<Card, StorageError> {
        val file = cardFile(projectDirectory, cardId)
        if (!fileManager.exists(file)) {
            return LcResult.fail(StorageError(StorageErrorType.NOT_FOUND, "No card with id $cardId."))
        }
        val raw = try {
            fileManager.readText(file)
        } catch (e: Exception) {
            return LcResult.fail(StorageError(StorageErrorType.IO_ERROR, e.message ?: "Failed to read card $cardId"))
        }
        return when (val result = CardSerializer.fromJson(raw)) {
            is CardSerializer.DeserializeResult.Success -> LcResult.ok(result.card)
            is CardSerializer.DeserializeResult.Malformed -> {
                logger.error("Corrupt card file", "$cardId: ${result.reason}")
                LcResult.fail(StorageError(StorageErrorType.CORRUPT_DATA, result.reason))
            }
        }
    }

    override fun deleteCard(projectDirectory: File, cardId: String): LcResult<Unit, StorageError> {
        val file = cardFile(projectDirectory, cardId)
        if (!fileManager.exists(file)) {
            return LcResult.fail(StorageError(StorageErrorType.NOT_FOUND, "No card with id $cardId."))
        }
        return try {
            fileManager.deleteRecursively(file)
            logger.info("Card deleted", cardId)
            LcResult.ok(Unit)
        } catch (e: Exception) {
            LcResult.fail(StorageError(StorageErrorType.IO_ERROR, e.message ?: "Failed to delete card."))
        }
    }

    override fun cardExists(projectDirectory: File, cardId: String): Boolean =
        fileManager.exists(cardFile(projectDirectory, cardId))

    override fun deleteCardsForNode(projectDirectory: File, nodeId: String): LcResult<Unit, StorageError> {
        val cards = listCardsForNode(projectDirectory, nodeId)
        for (card in cards) {
            val result = deleteCard(projectDirectory, card.id)
            if (result is LcResult.Fail) return result
        }
        return LcResult.ok(Unit)
    }

    override fun listCards(projectDirectory: File): List<Card> {
        val dir = cardsDirectory(projectDirectory)
        if (!fileManager.exists(dir)) return emptyList()
        return fileManager.listFiles(dir, ".json").mapNotNull { file ->
            val raw = try {
                fileManager.readText(file)
            } catch (e: Exception) {
                logger.warn("Skipping unreadable card file", file.name)
                return@mapNotNull null
            }
            when (val result = CardSerializer.fromJson(raw)) {
                is CardSerializer.DeserializeResult.Success -> result.card
                is CardSerializer.DeserializeResult.Malformed -> {
                    logger.warn("Skipping corrupt card file", "${file.name}: ${result.reason}")
                    null
                }
            }
        }.sortedBy { it.order }
    }
}
