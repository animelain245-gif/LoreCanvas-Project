package com.lorecanvas.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lorecanvas.domain.Node

@Composable
fun ReferenceSidebar(
    pinnedNodes: List<Node>,
    onOpenNode: (Node) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxHeight().width(300.dp).padding(16.dp)) {
        Text("Reference", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        Text("Pinned Nodes", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(8.dp))

        if (pinnedNodes.isEmpty()) {
            Text("No pinned nodes. Pin nodes in the Workspace to see them here.", style = MaterialTheme.typography.bodySmall)
        }

        LazyColumn {
            items(pinnedNodes) { node ->
                Card(
                    modifier = Modifier.padding(vertical = 4.dp),
                    onClick = { onOpenNode(node) }
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(node.name, style = MaterialTheme.typography.bodyMedium)
                        Text(node.type, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        
        // Future: Detected Nodes (Phase 9D)
        // Future: Chapter Info
    }
}
