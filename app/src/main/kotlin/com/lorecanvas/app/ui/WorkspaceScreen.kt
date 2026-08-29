package com.lorecanvas.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.unit.dp
import com.lorecanvas.app.ui.theme.Ok
import com.lorecanvas.app.ui.theme.Pending
import com.lorecanvas.domain.Node
import com.lorecanvas.domain.NodeTypes
import com.lorecanvas.domain.Project

/**
 * Workspace screen — the "inside a Project" view (LCD-009's Workspace
 * concept). Phase 3 adds the Node list + Create Node entry point on top of
 * Phase 2's Project detail/rename/tag/Save/Close. Real Workspace chrome
 * (sidebar, inspector, panels) is still Phase 9 — this stays a single
 * scrolling screen deliberately.
 */
@Composable
fun WorkspaceScreen(
    project: Project,
    nodes: List<Node>,
    isDirty: Boolean,
    statusMessage: String?,
    isError: Boolean,
    onRename: (String) -> Unit,
    onAddTag: (String) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    onCreateNode: (name: String, type: String, summary: String) -> Unit,
    onOpenNode: (Node) -> Unit,
    onOpenTimelines: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenGraph: () -> Unit,
    onOpenTemplates: () -> Unit,
    onExportProject: () -> Unit,
    onImportProject: () -> Unit,
    onStartWriting: () -> Unit,
    onOpenManuscript: () -> Unit
) {
    var nameField by remember(project.id) { mutableStateOf(project.name) }
    var newTag by remember { mutableStateOf("") }
    var showCreateNodeDialog by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onClose) {
                    Text("← Close Project")
                }
                if (isDirty) {
                    Text("Unsaved changes", style = MaterialTheme.typography.labelSmall, color = Pending)
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onStartWriting) { Text("Start Writing") }
                OutlinedButton(onClick = onOpenManuscript) { Text("Story") }
                OutlinedButton(onClick = onOpenTimelines) { Text("Timeline") }
                OutlinedButton(onClick = onOpenSearch) { Text("Search") }
                OutlinedButton(onClick = onOpenGraph) { Text("Graph") }
                OutlinedButton(onClick = onOpenTemplates) { Text("Templates") }
                OutlinedButton(onClick = onExportProject) { Text("Export") }
                OutlinedButton(onClick = onImportProject) { Text("Import") }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = nameField,
                onValueChange = {
                    nameField = it
                    onRename(it)
                },
                label = { Text("Project name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("Tags", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(project.tags) { tag ->
                    AssistChip(onClick = {}, label = { Text(tag) })
                }
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
                OutlinedButton(onClick = {
                    if (newTag.isNotBlank()) {
                        onAddTag(newTag)
                        newTag = ""
                    }
                }) {
                    Text("Add")
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onSave) {
                    Text("Save")
                }
                OutlinedButton(onClick = onClose) {
                    Text("Close")
                }
            }

            if (statusMessage != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError) Pending else Ok
                )
            }

            Spacer(Modifier.height(24.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Project details", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                DetailRow("ID", project.id)
                DetailRow("Version", project.version)
                DetailRow("Created", project.createdAt)
                DetailRow("Modified", project.modifiedAt)
            }

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Nodes (${nodes.size})", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { showCreateNodeDialog = true }) {
                    Text("+ New Node")
                }
            }
            Spacer(Modifier.height(8.dp))

            if (nodes.isEmpty()) {
                Text(
                    "No nodes yet. A Node represents any significant concept in your world — " +
                        "a character, a kingdom, an item, an event, anything.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        items(nodes, key = { it.id }) { node ->
            NodeRow(node, onClick = { onOpenNode(node) })
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showCreateNodeDialog) {
        CreateNodeDialog(
            onDismiss = { showCreateNodeDialog = false },
            onConfirm = { name, type, summary ->
                showCreateNodeDialog = false
                onCreateNode(name, type, summary)
            }
        )
    }
}

@Composable
private fun NodeRow(node: Node, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(node.name, style = MaterialTheme.typography.titleMedium)
                AssistChip(onClick = {}, label = { Text(node.type) })
            }
            if (node.summary.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(node.summary, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onClick) {
                Text("Open")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateNodeDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, summary: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(NodeTypes.SUGGESTED.first()) }
    var summary by remember { mutableStateOf("") }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Node") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = typeMenuExpanded,
                    onExpandedChange = { typeMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text("Type") },
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    DropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false }
                    ) {
                        NodeTypes.SUGGESTED.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    type = suggestion
                                    typeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Summary (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, type, summary) },
                enabled = name.isNotBlank() && type.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.wrapContentWidth()
        )
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
