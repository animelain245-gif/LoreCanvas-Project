package com.lorecanvas.storage.serialization

import com.lorecanvas.common.Json
import com.lorecanvas.common.JsonParseException
import com.lorecanvas.common.JsonValue
import com.lorecanvas.common.jsonArrayOf
import com.lorecanvas.common.jsonObjectOf
import com.lorecanvas.common.optionalString
import com.lorecanvas.common.requireString
import com.lorecanvas.common.stringList
import com.lorecanvas.common.toJson
import com.lorecanvas.domain.Card

object CardSerializer {

    fun toJson(card: Card): String {
        val obj = jsonObjectOf(
            "id" to card.id.toJson(),
            "parentNodeId" to card.parentNodeId.toJson(),
            "title" to card.title.toJson(),
            "type" to card.type.toJson(),
            "content" to card.content.toJson(),
            "order" to JsonValue.JsonNumber(card.order.toDouble()),
            "tags" to jsonArrayOf(card.tags.map { it.toJson() }),
            "createdAt" to card.createdAt.toJson(),
            "modifiedAt" to card.modifiedAt.toJson()
        )
        return Json.stringify(obj, pretty = true)
    }

    sealed class DeserializeResult {
        data class Success(val card: Card) : DeserializeResult()
        data class Malformed(val reason: String) : DeserializeResult()
    }

    fun fromJson(text: String): DeserializeResult {
        val parsed = try {
            Json.parse(text)
        } catch (e: JsonParseException) {
            return DeserializeResult.Malformed("Invalid JSON: ${e.message}")
        }
        val obj = parsed as? JsonValue.JsonObject
            ?: return DeserializeResult.Malformed("Root of a card file must be an object.")

        return try {
            val order = (obj["order"] as? JsonValue.JsonNumber)?.value?.toInt() ?: 0
            val card = Card.restore(
                id = obj.requireString("id"),
                createdAt = obj.requireString("createdAt"),
                modifiedAt = obj.requireString("modifiedAt"),
                parentNodeId = obj.requireString("parentNodeId"),
                title = obj.requireString("title"),
                type = obj.requireString("type"),
                content = obj.optionalString("content"),
                order = order,
                tags = obj.stringList("tags")
            )
            DeserializeResult.Success(card)
        } catch (e: JsonParseException) {
            DeserializeResult.Malformed(e.message ?: "Missing required field.")
        } catch (e: IllegalArgumentException) {
            DeserializeResult.Malformed(e.message ?: "Invalid card data.")
        }
    }
}
