package com.lorecanvas.storage.serialization

import com.lorecanvas.common.Json
import com.lorecanvas.common.JsonParseException
import com.lorecanvas.common.JsonValue
import com.lorecanvas.common.jsonArrayOf
import com.lorecanvas.common.jsonObjectOf
import com.lorecanvas.common.optionalString
import com.lorecanvas.common.requireString
import com.lorecanvas.common.toJson
import com.lorecanvas.domain.DefaultCardSpec
import com.lorecanvas.domain.Template

object TemplateSerializer {

    fun toJson(template: Template): String {
        val obj = jsonObjectOf(
            "id" to template.id.toJson(),
            "name" to template.name.toJson(),
            "targetNodeType" to template.targetNodeType.toJson(),
            "category" to template.category.toJson(),
            "defaultCards" to jsonArrayOf(template.defaultCards.map { cardSpecToJson(it) }),
            "defaultMetadata" to metadataToJson(template.defaultMetadata),
            "createdAt" to template.createdAt.toJson(),
            "modifiedAt" to template.modifiedAt.toJson()
        )
        return Json.stringify(obj, pretty = true)
    }

    private fun cardSpecToJson(spec: DefaultCardSpec): JsonValue.JsonObject = jsonObjectOf(
        "title" to spec.title.toJson(),
        "type" to spec.type.toJson(),
        "content" to spec.content.toJson()
    )

    private fun metadataToJson(metadata: Map<String, Any?>): JsonValue.JsonObject {
        val fields = LinkedHashMap<String, JsonValue>()
        for ((k, v) in metadata) fields[k] = if (v is String) JsonValue.JsonString(v) else JsonValue.JsonNull
        return JsonValue.JsonObject(fields)
    }

    sealed class DeserializeResult {
        data class Success(val template: Template) : DeserializeResult()
        data class Malformed(val reason: String) : DeserializeResult()
    }

    fun fromJson(text: String): DeserializeResult {
        val parsed = try {
            Json.parse(text)
        } catch (e: JsonParseException) {
            return DeserializeResult.Malformed("Invalid JSON: ${e.message}")
        }
        val obj = parsed as? JsonValue.JsonObject
            ?: return DeserializeResult.Malformed("Root of a template file must be an object.")

        return try {
            val cards = (obj["defaultCards"] as? JsonValue.JsonArray)?.items?.map { item ->
                val cardObj = item as? JsonValue.JsonObject
                    ?: return DeserializeResult.Malformed("Each default card must be an object.")
                DefaultCardSpec(
                    title = cardObj.requireString("title"),
                    type = cardObj.requireString("type"),
                    content = cardObj.optionalString("content")
                )
            } ?: emptyList()

            val metadataObj = obj["defaultMetadata"] as? JsonValue.JsonObject
            val metadata: Map<String, Any?> = metadataObj?.fields?.mapValues { (_, v) -> (v as? JsonValue.JsonString)?.value } ?: emptyMap()

            val template = Template.restore(
                id = obj.requireString("id"),
                createdAt = obj.requireString("createdAt"),
                modifiedAt = obj.requireString("modifiedAt"),
                name = obj.requireString("name"),
                targetNodeType = obj.requireString("targetNodeType"),
                category = obj.optionalString("category", default = com.lorecanvas.domain.TemplateCategories.DEFAULT),
                defaultCards = cards,
                defaultMetadata = metadata
            )
            DeserializeResult.Success(template)
        } catch (e: JsonParseException) {
            DeserializeResult.Malformed(e.message ?: "Missing required field.")
        } catch (e: IllegalArgumentException) {
            DeserializeResult.Malformed(e.message ?: "Invalid template data.")
        }
    }
}
