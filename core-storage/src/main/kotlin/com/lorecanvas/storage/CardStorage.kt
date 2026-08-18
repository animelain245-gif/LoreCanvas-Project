package com.lorecanvas.storage

import com.lorecanvas.common.LcResult
import com.lorecanvas.domain.Card
import java.io.File

interface CardStorage {
    fun createCard(projectDirectory: File, card: Card): LcResult<Unit, StorageError>
    fun saveCard(projectDirectory: File, card: Card): LcResult<Unit, StorageError>
    fun loadCard(projectDirectory: File, cardId: String): LcResult<Card, StorageError>
    fun deleteCard(projectDirectory: File, cardId: String): LcResult<Unit, StorageError>
    fun cardExists(projectDirectory: File, cardId: String): Boolean
    fun listCards(projectDirectory: File): List<Card>
    fun listCardsForNode(projectDirectory: File, nodeId: String): List<Card> =
        listCards(projectDirectory).filter { it.parentNodeId == nodeId }
}
