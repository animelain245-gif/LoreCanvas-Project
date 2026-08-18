package com.lorecanvas.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lorecanvas.app.ui.theme.Ok
import com.lorecanvas.app.ui.theme.Pending
import com.lorecanvas.domain.Relationship

/**
 * Relationship Editor — finishes the Relationship workflow beyond
 * create/delete: editing type/description, and adding a
 * RelationshipContext history entry (LCD-005 Ch.12 — "instead of deleting
 * relationships, LoreCanvas preserves their history").
 */
@Composable
fun RelationshipEditorScreen(
    relationship: Relationship,
    sourceLabel: String,
    targetLabel: String,
    statusMessage: String?,
    isError: Boolean,
    onChangeType: (String) -> Unit,
    onUpdateDescription: (String) -> Unit,
    onAddContext: (startDate: String, description: String, endDate: String?) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    var typeField by remember(relationship.id) { mutableStateOf(relationship.type) }
    var descriptionField by remember(relationship.id) { mutableStateOf(relationship.description) }
    var showAddContextDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        TextButton(onClick = onBack) { Text("← Back") }
        Spacer(Modifier.height(16.dp))

        Text("$sourceLabel → $targetLabel", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = typeField,
            onValueChange = { typeField = it; onChangeType(it) },
            label = { Text("Relationship type") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = descriptionField,
            onValueChange = { descriptionField = it; onUpdateDescription(it) },
            label = { Text("Description") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("History (${relationship.contexts.size})", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { showAddContextDialog = true }) { Text("+ Add Historical Entry") }
        }
        Spacer(Modifier.height(8.dp))

        if (relationship.contexts.isEmpty()) {
            Text(
                "No history yet. Add an entry when this relationship's nature changes over time — friend becomes rival, ally becomes enemy — without losing what it used to be.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        relationship.contexts.forEach { context ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    val range = if (context.endDate != null) "${context.startDate} – ${context.endDate}" else "${context.startDate} –"
                    Text(range, style = MaterialTheme.typography.labelSmall)
                    Text(context.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onSave) { Text("Save") }
            OutlinedButton(onClick = { showDeleteConfirm = true }) { Text("Delete Relationship") }
        }

        if (statusMessage != null) {
            Spacer(Modifier.height(16.dp))
            Text(statusMessage, style = MaterialTheme.typography.bodyMedium, color = if (isError) Pending else Ok)
        }
    }

    if (showAddContextDialog) {
        var startDate by remember { mutableStateOf("") }
        var endDate by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddContextDialog = false },
            title = { Text("Add Historical Entry") },
            text = {
                Column {
                    OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("Start date") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = endDate, onValueChange = { endDate = it }, label = { Text("End date (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("What changed") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAddContextDialog = false
                        onAddContext(startDate, description, endDate.ifBlank { null })
                    },
                    enabled = startDate.isNotBlank() && description.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddContextDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this relationship?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}
