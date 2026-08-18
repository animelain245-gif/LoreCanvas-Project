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
import com.lorecanvas.domain.Timeline
import com.lorecanvas.domain.TimelineEvent

object TimelineSerializer {

    fun toJson(timeline: Timeline): String {
        val obj = jsonObjectOf(
            "id" to timeline.id.toJson(),
            "name" to timeline.name.toJson(),
            "description" to timeline.description.toJson(),
            "events" to jsonArrayOf(timeline.events.map { eventToJson(it) }),
            "settings" to settingsToJson(timeline.settings),
            "createdAt" to timeline.createdAt.toJson(),
            "modifiedAt" to timeline.modifiedAt.toJson()
        )
        return Json.stringify(obj, pretty = true)
    }

    private fun eventToJson(event: TimelineEvent): JsonValue.JsonObject = jsonObjectOf(
        "id" to event.id.toJson(),
        "date" to event.date.toJson(),
        "title" to event.title.toJson(),
        "description" to event.description.toJson(),
        "relatedNodeIds" to jsonArrayOf(event.relatedNodeIds.map { it.toJson() }),
        "tags" to jsonArrayOf(event.tags.map { it.toJson() })
    )

    private fun settingsToJson(settings: Map<String, Any?>): JsonValue.JsonObject {
        val fields = LinkedHashMap<String, JsonValue>()
        for ((k, v) in settings) fields[k] = if (v is String) JsonValue.JsonString(v) else JsonValue.JsonNull
        return JsonValue.JsonObject(fields)
    }

    sealed class DeserializeResult {
        data class Success(val timeline: Timeline) : DeserializeResult()
        data class Malformed(val reason: String) : DeserializeResult()
    }

    fun fromJson(text: String): DeserializeResult {
        val parsed = try {
            Json.parse(text)
        } catch (e: JsonParseException) {
            return DeserializeResult.Malformed("Invalid JSON: ${e.message}")
        }
        val obj = parsed as? JsonValue.JsonObject
            ?: return DeserializeResult.Malformed("Root of a timeline file must be an object.")

        return try {
            val events = (obj["events"] as? JsonValue.JsonArray)?.items?.map { item ->
                val eventObj = item as? JsonValue.JsonObject
                    ?: return DeserializeResult.Malformed("Each timeline event must be an object.")
                TimelineEvent(
                    id = eventObj.requireString("id"),
                    date = eventObj.requireString("date"),
                    title = eventObj.requireString("title"),
                    description = eventObj.optionalString("description"),
                    relatedNodeIds = eventObj.stringList("relatedNodeIds"),
                    tags = eventObj.stringList("tags")
                )
            } ?: emptyList()

            val settingsObj = obj["settings"] as? JsonValue.JsonObject
            val settings: Map<String, Any?> = settingsObj?.fields?.mapValues { (_, v) ->
                (v as? JsonValue.JsonString)?.value
            } ?: emptyMap()

            val timeline = Timeline.restore(
                id = obj.requireString("id"),
                createdAt = obj.requireString("createdAt"),
                modifiedAt = obj.requireString("modifiedAt"),
                name = obj.requireString("name"),
                description = obj.optionalString("description"),
                events = events,
                settings = settings
            )
            DeserializeResult.Success(timeline)
        } catch (e: JsonParseException) {
            DeserializeResult.Malformed(e.message ?: "Missing required field.")
        } catch (e: IllegalArgumentException) {
            DeserializeResult.Malformed(e.message ?: "Invalid timeline data.")
        }
    }
}
