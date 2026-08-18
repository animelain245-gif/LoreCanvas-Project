package com.lorecanvas.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import com.lorecanvas.domain.Template

/** Template List — LCD-009 Ch.13's "Choose Template" entry point. */
@Composable
fun TemplateListScreen(
    templates: List<Template>,
    statusMessage: String?,
    isError: Boolean,
    onBack: () -> Unit,
    onApplyTemplate: (Template, nodeName: String) -> Unit,
    onDeleteTemplate: (String) -> Unit
) {
    var templateToApply by remember { mutableStateOf<Template?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        TextButton(onClick = onBack) { Text("← Back to Workspace") }
        Spacer(Modifier.height(16.dp))
        Text("Templates", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Templates capture a Node's Card structure so you don't rebuild it every time. Use \"Save as Template\" from a Node's editor to create one.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))

        if (statusMessage != null) {
            Text(
                statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) com.lorecanvas.app.ui.theme.Pending else com.lorecanvas.app.ui.theme.Ok
            )
            Spacer(Modifier.height(16.dp))
        }

        if (templates.isEmpty()) {
            Text("No templates yet.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(templates, key = { it.id }) { template ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(template.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "For: ${template.targetNodeType} · ${template.defaultCards.size} default card(s)",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { templateToApply = template }) { Text("Apply") }
                                OutlinedButton(onClick = { onDeleteTemplate(template.id) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }

    val applying = templateToApply
    if (applying != null) {
        var nodeName by remember(applying.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { templateToApply = null },
            title = { Text("New \"${applying.targetNodeType}\" from \"${applying.name}\"") },
            text = {
                OutlinedTextField(value = nodeName, onValueChange = { nodeName = it }, label = { Text("Node name") }, singleLine = true)
            },
            confirmButton = {
                TextButton(
                    onClick = { templateToApply = null; onApplyTemplate(applying, nodeName) },
                    enabled = nodeName.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { templateToApply = null }) { Text("Cancel") } }
        )
    }
}
