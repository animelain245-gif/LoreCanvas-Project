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
import com.lorecanvas.domain.Node
import com.lorecanvas.domain.NodeStatus

/**
 * Node Serializer (LCD-007, Chapter 5) — mirrors [ProjectSerializer]'s
 * shape exactly, since LCD-007 asks for "a dedicated serializer" per
 * entity type rather than one generic one. Same deterministic-output,
 * no-business-validation rules apply (Chapter 10).
 */
object NodeSerializer {

    fun toJson(node: Node): String {
        val obj = jsonObjectOf(
            "id" to node.id.toJson(),
            "name" to node.name.toJson(),
            "type" to node.type.toJson(),
            "summary" to node.summary.toJson(),
            "status" to node.status.name.toJson(),
            "tags" to jsonArrayOf(node.tags.map { it.toJson() }),
            "createdAt" to node.createdAt.toJson(),
            "modifiedAt" to node.modifiedAt.toJson()
        )
        return Json.stringify(obj, pretty = true)
    }

    sealed class DeserializeResult {
        data class Success(val node: Node) : DeserializeResult()
        data class Malformed(val reason: String) : DeserializeResult()
    }

    fun fromJson(text: String): DeserializeResult {
        val parsed = try {
            Json.parse(text)
        } catch (e: JsonParseException) {
            return DeserializeResult.Malformed("Invalid JSON: ${e.message}")
        }

        val obj = parsed as? JsonValue.JsonObject
            ?: return DeserializeResult.Malformed("Root of a node file must be an object.")

        return try {
            val statusRaw = obj.optionalString("status", default = NodeStatus.ACTIVE.name)
            val status = try {
                NodeStatus.valueOf(statusRaw)
            } catch (e: IllegalArgumentException) {
                return DeserializeResult.Malformed("Unknown node status: '$statusRaw'")
            }

            val node = Node.restore(
                id = obj.requireString("id"),
                createdAt = obj.requireString("createdAt"),
                modifiedAt = obj.requireString("modifiedAt"),
                name = obj.requireString("name"),
                type = obj.requireString("type"),
                summary = obj.optionalString("summary"),
                status = status,
                tags = obj.stringList("tags")
            )
            DeserializeResult.Success(node)
        } catch (e: JsonParseException) {
            DeserializeResult.Malformed(e.message ?: "Missing required field.")
        } catch (e: IllegalArgumentException) {
            DeserializeResult.Malformed(e.message ?: "Invalid node data.")
        }
    }
}
