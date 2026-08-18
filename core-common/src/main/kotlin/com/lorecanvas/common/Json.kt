package com.lorecanvas.common

/**
 * A tiny, dependency-free JSON implementation.
 *
 * Why hand-written instead of kotlinx.serialization: at this stage there is
 * exactly one serializable entity (Project), the shape is small and stable,
 * and a hand-written codec keeps this module (and everything that depends
 * on it) buildable with nothing but the Kotlin stdlib — no compiler plugin,
 * no Maven dependency resolution required just to typecheck. LCD-007,
 * Chapter 5 requires serializers to "produce deterministic output" — using
 * [JsonObject]'s ordered map makes that automatic rather than incidental.
 *
 * This is intentionally general-purpose (not Project-specific) so the next
 * entity's serializer (Node, in Phase 3) can reuse it. If the number of
 * serializable entities grows much further, revisit whether
 * kotlinx.serialization is worth the dependency at that point — this is a
 * deliberate, revisitable trade-off, not a permanent architectural stance.
 */
sealed class JsonValue {
    data class JsonString(val value: String) : JsonValue()
    data class JsonNumber(val value: Double) : JsonValue()
    data class JsonBool(val value: Boolean) : JsonValue()
    object JsonNull : JsonValue()
    data class JsonArray(val items: List<JsonValue>) : JsonValue()

    /** LinkedHashMap preserves insertion order, which is what makes output deterministic. */
    data class JsonObject(val fields: LinkedHashMap<String, JsonValue>) : JsonValue() {
        operator fun get(key: String): JsonValue? = fields[key]
    }
}

class JsonParseException(message: String) : Exception(message)

object Json {

    // -----------------------------------------------------------------
    // Writing
    // -----------------------------------------------------------------

    fun stringify(value: JsonValue, pretty: Boolean = true): String {
        val sb = StringBuilder()
        write(value, sb, indent = 0, pretty = pretty)
        return sb.toString()
    }

    private fun write(value: JsonValue, sb: StringBuilder, indent: Int, pretty: Boolean) {
        when (value) {
            is JsonValue.JsonNull -> sb.append("null")
            is JsonValue.JsonBool -> sb.append(if (value.value) "true" else "false")
            is JsonValue.JsonNumber -> sb.append(formatNumber(value.value))
            is JsonValue.JsonString -> sb.append(quote(value.value))
            is JsonValue.JsonArray -> writeArray(value, sb, indent, pretty)
            is JsonValue.JsonObject -> writeObject(value, sb, indent, pretty)
        }
    }

    private fun writeArray(array: JsonValue.JsonArray, sb: StringBuilder, indent: Int, pretty: Boolean) {
        if (array.items.isEmpty()) {
            sb.append("[]")
            return
        }
        sb.append("[")
        if (pretty) sb.append('\n')
        array.items.forEachIndexed { index, item ->
            if (pretty) sb.append(indentStr(indent + 1))
            write(item, sb, indent + 1, pretty)
            if (index != array.items.lastIndex) sb.append(",")
            if (pretty) sb.append('\n')
        }
        if (pretty) sb.append(indentStr(indent))
        sb.append("]")
    }

    private fun writeObject(obj: JsonValue.JsonObject, sb: StringBuilder, indent: Int, pretty: Boolean) {
        if (obj.fields.isEmpty()) {
            sb.append("{}")
            return
        }
        sb.append("{")
        if (pretty) sb.append('\n')
        val entries = obj.fields.entries.toList()
        entries.forEachIndexed { index, (key, v) ->
            if (pretty) sb.append(indentStr(indent + 1))
            sb.append(quote(key)).append(if (pretty) ": " else ":")
            write(v, sb, indent + 1, pretty)
            if (index != entries.lastIndex) sb.append(",")
            if (pretty) sb.append('\n')
        }
        if (pretty) sb.append(indentStr(indent))
        sb.append("}")
    }

    private fun indentStr(level: Int): String = "  ".repeat(level)

    private fun formatNumber(n: Double): String =
        if (n == Math.floor(n) && !n.isInfinite() && Math.abs(n) < 1e15) {
            n.toLong().toString()
        } else {
            n.toString()
        }

    private fun quote(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c.code < 0x20) sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        sb.append("\"")
        return sb.toString()
    }

    // -----------------------------------------------------------------
    // Reading
    // -----------------------------------------------------------------

    fun parse(text: String): JsonValue {
        val parser = Parser(text)
        val value = parser.parseValue()
        parser.skipWhitespace()
        if (!parser.isAtEnd()) {
            throw JsonParseException("Unexpected trailing content at position ${parser.pos}")
        }
        return value
    }

    private class Parser(private val text: String) {
        var pos: Int = 0

        fun isAtEnd(): Boolean = pos >= text.length

        fun skipWhitespace() {
            while (pos < text.length && text[pos].isWhitespace()) pos++
        }

        fun parseValue(): JsonValue {
            skipWhitespace()
            if (isAtEnd()) throw JsonParseException("Unexpected end of input")
            return when (text[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> JsonValue.JsonString(parseStringLiteral())
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                else -> parseNumber()
            }
        }

        private fun parseObject(): JsonValue.JsonObject {
            expect('{')
            val fields = LinkedHashMap<String, JsonValue>()
            skipWhitespace()
            if (peek() == '}') {
                pos++
                return JsonValue.JsonObject(fields)
            }
            while (true) {
                skipWhitespace()
                val key = parseStringLiteral()
                skipWhitespace()
                expect(':')
                val value = parseValue()
                fields[key] = value
                skipWhitespace()
                when (peek()) {
                    ',' -> {
                        pos++
                    }
                    '}' -> {
                        pos++
                        return JsonValue.JsonObject(fields)
                    }
                    else -> throw JsonParseException("Expected ',' or '}' at position $pos")
                }
            }
        }

        private fun parseArray(): JsonValue.JsonArray {
            expect('[')
            val items = mutableListOf<JsonValue>()
            skipWhitespace()
            if (peek() == ']') {
                pos++
                return JsonValue.JsonArray(items)
            }
            while (true) {
                items.add(parseValue())
                skipWhitespace()
                when (peek()) {
                    ',' -> {
                        pos++
                    }
                    ']' -> {
                        pos++
                        return JsonValue.JsonArray(items)
                    }
                    else -> throw JsonParseException("Expected ',' or ']' at position $pos")
                }
            }
        }

        private fun parseStringLiteral(): String {
            expect('"')
            val sb = StringBuilder()
            while (true) {
                if (isAtEnd()) throw JsonParseException("Unterminated string")
                val c = text[pos++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (isAtEnd()) throw JsonParseException("Unterminated escape sequence")
                        when (val esc = text[pos++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'b' -> sb.append('\b')
                            'u' -> {
                                if (pos + 4 > text.length) throw JsonParseException("Invalid unicode escape")
                                val hex = text.substring(pos, pos + 4)
                                sb.append(hex.toInt(16).toChar())
                                pos += 4
                            }
                            else -> throw JsonParseException("Invalid escape character: $esc")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun parseBoolean(): JsonValue.JsonBool {
            return if (text.startsWith("true", pos)) {
                pos += 4
                JsonValue.JsonBool(true)
            } else if (text.startsWith("false", pos)) {
                pos += 5
                JsonValue.JsonBool(false)
            } else {
                throw JsonParseException("Invalid literal at position $pos")
            }
        }

        private fun parseNull(): JsonValue.JsonNull {
            if (text.startsWith("null", pos)) {
                pos += 4
                return JsonValue.JsonNull
            }
            throw JsonParseException("Invalid literal at position $pos")
        }

        private fun parseNumber(): JsonValue.JsonNumber {
            val start = pos
            if (peek() == '-') pos++
            while (!isAtEnd() && text[pos].isDigit()) pos++
            if (!isAtEnd() && text[pos] == '.') {
                pos++
                while (!isAtEnd() && text[pos].isDigit()) pos++
            }
            if (!isAtEnd() && (text[pos] == 'e' || text[pos] == 'E')) {
                pos++
                if (!isAtEnd() && (text[pos] == '+' || text[pos] == '-')) pos++
                while (!isAtEnd() && text[pos].isDigit()) pos++
            }
            if (pos == start) throw JsonParseException("Invalid number at position $pos")
            return JsonValue.JsonNumber(text.substring(start, pos).toDouble())
        }

        private fun peek(): Char {
            if (isAtEnd()) throw JsonParseException("Unexpected end of input")
            return text[pos]
        }

        private fun expect(c: Char) {
            skipWhitespace()
            if (isAtEnd() || text[pos] != c) {
                throw JsonParseException("Expected '$c' at position $pos")
            }
            pos++
        }
    }
}

// ---------------------------------------------------------------------
// Small convenience builders/accessors, used by entity serializers.
// ---------------------------------------------------------------------

fun jsonObjectOf(vararg pairs: Pair<String, JsonValue>): JsonValue.JsonObject =
    JsonValue.JsonObject(LinkedHashMap(pairs.toMap()))

fun jsonArrayOf(items: List<JsonValue>): JsonValue.JsonArray = JsonValue.JsonArray(items)

fun String.toJson(): JsonValue.JsonString = JsonValue.JsonString(this)

fun JsonValue.JsonObject.requireString(field: String): String =
    (this[field] as? JsonValue.JsonString)?.value
        ?: throw JsonParseException("Missing or invalid required string field '$field'")

fun JsonValue.JsonObject.optionalString(field: String, default: String = ""): String =
    (this[field] as? JsonValue.JsonString)?.value ?: default

fun JsonValue.JsonObject.stringList(field: String): List<String> =
    (this[field] as? JsonValue.JsonArray)?.items?.mapNotNull { (it as? JsonValue.JsonString)?.value } ?: emptyList()
