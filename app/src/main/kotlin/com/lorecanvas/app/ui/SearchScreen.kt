package com.lorecanvas.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lorecanvas.search.SearchResult
import com.lorecanvas.search.SearchResultKind

/** Search — LCD-009 Ch.14: "User Types -> Search Index -> Rank Results -> Display Results -> Open Selection." */
@Composable
fun SearchScreen(
    query: String,
    results: List<SearchResult>,
    activeFilters: Set<SearchResultKind>,
    onQueryChange: (String) -> Unit,
    onToggleFilter: (SearchResultKind) -> Unit,
    onSelectResult: (SearchResult) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        TextButton(onClick = onBack) { Text("← Back to Workspace") }
        Spacer(Modifier.height(16.dp))
        Text("Search", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Search this project") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        LazyRow {
            items(SearchResultKind.entries.toList()) { kind ->
                FilterChip(
                    selected = kind in activeFilters,
                    onClick = { onToggleFilter(kind) },
                    label = { Text(kind.label()) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        if (query.isBlank()) {
            Text("Start typing to search Nodes, Cards, Relationships, and Timeline events.", style = MaterialTheme.typography.bodyMedium)
        } else if (results.isEmpty()) {
            Text("No matches for \"$query\".", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn {
                items(results) { result ->
                    SearchResultRow(result, onClick = { onSelectResult(result) })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

private fun SearchResultKind.label(): String = when (this) {
    SearchResultKind.NODE -> "Nodes"
    SearchResultKind.CARD -> "Cards"
    SearchResultKind.RELATIONSHIP -> "Relationships"
    SearchResultKind.TIMELINE_EVENT -> "Timeline"
    SearchResultKind.TEMPLATE -> "Templates"
}

@Composable
private fun SearchResultRow(result: SearchResult, onClick: () -> Unit) {
    val (kind, title, subtitle) = when (result) {
        is SearchResult.NodeResult -> Triple("Node", result.node.name, result.node.type)
        is SearchResult.CardResult -> Triple("Card", result.card.title, result.card.type)
        is SearchResult.RelationshipResult -> Triple("Relationship", result.relationship.type, result.relationship.description)
        is SearchResult.TimelineEventResult -> Triple("Timeline Event", result.event.title, result.event.date)
        is SearchResult.TemplateResult -> Triple("Template", result.template.name, "For: ${result.template.targetNodeType}")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(kind, style = MaterialTheme.typography.labelSmall)
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onClick) { Text("Open") }
        }
    }
}
