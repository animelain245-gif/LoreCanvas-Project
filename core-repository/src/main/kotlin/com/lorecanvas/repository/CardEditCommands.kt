package com.lorecanvas.repository

import com.lorecanvas.commands.Command
import com.lorecanvas.commands.CompoundCommand
import com.lorecanvas.domain.Card

/** Card's counterpart to [NodeEditCommands] — see its class doc for the full rationale. */
object CardEditCommands {

    data class Snapshot(val title: String, val type: String, val content: String, val tags: List<String>) {
        companion object {
            fun of(card: Card): Snapshot = Snapshot(card.title, card.type, card.content, card.tags)
        }
    }

    fun buildSaveCommand(cardRepository: CardRepository, card: Card, snapshot: Snapshot): Command? {
        val commands = mutableListOf<Command>()

        if (card.title != snapshot.title) {
            val newTitle = card.title
            card.rename(snapshot.title)
            commands.add(RenameCardCommand(cardRepository, card, newTitle))
        }
        if (card.type != snapshot.type) {
            val newType = card.type
            card.changeType(snapshot.type)
            commands.add(ChangeCardTypeCommand(cardRepository, card, newType))
        }
        if (card.content != snapshot.content) {
            val newContent = card.content
            card.updateContent(snapshot.content)
            commands.add(UpdateCardContentCommand(cardRepository, card, newContent))
        }
        val addedTags = card.tags.filter { it !in snapshot.tags }
        for (tag in addedTags) {
            card.removeTag(tag)
            commands.add(AddCardTagCommand(cardRepository, card, tag))
        }

        return when (commands.size) {
            0 -> null
            1 -> commands[0]
            else -> CompoundCommand("Edit Card", commands)
        }
    }
}
