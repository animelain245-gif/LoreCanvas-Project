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
import com.lorecanvas.domain.Project

/**
 * Project Serializer (LCD-007, Chapter 5): "Each entity type has a
 * dedicated serializer... Serializers should produce deterministic output.
 * Saving the same project twice without modifications should generate
 * identical data."
 *
 * Deliberately knows nothing about files or storage locations (that's
 * [com.lorecanvas.storage.ProjectFileStorage]'s job) and performs no
 * business validation (LCD-007, Chapter 10 — "The Storage Engine must
 * never validate business logic"). It only converts a [Project] to and
 * from its JSON representation.
 */
object ProjectSerializer {

    private const val CURRENT_FORMAT_VERSION = "1.0"

    /** Matches LCD-007 Chapter 4's "Root File" contents plus a format version tag. */
    fun toJson(project: Project): String {
        val obj = jsonObjectOf(
            "formatVersion" to CURRENT_FORMAT_VERSION.toJson(),
            "id" to project.id.toJson(),
            "name" to project.name.toJson(),
            "description" to project.description.toJson(),
            "version" to project.version.toJson(),
            "author" to project.author.toJson(),
            "tags" to jsonArrayOf(project.tags.map { it.toJson() }),
            "settings" to settingsToJson(project.settings),
            "createdAt" to project.createdAt.toJson(),
            "modifiedAt" to project.modifiedAt.toJson()
        )
        return Json.stringify(obj, pretty = true)
    }

    sealed class DeserializeResult {
        data class Success(val project: Project, val formatVersion: String) : DeserializeResult()
        data class Malformed(val reason: String) : DeserializeResult()
    }

    fun fromJson(text: String): DeserializeResult {
        val parsed = try {
            Json.parse(text)
        } catch (e: JsonParseException) {
            return DeserializeResult.Malformed("Invalid JSON: ${e.message}")
        }

        val obj = parsed as? JsonValue.JsonObject
            ?: return DeserializeResult.Malformed("Root of project.json must be an object.")

        return try {
            val formatVersion = obj.optionalString("formatVersion", default = "unknown")
            val project = Project.restore(
                id = obj.requireString("id"),
                createdAt = obj.requireString("createdAt"),
                modifiedAt = obj.requireString("modifiedAt"),
                name = obj.requireString("name"),
                description = obj.optionalString("description"),
                version = obj.optionalString("version", default = "1.0"),
                author = obj.optionalString("author"),
                tags = obj.stringList("tags"),
                settings = jsonToSettings(obj["settings"])
            )
            DeserializeResult.Success(project, formatVersion)
        } catch (e: JsonParseException) {
            DeserializeResult.Malformed(e.message ?: "Missing required field.")
        } catch (e: IllegalArgumentException) {
            DeserializeResult.Malformed(e.message ?: "Invalid project data.")
        }
    }

    // Settings is a Map<String, Any?> in the domain layer (LCD-005, Chapter
    // 4 — "reserved for future workspace preferences"). Only the handful of
    // JSON-representable scalar types are supported today; this is
    // sufficient until a real settings schema exists.
    private fun settingsToJson(settings: Map<String, Any?>): JsonValue.JsonObject {
        val fields = LinkedHashMap<String, JsonValue>()
        for ((key, value) in settings) {
            fields[key] = anyToJson(value)
        }
        return JsonValue.JsonObject(fields)
    }

    private fun anyToJson(value: Any?): JsonValue = when (value) {
        null -> JsonValue.JsonNull
        is String -> JsonValue.JsonString(value)
        is Boolean -> JsonValue.JsonBool(value)
        is Number -> JsonValue.JsonNumber(value.toDouble())
        is List<*> -> JsonValue.JsonArray(value.map { anyToJson(it) })
        is Map<*, *> -> {
            val fields = LinkedHashMap<String, JsonValue>()
            for ((k, v) in value) fields[k.toString()] = anyToJson(v)
            JsonValue.JsonObject(fields)
        }
        else -> JsonValue.JsonString(value.toString())
    }

    private fun jsonToSettings(value: JsonValue?): Map<String, Any?> {
        val obj = value as? JsonValue.JsonObject ?: return emptyMap()
        return obj.fields.mapValues { (_, v) -> jsonToAny(v) }
    }

    private fun jsonToAny(value: JsonValue): Any? = when (value) {
        is JsonValue.JsonNull -> null
        is JsonValue.JsonString -> value.value
        is JsonValue.JsonBool -> value.value
        is JsonValue.JsonNumber -> value.value
        is JsonValue.JsonArray -> value.items.map { jsonToAny(it) }
        is JsonValue.JsonObject -> value.fields.mapValues { (_, v) -> jsonToAny(v) }
    }
}
