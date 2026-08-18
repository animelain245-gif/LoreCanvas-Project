package com.lorecanvas.storage.serialization

import com.lorecanvas.common.Json
import com.lorecanvas.common.JsonParseException
import com.lorecanvas.common.JsonValue
import com.lorecanvas.common.jsonArrayOf
import com.lorecanvas.common.jsonObjectOf
import com.lorecanvas.common.optionalString
import com.lorecanvas.common.requireString
import com.lorecanvas.common.toJson
import com.lorecanvas.domain.Relationship
import com.lorecanvas.domain.RelationshipContext
import com.lorecanvas.domain.RelationshipDirection

object RelationshipSerializer {

    fun toJson(relationship: Relationship): String {
        val obj = jsonObjectOf(
            "id" to relationship.id.toJson(),
            "sourceNodeId" to relationship.sourceNodeId.toJson(),
            "targetNodeId" to relationship.targetNodeId.toJson(),
            "type" to relationship.type.toJson(),
            "direction" to relationship.direction.name.toJson(),
            "description" to relationship.description.toJson(),
            "status" to relationship.status.toJson(),
            "contexts" to jsonArrayOf(relationship.contexts.map { contextToJson(it) }),
            "createdAt" to relationship.createdAt.toJson(),
            "modifiedAt" to relationship.modifiedAt.toJson()
        )
        return Json.stringify(obj, pretty = true)
    }

    private fun contextToJson(context: RelationshipContext): JsonValue.JsonObject = jsonObjectOf(
        "id" to context.id.toJson(),
        "startDate" to context.startDate.toJson(),
        "endDate" to (context.endDate?.toJson() ?: JsonValue.JsonNull),
        "description" to context.description.toJson(),
        "timelineEventIds" to jsonArrayOf(context.timelineEventIds.map { it.toJson() })
    )

    sealed class DeserializeResult {
        data class Success(val relationship: Relationship) : DeserializeResult()
        data class Malformed(val reason: String) : DeserializeResult()
    }

    fun fromJson(text: String): DeserializeResult {
        val parsed = try {
            Json.parse(text)
        } catch (e: JsonParseException) {
            return DeserializeResult.Malformed("Invalid JSON: ${e.message}")
        }
        val obj = parsed as? JsonValue.JsonObject
            ?: return DeserializeResult.Malformed("Root of a relationship file must be an object.")

        return try {
            val directionRaw = obj.optionalString("direction", default = RelationshipDirection.DIRECTED.name)
            val direction = try {
                RelationshipDirection.valueOf(directionRaw)
            } catch (e: IllegalArgumentException) {
                return DeserializeResult.Malformed("Unknown relationship direction: '$directionRaw'")
            }

            val contexts = (obj["contexts"] as? JsonValue.JsonArray)?.items?.map { item ->
                val contextObj = item as? JsonValue.JsonObject
                    ?: return DeserializeResult.Malformed("Each relationship context must be an object.")
                RelationshipContext(
                    id = contextObj.requireString("id"),
                    startDate = contextObj.requireString("startDate"),
                    endDate = (contextObj["endDate"] as? JsonValue.JsonString)?.value,
                    description = contextObj.optionalString("description"),
                    timelineEventIds = (contextObj["timelineEventIds"] as? JsonValue.JsonArray)
                        ?.items?.mapNotNull { (it as? JsonValue.JsonString)?.value } ?: emptyList()
                )
            } ?: emptyList()

            val relationship = Relationship.restore(
                id = obj.requireString("id"),
                createdAt = obj.requireString("createdAt"),
                modifiedAt = obj.requireString("modifiedAt"),
                sourceNodeId = obj.requireString("sourceNodeId"),
                targetNodeId = obj.requireString("targetNodeId"),
                type = obj.requireString("type"),
                direction = direction,
                description = obj.optionalString("description"),
                status = obj.optionalString("status", default = "Active"),
                contexts = contexts
            )
            DeserializeResult.Success(relationship)
        } catch (e: JsonParseException) {
            DeserializeResult.Malformed(e.message ?: "Missing required field.")
        } catch (e: IllegalArgumentException) {
            DeserializeResult.Malformed(e.message ?: "Invalid relationship data.")
        }
    }
}
