package com.lorecanvas.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorecanvas.app.ui.theme.Ok
import com.lorecanvas.app.ui.theme.Pending
import com.lorecanvas.common.Markdown
import com.lorecanvas.common.MarkdownStyle
import com.lorecanvas.domain.Card

/** Card Editor — LCD-009 Ch.10 (Card Editing Workflow). */
@Composable
fun CardEditorScreen(
    card: Card,
    isDirty: Boolean,
    statusMessage: String?,
    isError: Boolean,
    onRename: (String) -> Unit,
    onChangeType: (String) -> Unit,
    onUpdateContent: (String) -> Unit,
    onAddTag: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    var titleField by remember(card.id) { mutableStateOf(card.title) }
    var typeField by remember(card.id) { mutableStateOf(card.type) }
    var contentField by remember(card.id) { mutableStateOf(card.content) }
    var previewMode by remember { mutableStateOf(false) }
    var newTag by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("← Back to Node") }
            if (isDirty) {
                Text("Unsaved changes", style = MaterialTheme.typography.labelSmall, color = Pending)
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = titleField,
            onValueChange = { titleField = it; onRename(it) },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = typeField,
            onValueChange = { typeField = it; onChangeType(it) },
            label = { Text("Card type") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Content", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { previewMode = false }) { Text(if (!previewMode) "● Edit" else "Edit") }
            TextButton(onClick = { previewMode = true }) { Text(if (previewMode) "● Preview" else "Preview") }
        }
        Spacer(Modifier.height(4.dp))
        if (previewMode) {
            Text(
                text = renderMarkdown(contentField),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
        } else {
            OutlinedTextField(
                value = contentField,
                onValueChange = { contentField = it; onUpdateContent(it) },
                label = { Text("Content (supports **bold**, *italic*, `code`, # headings)") },
                minLines = 8,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("Tags", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(card.tags) { tag -> AssistChip(onClick = {}, label = { Text(tag) }) }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newTag,
                onValueChange = { newTag = it },
                label = { Text("Add tag") },
                singleLine = true,
                modifier = Modifier.wrapContentWidth()
            )
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { if (newTag.isNotBlank()) { onAddTag(newTag); newTag = "" } }) {
                Text("Add")
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onSave) { Text("Save") }
            OutlinedButton(onClick = { showDeleteConfirm = true }) { Text("Delete") }
        }

        if (statusMessage != null) {
            Spacer(Modifier.height(16.dp))
            Text(statusMessage, style = MaterialTheme.typography.bodyMedium, color = if (isError) Pending else Ok)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete \"${card.title}\"?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

/**
 * Converts a Card's markdown-lite content (parsed by the pure-Kotlin
 * `com.lorecanvas.common.Markdown`) into a Compose `AnnotatedString` for
 * Preview mode. This is the only place that translates [MarkdownSpan]s
 * into actual Compose styling — the parser itself stays UI-framework-free.
 */
private fun renderMarkdown(content: String) = buildAnnotatedString {
    val lines = Markdown.parse(content)
    lines.forEachIndexed { index, line ->
        val baseFontSize = when (line.headingLevel) {
            1 -> 22.sp
            2 -> 18.sp
            3 -> 16.sp
            else -> null
        }
        withStyle(SpanStyle(fontSize = baseFontSize ?: 14.sp, fontWeight = if (line.headingLevel > 0) FontWeight.Bold else FontWeight.Normal)) {
            line.spans.forEach { span ->
                when (span.style) {
                    MarkdownStyle.PLAIN -> append(span.text)
                    MarkdownStyle.BOLD -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(span.text) }
                    MarkdownStyle.ITALIC -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(span.text) }
                    MarkdownStyle.CODE -> withStyle(SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)) { append(span.text) }
                }
            }
        }
        if (index != lines.lastIndex) append("\n")
    }
}
