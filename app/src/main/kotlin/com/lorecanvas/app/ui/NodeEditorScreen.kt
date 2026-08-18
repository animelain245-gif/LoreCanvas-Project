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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.lorecanvas.domain.CardTypes
import com.lorecanvas.domain.Node
import com.lorecanvas.domain.NodeStatus
import com.lorecanvas.domain.Relationship
import com.lorecanvas.domain.RelationshipTypes

/**
 * Node Editor screen — LCD-009 Ch.7-8 (Node Editing/Deletion). Phase 4/5
 * add a Cards section (Ch.9-10) and a Relationships section (Ch.11) below
 * the Node's own fields.
 */
@Composable
fun NodeEditorScreen(
    node: Node,
    cards: List<com.lorecanvas.domain.Card>,
    relationships: List<Relationship>,
    otherNodes: List<Node>,
    isDirty: Boolean,
    statusMessage: String?,
    isError: Boolean,
    onRename: (String) -> Unit,
    onChangeType: (String) -> Unit,
    onUpdateSummary: (String) -> Unit,
    onAddTag: (String) -> Unit,
    onArchiveToggle: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    onCreateCard: (title: String, type: String) -> Unit,
    onOpenCard: (com.lorecanvas.domain.Card) -> Unit,
    onCreateRelationship: (targetNodeId: String, type: String, description: String) -> Unit,
    onDeleteRelationship: (String) -> Unit,
    onOpenRelationship: (Relationship) -> Unit,
    onSaveAsTemplate: (templateName: String) -> Unit
) {
    var nameField by remember(node.id) { mutableStateOf(node.name) }
    var typeField by remember(node.id) { mutableStateOf(node.type) }
    var summaryField by remember(node.id) { mutableStateOf(node.summary) }
    var newTag by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCreateCardDialog by remember { mutableStateOf(false) }
    var showCreateRelDialog by remember { mutableStateOf(false) }
    var showSaveAsTemplateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("← Back to Nodes") }
            if (isDirty) {
                Text("Unsaved changes", style = MaterialTheme.typography.labelSmall, color = Pending)
            }
        }

        Spacer(Modifier.height(16.dp))

        if (node.status == NodeStatus.ARCHIVED) {
            AssistChip(onClick = {}, label = { Text("Archived") })
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = nameField,
            onValueChange = { nameField = it; onRename(it) },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = typeField,
            onValueChange = { typeField = it; onChangeType(it) },
            label = { Text("Type") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = summaryField,
            onValueChange = { summaryField = it; onUpdateSummary(it) },
            label = { Text("Summary") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Text("Tags", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(node.tags) { tag -> AssistChip(onClick = {}, label = { Text(tag) }) }
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
            OutlinedButton(onClick = onArchiveToggle) {
                Text(if (node.status == NodeStatus.ARCHIVED) "Restore" else "Archive")
            }
            OutlinedButton(onClick = { showDeleteConfirm = true }) { Text("Delete") }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { showSaveAsTemplateDialog = true }) { Text("Save as Template") }

        if (statusMessage != null) {
            Spacer(Modifier.height(16.dp))
            Text(statusMessage, style = MaterialTheme.typography.bodyMedium, color = if (isError) Pending else Ok)
        }

        Spacer(Modifier.height(24.dp))
        DetailRow("ID", node.id)
        DetailRow("Created", node.createdAt)
        DetailRow("Modified", node.modifiedAt)

        // ---------------- Cards ----------------
        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Cards (${cards.size})", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { showCreateCardDialog = true }) { Text("+ New Card") }
        }
        Spacer(Modifier.height(8.dp))
        if (cards.isEmpty()) {
            Text("No cards yet. Cards hold everything known about this node.", style = MaterialTheme.typography.bodyMedium)
        }
        cards.forEach { card ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(card.title, style = MaterialTheme.typography.titleMedium)
                        Text(card.type, style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = { onOpenCard(card) }) { Text("Open") }
                }
            }
        }

        // ---------------- Relationships ----------------
        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Relationships (${relationships.size})", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { showCreateRelDialog = true }, enabled = otherNodes.isNotEmpty()) {
                Text("+ New Relationship")
            }
        }
        Spacer(Modifier.height(8.dp))
        if (relationships.isEmpty()) {
            Text("No relationships yet.", style = MaterialTheme.typography.bodyMedium)
        }
        relationships.forEach { rel ->
            val isSource = rel.sourceNodeId == node.id
            val otherId = rel.otherNodeId(node.id)
            val otherName = otherNodes.find { it.id == otherId }?.name ?: "(unknown)"
            val label = if (isSource) "${rel.type} → $otherName" else "$otherName → ${rel.type} → this"
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                        if (rel.description.isNotBlank()) {
                            Text(rel.description, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Row {
                        TextButton(onClick = { onOpenRelationship(rel) }) { Text("Open") }
                        TextButton(onClick = { onDeleteRelationship(rel.id) }) { Text("Remove") }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete \"${node.name}\"?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showCreateCardDialog) {
        CreateCardDialog(
            onDismiss = { showCreateCardDialog = false },
            onConfirm = { title, type -> showCreateCardDialog = false; onCreateCard(title, type) }
        )
    }

    if (showCreateRelDialog) {
        CreateRelationshipDialog(
            otherNodes = otherNodes,
            onDismiss = { showCreateRelDialog = false },
            onConfirm = { targetId, type, desc -> showCreateRelDialog = false; onCreateRelationship(targetId, type, desc) }
        )
    }

    if (showSaveAsTemplateDialog) {
        var templateName by remember { mutableStateOf("${node.name} Template") }
        AlertDialog(
            onDismissRequest = { showSaveAsTemplateDialog = false },
            title = { Text("Save as Template") },
            text = {
                Column {
                    Text("Captures this node's ${cards.size} card(s) as a reusable template.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = templateName, onValueChange = { templateName = it }, label = { Text("Template name") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showSaveAsTemplateDialog = false; onSaveAsTemplate(templateName) },
                    enabled = templateName.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSaveAsTemplateDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CreateCardDialog(onDismiss: () -> Unit, onConfirm: (title: String, type: String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(CardTypes.SUGGESTED.first()) }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Card") },
        text = {
            Column {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(expanded = typeMenuExpanded, onExpandedChange = { typeMenuExpanded = it }) {
                    OutlinedTextField(
                        value = type, onValueChange = { type = it },
                        label = { Text("Card type") }, singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    DropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                        CardTypes.SUGGESTED.forEach { suggestion ->
                            DropdownMenuItem(text = { Text(suggestion) }, onClick = { type = suggestion; typeMenuExpanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title, type) }, enabled = title.isNotBlank() && type.isNotBlank()) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CreateRelationshipDialog(
    otherNodes: List<Node>,
    onDismiss: () -> Unit,
    onConfirm: (targetNodeId: String, type: String, description: String) -> Unit
) {
    var targetId by remember { mutableStateOf(otherNodes.firstOrNull()?.id ?: "") }
    var targetMenuExpanded by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(RelationshipTypes.SUGGESTED.first()) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }

    val targetName = otherNodes.find { it.id == targetId }?.name ?: ""

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Relationship") },
        text = {
            Column {
                ExposedDropdownMenuBox(expanded = targetMenuExpanded, onExpandedChange = { targetMenuExpanded = it }) {
                    OutlinedTextField(
                        value = targetName, onValueChange = {}, readOnly = true,
                        label = { Text("Target node") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    DropdownMenu(expanded = targetMenuExpanded, onDismissRequest = { targetMenuExpanded = false }) {
                        otherNodes.forEach { candidate ->
                            DropdownMenuItem(
                                text = { Text("${candidate.name} (${candidate.type})") },
                                onClick = { targetId = candidate.id; targetMenuExpanded = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(expanded = typeMenuExpanded, onExpandedChange = { typeMenuExpanded = it }) {
                    OutlinedTextField(
                        value = type, onValueChange = { type = it },
                        label = { Text("Relationship type") }, singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    DropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                        RelationshipTypes.SUGGESTED.forEach { suggestion ->
                            DropdownMenuItem(text = { Text(suggestion) }, onClick = { type = suggestion; typeMenuExpanded = false })
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(targetId, type, description) },
                enabled = targetId.isNotBlank() && type.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.wrapContentWidth())
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
