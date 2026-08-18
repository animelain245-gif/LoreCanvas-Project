package com.lorecanvas.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorecanvas.app.ui.theme.Accent
import com.lorecanvas.app.ui.theme.Border
import com.lorecanvas.app.ui.theme.Ink400
import com.lorecanvas.app.ui.theme.Ink900
import com.lorecanvas.graph.GraphModel
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Graph View — "a visual, explorable map of how everything in a project
 * connects" (LCD-001 Ch.5).
 *
 * Two interaction modes, switched by an explicit toggle rather than
 * trying to disambiguate overlapping gestures automatically (a
 * deliberate simplicity choice — I can't compile-test this screen, so a
 * design where only one drag-gesture-detector is ever active at a time is
 * safer than one that infers intent from gesture shape):
 * - **Explore** (default): pinch/drag pans and zooms; tap selects a node
 *   and highlights it plus its directly-connected neighbors; a second,
 *   explicit "Open Node" action navigates.
 * - **Select** (toggled on): drag draws a selection rectangle; every node
 *   whose drawn position falls inside it becomes selected (multi-select),
 *   highlighting all of them plus their combined neighbors.
 *
 * Layer ordering: nodes are drawn after edges, and *highlighted* nodes
 * are drawn last of all, so a selected node's highlight is never
 * visually covered by a dimmed, unrelated neighbor drawn on top of it.
 */
@Composable
fun GraphScreen(graph: GraphModel, onSelectNode: (String) -> Unit, onBack: () -> Unit) {
    val textMeasurer = rememberTextMeasurer()
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var selectedNodeIds by remember { mutableStateOf(setOf<String>()) }
    var typeFilter by remember { mutableStateOf<String?>(null) }
    var boxSelectMode by remember { mutableStateOf(false) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCurrent by remember { mutableStateOf<Offset?>(null) }

    val availableTypes = remember(graph) { graph.nodes.map { it.type }.distinct().sorted() }
    val visibleNodes = if (typeFilter == null) graph.nodes else graph.nodes.filter { it.type == typeFilter }
    val visibleIds = visibleNodes.map { it.id }.toSet()
    val visibleEdges = graph.edges.filter { it.sourceId in visibleIds && it.targetId in visibleIds }

    val highlightedIds: Set<String> = remember(selectedNodeIds, visibleEdges) {
        if (selectedNodeIds.isEmpty()) {
            emptySet()
        } else {
            val neighbors = visibleEdges.filter { it.sourceId in selectedNodeIds || it.targetId in selectedNodeIds }
                .flatMap { listOf(it.sourceId, it.targetId) }
            (neighbors + selectedNodeIds).toSet()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("← Back to Workspace") }
            if (selectedNodeIds.size == 1) {
                Button(onClick = { onSelectNode(selectedNodeIds.first()) }) { Text("Open Node") }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Graph View", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = {
                boxSelectMode = !boxSelectMode
                dragStart = null
                dragCurrent = null
            }) {
                Text(if (boxSelectMode) "✓ Select Mode" else "Select Mode")
            }
        }
        Spacer(Modifier.height(8.dp))

        if (availableTypes.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(selected = typeFilter == null, onClick = { typeFilter = null }, label = { Text("All") })
                }
                items(availableTypes) { t ->
                    FilterChip(selected = typeFilter == t, onClick = { typeFilter = if (typeFilter == t) null else t }, label = { Text(t) })
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (selectedNodeIds.size > 1) {
            Text("${selectedNodeIds.size} nodes selected", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
        }

        if (graph.nodes.isEmpty()) {
            Text("Add some Nodes and Relationships to see the graph.", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }

        val exploreModifier = Modifier
            .fillMaxSize()
            .pointerInput(boxSelectMode) {
                if (!boxSelectMode) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 4f)
                        offset += pan
                    }
                }
            }
            .pointerInput(visibleNodes, scale, offset, boxSelectMode) {
                if (!boxSelectMode) {
                    detectTapGestures { tapOffset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val nearest = visibleNodes.minByOrNull { node ->
                            val nx = node.x * w * scale + offset.x
                            val ny = node.y * h * scale + offset.y
                            hypot((nx - tapOffset.x).toDouble(), (ny - tapOffset.y).toDouble())
                        }
                        selectedNodeIds = if (nearest != null) {
                            val nx = nearest.x * w * scale + offset.x
                            val ny = nearest.y * h * scale + offset.y
                            val distance = hypot((nx - tapOffset.x).toDouble(), (ny - tapOffset.y).toDouble())
                            if (distance < 60.0 * scale) {
                                if (selectedNodeIds == setOf(nearest.id)) emptySet() else setOf(nearest.id)
                            } else {
                                emptySet()
                            }
                        } else {
                            emptySet()
                        }
                    }
                }
            }
            .pointerInput(visibleNodes, scale, offset, boxSelectMode) {
                if (boxSelectMode) {
                    detectDragGestures(
                        onDragStart = { start -> dragStart = start; dragCurrent = start },
                        onDrag = { change, _ -> dragCurrent = change.position },
                        onDragEnd = {
                            val start = dragStart
                            val end = dragCurrent
                            if (start != null && end != null) {
                                val rect = Rect(
                                    left = min(start.x, end.x), top = min(start.y, end.y),
                                    right = max(start.x, end.x), bottom = max(start.y, end.y)
                                )
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                selectedNodeIds = visibleNodes.filter { node ->
                                    val nx = node.x * w * scale + offset.x
                                    val ny = node.y * h * scale + offset.y
                                    rect.contains(Offset(nx, ny))
                                }.map { it.id }.toSet()
                            }
                            dragStart = null
                            dragCurrent = null
                        }
                    )
                }
            }

        Canvas(modifier = exploreModifier) {
            val w = size.width
            val h = size.height

            fun screenPos(nx: Float, ny: Float) = Offset(nx * w * scale + offset.x, ny * h * scale + offset.y)

            visibleEdges.forEach { edge ->
                val source = visibleNodes.find { it.id == edge.sourceId }
                val target = visibleNodes.find { it.id == edge.targetId }
                if (source != null && target != null) {
                    val isHighlighted = edge.sourceId in selectedNodeIds || edge.targetId in selectedNodeIds
                    drawLine(
                        color = if (isHighlighted) Accent else Border,
                        start = screenPos(source.x, source.y),
                        end = screenPos(target.x, target.y),
                        strokeWidth = if (isHighlighted) 3f else 2f
                    )
                }
            }

            // Layer ordering: dimmed (non-highlighted) nodes first, highlighted/selected nodes last,
            // so a selection's highlight is never drawn over by an unrelated node.
            val dimmed = visibleNodes.filter { selectedNodeIds.isNotEmpty() && it.id !in highlightedIds }
            val emphasized = visibleNodes.filter { selectedNodeIds.isEmpty() || it.id in highlightedIds }

            (dimmed + emphasized).forEach { node ->
                val center = screenPos(node.x, node.y)
                val isDimmed = node in dimmed
                val isSelected = node.id in selectedNodeIds
                val nodeColor = if (isDimmed) Ink400 else Accent
                val radius = (if (isSelected) 28f else 24f) * scale

                drawCircle(color = nodeColor, radius = radius, center = center)
                drawCircle(color = Color.White, radius = radius, center = center, style = Stroke(width = 2f))

                val textLayout = textMeasurer.measure(
                    node.label,
                    style = TextStyle(fontSize = (12 * scale).sp, color = if (isDimmed) Ink400 else Ink900)
                )
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(center.x - textLayout.size.width / 2f, center.y + radius + 4f)
                )
            }

            val start = dragStart
            val current = dragCurrent
            if (boxSelectMode && start != null && current != null) {
                val rect = Rect(
                    left = min(start.x, current.x), top = min(start.y, current.y),
                    right = max(start.x, current.x), bottom = max(start.y, current.y)
                )
                drawRect(color = Accent.copy(alpha = 0.15f), topLeft = rect.topLeft, size = rect.size)
                drawRect(color = Accent, topLeft = rect.topLeft, size = rect.size, style = Stroke(width = 2f))
            }
        }
    }
}
