package com.lorecanvas.common

/**
 * Minimal inline Markdown support for Card content — real rich text
 * editing, not a placeholder, scoped deliberately small: bold (`**x**`),
 * italic (`*x*`), inline code (`` `x` ``), and headings (`# `, `## `,
 * `### ` at the start of a line). This is the same "small, dependency-free,
 * fully verifiable" trade-off as `Json.kt` — a real WYSIWYG rich text
 * editor is a much larger undertaking than one Card field justifies right
 * now, and this parser is pure Kotlin, so it's testable the same way
 * everything else in this module is, with no Android/Compose dependency.
 *
 * The `app` module turns [MarkdownSpan]s into a Compose `AnnotatedString`
 * for the Preview mode in CardEditorScreen; editing itself stays plain
 * text (the markdown source), which is how most lightweight note apps
 * (this one included) implement "rich text" without a complex document
 * model.
 */
enum class MarkdownStyle { PLAIN, BOLD, ITALIC, CODE }

data class MarkdownSpan(val text: String, val style: MarkdownStyle)

data class MarkdownLine(val text: String, val headingLevel: Int, val spans: List<MarkdownSpan>)

object Markdown {

    /** Parses a full multi-line Card content string into per-line spans. */
    fun parse(content: String): List<MarkdownLine> = content.split("\n").map { rawLine -> parseLine(rawLine) }

    private fun parseLine(rawLine: String): MarkdownLine {
        var headingLevel = 0
        var text = rawLine
        for (level in 3 downTo 1) {
            val prefix = "#".repeat(level) + " "
            if (text.startsWith(prefix)) {
                headingLevel = level
                text = text.removePrefix(prefix)
                break
            }
        }
        return MarkdownLine(rawLine, headingLevel, parseInline(text))
    }

    /**
     * Parses one line's inline formatting. Deliberately simple single-pass
     * scanner rather than a general grammar — bold/italic/code don't nest
     * in this feature set, so a full recursive parser would be solving a
     * problem this Card field doesn't have.
     */
    fun parseInline(text: String): List<MarkdownSpan> {
        val spans = mutableListOf<MarkdownSpan>()
        val plain = StringBuilder()
        var i = 0

        fun flushPlain() {
            if (plain.isNotEmpty()) {
                spans.add(MarkdownSpan(plain.toString(), MarkdownStyle.PLAIN))
                plain.clear()
            }
        }

        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end == -1) {
                        plain.append(text.substring(i))
                        i = text.length
                    } else {
                        flushPlain()
                        spans.add(MarkdownSpan(text.substring(i + 2, end), MarkdownStyle.BOLD))
                        i = end + 2
                    }
                }
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end == -1) {
                        plain.append(text.substring(i))
                        i = text.length
                    } else {
                        flushPlain()
                        spans.add(MarkdownSpan(text.substring(i + 1, end), MarkdownStyle.CODE))
                        i = end + 1
                    }
                }
                text[i] == '*' -> {
                    val end = text.indexOf('*', i + 1)
                    if (end == -1) {
                        plain.append(text.substring(i))
                        i = text.length
                    } else {
                        flushPlain()
                        spans.add(MarkdownSpan(text.substring(i + 1, end), MarkdownStyle.ITALIC))
                        i = end + 1
                    }
                }
                else -> {
                    plain.append(text[i])
                    i++
                }
            }
        }
        flushPlain()
        return spans
    }
}
