package com.lorecanvas.storage.serialization

import com.lorecanvas.common.Json
import com.lorecanvas.common.JsonParseException
import com.lorecanvas.common.JsonValue
import com.lorecanvas.common.jsonArrayOf
import com.lorecanvas.common.jsonObjectOf
import com.lorecanvas.domain.Card
import com.lorecanvas.domain.Node
import com.lorecanvas.domain.Relationship
import com.lorecanvas.domain.Timeline

/**
 * Bumped whenever the bundle's shape changes in a way older readers
 * couldn't handle. [ImportExportRepository] checks this before importing
 * (LCD-009 Ch.15 — "Version compatibility"); a real migration step
 * between versions is future work once a second version actually exists
 * to migrate *from* — there's nothing to migrate yet with only one
 * format version ever having shipped.
 */
const val CURRENT_EXPORT_FORMAT_VERSION = "1.0"

/**
 * A self-contained export of a project (or a selection within it) — LCD-009
 * Ch.15: "Exported data should be independent of the originating project."
 * One JSON file, reusing each entity's existing dedicated serializer
 * rather than inventing a new format.
 */
data class ExportBundle(
    val nodes: List<Node>,
    val cards: List<Card>,
    val relationships: List<Relationship>,
    val timelines: List<Timeline>,
    val formatVersion: String = CURRENT_EXPORT_FORMAT_VERSION
)

object ExportBundleSerializer {

    fun toJson(bundle: ExportBundle): String {
        val obj = jsonObjectOf(
            "exportFormatVersion" to JsonValue.JsonString(bundle.formatVersion),
            "nodes" to jsonArrayOf(bundle.nodes.map { Json.parse(NodeSerializer.toJson(it)) }),
            "cards" to jsonArrayOf(bundle.cards.map { Json.parse(CardSerializer.toJson(it)) }),
            "relationships" to jsonArrayOf(bundle.relationships.map { Json.parse(RelationshipSerializer.toJson(it)) }),
            "timelines" to jsonArrayOf(bundle.timelines.map { Json.parse(TimelineSerializer.toJson(it)) })
        )
        return Json.stringify(obj, pretty = true)
    }

    sealed class DeserializeResult {
        data class Success(val bundle: ExportBundle) : DeserializeResult()
        data class Malformed(val reason: String) : DeserializeResult()
    }

    fun fromJson(text: String): DeserializeResult {
        val parsed = try {
            Json.parse(text)
        } catch (e: JsonParseException) {
            return DeserializeResult.Malformed("Invalid JSON: ${e.message}")
        }
        val obj = parsed as? JsonValue.JsonObject
            ?: return DeserializeResult.Malformed("Root of an export bundle must be an object.")

        val nodesArray = obj["nodes"] as? JsonValue.JsonArray ?: return DeserializeResult.Malformed("Missing 'nodes' array.")
        val cardsArray = obj["cards"] as? JsonValue.JsonArray ?: return DeserializeResult.Malformed("Missing 'cards' array.")
        val relsArray = obj["relationships"] as? JsonValue.JsonArray ?: return DeserializeResult.Malformed("Missing 'relationships' array.")
        val timelinesArray = obj["timelines"] as? JsonValue.JsonArray ?: return DeserializeResult.Malformed("Missing 'timelines' array.")

        val nodes = mutableListOf<Node>()
        for (item in nodesArray.items) {
            when (val r = NodeSerializer.fromJson(Json.stringify(item))) {
                is NodeSerializer.DeserializeResult.Success -> nodes.add(r.node)
                is NodeSerializer.DeserializeResult.Malformed -> return DeserializeResult.Malformed("Bad node in bundle: ${r.reason}")
            }
        }
        val cards = mutableListOf<Card>()
        for (item in cardsArray.items) {
            when (val r = CardSerializer.fromJson(Json.stringify(item))) {
                is CardSerializer.DeserializeResult.Success -> cards.add(r.card)
                is CardSerializer.DeserializeResult.Malformed -> return DeserializeResult.Malformed("Bad card in bundle: ${r.reason}")
            }
        }
        val relationships = mutableListOf<Relationship>()
        for (item in relsArray.items) {
            when (val r = RelationshipSerializer.fromJson(Json.stringify(item))) {
                is RelationshipSerializer.DeserializeResult.Success -> relationships.add(r.relationship)
                is RelationshipSerializer.DeserializeResult.Malformed -> return DeserializeResult.Malformed("Bad relationship in bundle: ${r.reason}")
            }
        }
        val timelines = mutableListOf<Timeline>()
        for (item in timelinesArray.items) {
            when (val r = TimelineSerializer.fromJson(Json.stringify(item))) {
                is TimelineSerializer.DeserializeResult.Success -> timelines.add(r.timeline)
                is TimelineSerializer.DeserializeResult.Malformed -> return DeserializeResult.Malformed("Bad timeline in bundle: ${r.reason}")
            }
        }

        val formatVersion = (obj["exportFormatVersion"] as? JsonValue.JsonString)?.value ?: "unknown"
        return DeserializeResult.Success(ExportBundle(nodes, cards, relationships, timelines, formatVersion))
    }
}
