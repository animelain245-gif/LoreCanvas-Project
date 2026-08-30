package com.lorecanvas.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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

@Composable
fun ProseEditorScreen(
    scene: Node,
    prose: String,
    pinnedNodes: List<Node>,
    isDirty: Boolean,
    statusMessage: String?,
    isError: Boolean,
    onUpdateProse: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onOpenReference: (Node) -> Unit
) {
    var textField by remember(scene.id) { mutableStateOf(prose) }
    var showSidebar by remember { mutableStateOf(true) }

    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f).padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                Row {
                    TextButton(onClick = onBack) { Text("← Manuscript") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { showSidebar = !showSidebar }) { Text(if (showSidebar) "Hide Sidebar" else "Show Sidebar") }
                }
                if (isDirty) {
                    Text("Unsaved changes", color = Pending, style = MaterialTheme.typography.labelSmall)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            Text(scene.name, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = textField,
                onValueChange = {
                    textField = it
                    onUpdateProse(it)
                },
                label = { Text("Prose") },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            Spacer(Modifier.height(16.dp))
            Row {
                Button(onClick = onSave) { Text("Save") }
            }

            if (statusMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(statusMessage, color = if (isError) Pending else Ok)
            }
        }

        if (showSidebar) {
            ReferenceSidebar(pinnedNodes = pinnedNodes, onOpenNode = onOpenReference)
        }
    }
}
