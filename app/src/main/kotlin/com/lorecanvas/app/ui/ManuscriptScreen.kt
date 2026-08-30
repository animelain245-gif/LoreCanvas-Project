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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lorecanvas.domain.Node

@Composable
fun ManuscriptScreen(
    stories: List<Node>,
    chapters: Map<String, List<Node>>,
    scenes: Map<String, List<Node>>,
    onOpenScene: (Node) -> Unit,
    onPreviewStory: (Node) -> Unit,
    onReorderNodes: (parentId: String?, List<String>) -> Unit,
    onAddChapter: (storyId: String) -> Unit,
    onAddScene: (chapterId: String) -> Unit,
    onDeleteHierarchy: (nodeId: String) -> Unit,
    onMoveNode: (nodeId: String, newParentId: String?) -> Unit,
    onBack: () -> Unit
) {
    var nodeToDelete by remember { mutableStateOf<Node?>(null) }
    var sceneToMove by remember { mutableStateOf<Node?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        TextButton(onClick = onBack) { Text("← Back to Workspace") }
        Spacer(Modifier.height(16.dp))
        Text("Manuscript", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        if (stories.isEmpty()) {
            Text("No stories yet. Start writing to create your first manuscript.")
        }

        LazyColumn {
            stories.forEach { story ->
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(story.name, style = MaterialTheme.typography.titleLarge)
                        Row {
                            Button(onClick = { onPreviewStory(story) }) { Text("Preview") }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { nodeToDelete = story }) {
                                Text("Delete")
                            }
                        }
                    }
                    Button(onClick = { onAddChapter(story.id) }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("+ Add Chapter")
                    }
                    Spacer(Modifier.height(16.dp))
                }
                
                val storyChapters = chapters[story.id] ?: emptyList()
                storyChapters.forEachIndexed { index, chapter ->
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("  ${chapter.name}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { onAddScene(chapter.id) }) {
                                Text("+ Scene")
                            }
                            TextButton(onClick = { nodeToDelete = chapter }) {
                                Text("Delete")
                            }
                            TextButton(onClick = { 
                                if (index > 0) {
                                    val newOrder = storyChapters.map { it.id }.toMutableList()
                                    val tmp = newOrder[index]
                                    newOrder[index] = newOrder[index - 1]
                                    newOrder[index - 1] = tmp
                                    onReorderNodes(story.id, newOrder)
                                }
                            }, enabled = index > 0) {
                                Text("↑")
                            }
                            TextButton(onClick = { 
                                if (index < storyChapters.lastIndex) {
                                    val newOrder = storyChapters.map { it.id }.toMutableList()
                                    val tmp = newOrder[index]
                                    newOrder[index] = newOrder[index + 1]
                                    newOrder[index + 1] = tmp
                                    onReorderNodes(story.id, newOrder)
                                }
                            }, enabled = index < storyChapters.lastIndex) {
                                Text("↓")
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    
                    val chapterScenes = scenes[chapter.id] ?: emptyList()
                    items(chapterScenes.size) { sceneIndex ->
                        val scene = chapterScenes[sceneIndex]
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(start = 32.dp, top = 4.dp, bottom = 4.dp),
                            onClick = { onOpenScene(scene) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                                Text(scene.name, modifier = Modifier.weight(1f).padding(8.dp))
                                TextButton(onClick = { sceneToMove = scene }) {
                                    Text("Move")
                                }
                                TextButton(onClick = { nodeToDelete = scene }) {
                                    Text("Delete")
                                }
                                TextButton(onClick = { 
                                    if (sceneIndex > 0) {
                                        val newOrder = chapterScenes.map { it.id }.toMutableList()
                                        val tmp = newOrder[sceneIndex]
                                        newOrder[sceneIndex] = newOrder[sceneIndex - 1]
                                        newOrder[sceneIndex - 1] = tmp
                                        onReorderNodes(chapter.id, newOrder)
                                    }
                                }, enabled = sceneIndex > 0) {
                                    Text("↑")
                                }
                                TextButton(onClick = { 
                                    if (sceneIndex < chapterScenes.lastIndex) {
                                        val newOrder = chapterScenes.map { it.id }.toMutableList()
                                        val tmp = newOrder[sceneIndex]
                                        newOrder[sceneIndex] = newOrder[sceneIndex + 1]
                                        newOrder[sceneIndex + 1] = tmp
                                        onReorderNodes(chapter.id, newOrder)
                                    }
                                }, enabled = sceneIndex < chapterScenes.lastIndex) {
                                    Text("↓")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    nodeToDelete?.let { node ->
        AlertDialog(
            onDismissRequest = { nodeToDelete = null },
            title = { Text("Delete ${node.type}?") },
            text = { Text("Are you sure you want to delete \"${node.name}\"? This will also delete all its contents and cannot be undone (except via Undo).") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteHierarchy(node.id)
                    nodeToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { nodeToDelete = null }) { Text("Cancel") }
            }
        )
    }

    sceneToMove?.let { scene ->
        AlertDialog(
            onDismissRequest = { sceneToMove = null },
            title = { Text("Move Scene to Chapter") },
            text = {
                Column {
                    val allChapters = chapters.values.flatten()
                    allChapters.forEach { chapter ->
                        TextButton(
                            onClick = {
                                onMoveNode(scene.id, chapter.id)
                                sceneToMove = null
                            },
                            enabled = chapter.id != scene.parentNodeId,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(chapter.name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { sceneToMove = null }) { Text("Cancel") }
            }
        )
    }
}
