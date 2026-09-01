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
import com.lorecanvas.domain.Node
import com.lorecanvas.domain.Timeline
import com.lorecanvas.domain.TimelineEvent

/** Timeline Editor — LCD-009 Ch.12 (Timeline Workflow: create/edit events, sorted chronologically). */
@Composable
fun TimelineEditorScreen(
    timeline: Timeline,
    sortedEvents: List<TimelineEvent>,
    allNodes: List<Node>,
    statusMessage: String?,
    isError: Boolean,
    onRename: (String) -> Unit,
    onAddEvent: (date: String, title: String, description: String) -> Unit,
    onEditEvent: (updated: TimelineEvent) -> Unit,
    onRemoveEvent: (String) -> Unit,
    onBack: () -> Unit
) {
    var nameField by remember(timeline.id) { mutableStateOf(timeline.name) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<TimelineEvent?>(null) }
    var eventToDeleteId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        TextButton(onClick = onBack) { Text("← Back to Timelines") }
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = nameField,
            onValueChange = { nameField = it; onRename(it) },
            label = { Text("Timeline name") },
            modifier = Modifier.fillMaxWidth()
        )

        if (statusMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(statusMessage, style = MaterialTheme.typography.bodyMedium, color = if (isError) Pending else Ok)
        }

        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Events (${sortedEvents.size})", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { showAddEventDialog = true }) { Text("+ Add Event") }
        }
        Spacer(Modifier.height(8.dp))

        if (sortedEvents.isEmpty()) {
            Text("No events yet.", style = MaterialTheme.typography.bodyMedium)
        }

        sortedEvents.forEach { event ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(event.date, style = MaterialTheme.typography.labelSmall)
                        Row {
                            TextButton(onClick = { editingEvent = event }) { Text("Edit") }
                            TextButton(onClick = { eventToDeleteId = event.id }) { Text("Remove") }
                        }
                    }
                    Text(event.title, style = MaterialTheme.typography.titleMedium)
                    if (event.description.isNotBlank()) {
                        Text(event.description, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (event.relatedNodeIds.isNotEmpty()) {
                        val names = event.relatedNodeIds.mapNotNull { id -> allNodes.find { it.id == id }?.name }
                        Text("Related: ${names.joinToString()}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }

    if (showAddEventDialog) {
        var date by remember { mutableStateOf("") }
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddEventDialog = false },
            title = { Text("New Event") },
            text = {
                Column {
                    OutlinedTextField(
                        value = date, onValueChange = { date = it },
                        label = { Text("Date (e.g. 1012)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = description, onValueChange = { description = it },
                        label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showAddEventDialog = false; onAddEvent(date, title, description) },
                    enabled = date.isNotBlank() && title.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddEventDialog = false }) { Text("Cancel") } }
        )
    }

    val eventBeingEdited = editingEvent
    if (eventBeingEdited != null) {
        var date by remember(eventBeingEdited.id) { mutableStateOf(eventBeingEdited.date) }
        var title by remember(eventBeingEdited.id) { mutableStateOf(eventBeingEdited.title) }
        var description by remember(eventBeingEdited.id) { mutableStateOf(eventBeingEdited.description) }

        AlertDialog(
            onDismissRequest = { editingEvent = null },
            title = { Text("Edit Event") },
            text = {
                Column {
                    OutlinedTextField(
                        value = date, onValueChange = { date = it },
                        label = { Text("Date") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = description, onValueChange = { description = it },
                        label = { Text("Description") }, modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        editingEvent = null
                        onEditEvent(eventBeingEdited.copy(date = date, title = title, description = description))
                    },
                    enabled = date.isNotBlank() && title.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingEvent = null }) { Text("Cancel") } }
        )
    }

    eventToDeleteId?.let { eventId ->
        AlertDialog(
            onDismissRequest = { eventToDeleteId = null },
            title = { Text("Remove Event?") },
            text = { Text("This can be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        eventToDeleteId = null
                        onRemoveEvent(eventId)
                    }
                ) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { eventToDeleteId = null }) { Text("Cancel") } }
        )
    }
}
