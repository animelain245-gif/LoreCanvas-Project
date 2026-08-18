package com.lorecanvas.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lorecanvas.common.LcResult
import com.lorecanvas.domain.Card
import com.lorecanvas.domain.Node
import com.lorecanvas.domain.NodeStatus
import com.lorecanvas.domain.Project
import com.lorecanvas.domain.Relationship
import com.lorecanvas.domain.Template
import com.lorecanvas.domain.Timeline
import com.lorecanvas.domain.TimelineEvent
import com.lorecanvas.events.EventBus
import com.lorecanvas.plugin.PluginRegistry
import com.lorecanvas.plugin.StatisticsPlugin
import com.lorecanvas.repository.CardRepository
import com.lorecanvas.repository.ImportExportRepository
import com.lorecanvas.repository.NodeRepository
import com.lorecanvas.repository.ProjectRepository
import com.lorecanvas.repository.RelationshipRepository
import com.lorecanvas.repository.TemplateRepository
import com.lorecanvas.repository.TimelineRepository
import com.lorecanvas.search.SearchIndexCache
import com.lorecanvas.search.SearchResult
import com.lorecanvas.storage.CardFileStorage
import com.lorecanvas.storage.NodeFileStorage
import com.lorecanvas.storage.ProjectFileStorage
import com.lorecanvas.storage.ProjectSummary
import com.lorecanvas.storage.RelationshipFileStorage
import com.lorecanvas.storage.TemplateFileStorage
import com.lorecanvas.storage.TimelineFileStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** All repositories (+ search cache + plugins) scoped to one open Project — created together, discarded together on close. */
private class WorkspaceContext(
    val nodeRepository: NodeRepository,
    val cardRepository: CardRepository,
    val relationshipRepository: RelationshipRepository,
    val timelineRepository: TimelineRepository,
    val templateRepository: TemplateRepository,
    val importExportRepository: ImportExportRepository,
    val searchCache: SearchIndexCache,
    val graphCache: com.lorecanvas.graph.GraphCache,
    val commandHistory: com.lorecanvas.commands.CommandHistory,
    val pluginRegistry: PluginRegistry,
    val statisticsPlugin: StatisticsPlugin
)

private sealed class UiState {
    object Loading : UiState()

    data class ProjectList(val projects: List<ProjectSummary>, val error: String? = null) : UiState()

    data class Workspace(
        val project: Project,
        val ctx: WorkspaceContext,
        val nodes: List<Node> = emptyList(),
        val isDirty: Boolean = false,
        val status: String? = null,
        val isError: Boolean = false
    ) : UiState()

    data class NodeEditor(
        val project: Project,
        val ctx: WorkspaceContext,
        val node: Node,
        val cards: List<Card> = emptyList(),
        val relationships: List<Relationship> = emptyList(),
        val allNodes: List<Node> = emptyList(),
        val isDirty: Boolean = false,
        val status: String? = null,
        val isError: Boolean = false
    ) : UiState()

    data class RelationshipEditor(
        val project: Project,
        val ctx: WorkspaceContext,
        val node: Node,
        val relationship: Relationship,
        val allNodes: List<Node>,
        val status: String? = null,
        val isError: Boolean = false
    ) : UiState()

    data class CardEditor(
        val project: Project,
        val ctx: WorkspaceContext,
        val node: Node,
        val card: Card,
        val isDirty: Boolean = false,
        val status: String? = null,
        val isError: Boolean = false
    ) : UiState()

    data class TimelineList(
        val project: Project,
        val ctx: WorkspaceContext,
        val timelines: List<Timeline> = emptyList()
    ) : UiState()

    data class TimelineEditor(
        val project: Project,
        val ctx: WorkspaceContext,
        val timeline: Timeline,
        val allNodes: List<Node> = emptyList(),
        val status: String? = null,
        val isError: Boolean = false
    ) : UiState()

    data class Search(
        val project: Project,
        val ctx: WorkspaceContext,
        val query: String = "",
        val results: List<SearchResult> = emptyList(),
        val activeFilters: Set<com.lorecanvas.search.SearchResultKind> = com.lorecanvas.search.SearchResultKind.entries.toSet()
    ) : UiState()

    data class Graph(
        val project: Project,
        val ctx: WorkspaceContext,
        val graph: com.lorecanvas.graph.GraphModel
    ) : UiState()

    data class TemplateListState(
        val project: Project,
        val ctx: WorkspaceContext,
        val templates: List<Template> = emptyList(),
        val status: String? = null,
        val isError: Boolean = false
    ) : UiState()

    data class ImportPicker(
        val project: Project,
        val ctx: WorkspaceContext,
        val exportFiles: List<File>,
        val status: String? = null,
        val isError: Boolean = false
    ) : UiState()
}

private fun sanitizeDirectoryName(name: String): String {
    val cleaned = name.trim().replace(Regex("[/\\\\:*?\"<>|]"), "_")
    return cleaned.ifBlank { "Untitled Project" }
}

/**
 * Root composable. Phases 4-8 add five more screens (CardEditor,
 * TimelineList, TimelineEditor, Search, Graph) to Phase 3's three-screen
 * state machine. [WorkspaceContext] bundles the four scoped repositories
 * (Node/Card/Relationship/Timeline) that all come into existence together
 * when a project opens and are discarded together when it closes.
 */
@Composable
fun LoreCanvasApp(projectsRootDirectory: File, exportsRootDirectory: File) {
    val repository = remember { ProjectRepository(projectStorage = ProjectFileStorage(), eventBus = EventBus()) }
    var uiState by remember { mutableStateOf<UiState>(UiState.Loading) }
    val scope = rememberCoroutineScope()

    fun refreshProjectList(error: String? = null) {
        scope.launch {
            val projects = withContext(Dispatchers.IO) { repository.listProjects(projectsRootDirectory) }
            uiState = UiState.ProjectList(projects, error)
        }
    }

    fun enterWorkspace(project: Project) {
        scope.launch {
            val dir = requireNotNull(repository.currentProjectDirectory) { "No open project directory." }
            val eventBus = EventBus()
            val nodeFileStorage = NodeFileStorage()
            val cardFileStorage = CardFileStorage()
            val relationshipFileStorage = RelationshipFileStorage()
            val timelineFileStorage = TimelineFileStorage()
            val templateFileStorage = TemplateFileStorage()

            val nodeRepository = NodeRepository(
                dir, nodeFileStorage, eventBus,
                cardStorage = cardFileStorage,
                relationshipStorage = relationshipFileStorage,
                timelineStorage = timelineFileStorage
            )
            val cardRepository = CardRepository(dir, cardFileStorage, nodeFileStorage, eventBus)
            val relationshipRepository = RelationshipRepository(dir, relationshipFileStorage, nodeFileStorage, eventBus)
            val timelineRepository = TimelineRepository(dir, timelineFileStorage, eventBus)
            val templateRepository = TemplateRepository(dir, templateFileStorage, nodeRepository, cardRepository, eventBus)
            val importExportRepository = ImportExportRepository(dir, nodeFileStorage, cardFileStorage, relationshipFileStorage, timelineFileStorage)
            val searchCache = withContext(Dispatchers.IO) {
                SearchIndexCache(nodeRepository, cardRepository, relationshipRepository, timelineRepository, templateRepository, eventBus)
            }
            val graphCache = withContext(Dispatchers.IO) {
                com.lorecanvas.graph.GraphCache(nodeRepository, relationshipRepository, eventBus)
            }
            val commandHistory = com.lorecanvas.commands.CommandHistory()
            val pluginRegistry = PluginRegistry(eventBus)
            val statisticsPlugin = StatisticsPlugin()
            pluginRegistry.load(statisticsPlugin)

            val ctx = WorkspaceContext(
                nodeRepository, cardRepository, relationshipRepository, timelineRepository,
                templateRepository, importExportRepository, searchCache, graphCache, commandHistory,
                pluginRegistry, statisticsPlugin
            )
            val nodes = withContext(Dispatchers.IO) { ctx.nodeRepository.list() }
            uiState = UiState.Workspace(project = project, ctx = ctx, nodes = nodes)
        }
    }

    fun enterNodeEditor(project: Project, ctx: WorkspaceContext, node: Node, allNodes: List<Node>) {
        scope.launch {
            val cards = withContext(Dispatchers.IO) { ctx.cardRepository.listForNode(node.id) }
            val relationships = withContext(Dispatchers.IO) { ctx.relationshipRepository.listForNode(node.id) }
            uiState = UiState.NodeEditor(project, ctx, node, cards, relationships, allNodes)
        }
    }

    remember {
        refreshProjectList()
        true
    }

    when (val state = uiState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }

        is UiState.ProjectList -> {
            ProjectListScreen(
                projects = state.projects,
                errorMessage = state.error,
                onCreateProject = { name ->
                    scope.launch {
                        val directory = File(projectsRootDirectory, sanitizeDirectoryName(name))
                        val result = withContext(Dispatchers.IO) { repository.create(directory, name) }
                        when (result) {
                            is LcResult.Ok -> enterWorkspace(result.value)
                            is LcResult.Fail -> uiState = UiState.ProjectList(state.projects, result.error.userMessage())
                        }
                    }
                },
                onOpenProject = { summary ->
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { repository.open(summary.directory) }
                        when (result) {
                            is LcResult.Ok -> enterWorkspace(result.value)
                            is LcResult.Fail -> uiState = UiState.ProjectList(state.projects, result.error.userMessage())
                        }
                    }
                }
            )
        }

        is UiState.Workspace -> {
            WorkspaceScreen(
                project = state.project,
                nodes = state.nodes,
                isDirty = state.isDirty,
                statusMessage = state.status,
                isError = state.isError,
                onRename = { newName ->
                    if (newName.isNotBlank()) {
                        state.project.rename(newName)
                        uiState = state.copy(isDirty = true, status = null)
                    }
                },
                onAddTag = { tag ->
                    state.project.addTag(tag)
                    uiState = state.copy(isDirty = true, status = null)
                },
                onSave = {
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { repository.save() }
                        uiState = when (result) {
                            is LcResult.Ok -> state.copy(isDirty = false, status = "Saved.", isError = false)
                            is LcResult.Fail -> state.copy(status = result.error.userMessage(), isError = true)
                        }
                    }
                },
                onClose = {
                    scope.launch {
                        withContext(Dispatchers.IO) { repository.close() }
                        refreshProjectList()
                    }
                },
                onCreateNode = { name, type, summary ->
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            state.ctx.nodeRepository.create(name = name, type = type, summary = summary)
                        }
                        uiState = when (result) {
                            is LcResult.Ok -> {
                                val nodes = withContext(Dispatchers.IO) { state.ctx.nodeRepository.list() }
                                state.copy(nodes = nodes)
                            }
                            is LcResult.Fail -> state.copy(status = result.error.userMessage(), isError = true)
                        }
                    }
                },
                onOpenNode = { node -> enterNodeEditor(state.project, state.ctx, node, state.nodes) },
                onOpenTimelines = {
                    scope.launch {
                        val timelines = withContext(Dispatchers.IO) { state.ctx.timelineRepository.list() }
                        uiState = UiState.TimelineList(state.project, state.ctx, timelines)
                    }
                },
                onOpenSearch = { uiState = UiState.Search(state.project, state.ctx) },
                onOpenGraph = {
                    scope.launch {
                        val graph = withContext(Dispatchers.IO) { state.ctx.graphCache.current() }
                        uiState = UiState.Graph(state.project, state.ctx, graph)
                    }
                },
                onOpenTemplates = {
                    scope.launch {
                        val templates = withContext(Dispatchers.IO) { state.ctx.templateRepository.list() }
                        uiState = UiState.TemplateListState(state.project, state.ctx, templates)
                    }
                },
                onExportProject = {
                    scope.launch {
                        val outputFile = File(exportsRootDirectory, "${state.project.name.replace(Regex("[^A-Za-z0-9]+"), "_")}-${System.currentTimeMillis()}.json")
                        val result = withContext(Dispatchers.IO) { state.ctx.importExportRepository.exportAll(outputFile) }
                        uiState = when (result) {
                            is LcResult.Ok -> state.copy(status = "Exported to ${outputFile.name}", isError = false)
                            is LcResult.Fail -> state.copy(status = result.error.userMessage(), isError = true)
                        }
                    }
                },
                onImportProject = {
                    scope.launch {
                        val files = withContext(Dispatchers.IO) {
                            exportsRootDirectory.listFiles { f -> f.isFile && f.name.endsWith(".json") }?.toList() ?: emptyList()
                        }
                        uiState = UiState.ImportPicker(state.project, state.ctx, files)
                    }
                }
            )
        }

        is UiState.NodeEditor -> {
            NodeEditorScreen(
                node = state.node,
                cards = state.cards,
                relationships = state.relationships,
                otherNodes = state.allNodes.filter { it.id != state.node.id },
                isDirty = state.isDirty,
                statusMessage = state.status,
                isError = state.isError,
                onRename = { newName ->
                    if (newName.isNotBlank()) {
                        state.node.rename(newName)
                        uiState = state.copy(isDirty = true, status = null)
                    }
                },
                onChangeType = { newType ->
                    if (newType.isNotBlank()) {
                        state.node.changeType(newType)
                        uiState = state.copy(isDirty = true, status = null)
                    }
                },
                onUpdateSummary = { newSummary ->
                    state.node.updateSummary(newSummary)
                    uiState = state.copy(isDirty = true, status = null)
                },
                onAddTag = { tag ->
                    state.node.addTag(tag)
                    uiState = state.copy(isDirty = true, status = null)
                },
                onArchiveToggle = {
                    if (state.node.status == NodeStatus.ARCHIVED) state.node.restore() else state.node.archive()
                    uiState = state.copy(isDirty = true, status = null)
                },
                onSave = {
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { state.ctx.nodeRepository.save(state.node) }
                        uiState = when (result) {
                            is LcResult.Ok -> state.copy(isDirty = false, status = "Saved.", isError = false)
                            is LcResult.Fail -> state.copy(status = result.error.userMessage(), isError = true)
                        }
                    }
                },
                onDelete = {
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { state.ctx.nodeRepository.delete(state.node.id) }
                        when (result) {
                            is LcResult.Ok -> {
                                val nodes = withContext(Dispatchers.IO) { state.ctx.nodeRepository.list() }
                                uiState = UiState.Workspace(state.project, state.ctx, nodes)
                            }
                            is LcResult.Fail -> uiState = state.copy(status = result.error.userMessage(), isError = true)
                        }
                    }
                },
                onBack = {
                    scope.launch {
                        val nodes = withContext(Dispatchers.IO) { state.ctx.nodeRepository.list() }
                        uiState = UiState.Workspace(state.project, state.ctx, nodes)
                    }
                },
                onCreateCard = { title, type ->
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { state.ctx.cardRepository.create(state.node.id, title, type) }
                        uiState = when (result) {
                            is LcResult.Ok -> {
                                val cards = withContext(Dispatchers.IO) { state.ctx.cardRepository.listForNode(state.node.id) }
                                state.copy(cards = cards)
                            }
                            is LcResult.Fail -> state.copy(status = result.error.userMessage(), isError = true)
                        }
                    }
                },
                onOpenCard = { card -> uiState = UiState.CardEditor(state.project, state.ctx, state.node, card) },
                onCreateRelationship = { targetId, type, description ->
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            state.ctx.relationshipRepository.create(state.node.id, targetId, type, description = description)
                        }
                        uiState = when (result) {
                            is LcResult.Ok -> {
                                val rels = withContext(Dispatchers.IO) { state.ctx.relationshipRepository.listForNode(state.node.id) }
                                state.copy(relationships = rels)
                            }
                            is LcResult.Fail -> state.copy(status = result.error.userMessage(), isError = true)
                        }
                    }
                },
                onDeleteRelationship = { relId ->
                    scope.launch {
                        withContext(Dispatchers.IO) { state.ctx.relationshipRepository.delete(relId) }
                        val rels = withContext(Dispatchers.IO) { state.ctx.relationshipRepository.listForNode(state.node.id) }
                        uiState = state.copy(relationships = rels)
                    }
                },
                onOpenRelationship = { rel ->
                    uiState = UiState.RelationshipEditor(state.project, state.ctx, state.node, rel, state.allNodes)
                },
                onSaveAsTemplate = { templateName ->
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            state.ctx.templateRepository.createFromNode(templateName, state.node)
                        }
                        uiState = when (result) {
                            is LcResult.Ok -> state.copy(status = "Saved as template \"$templateName\".", isError = false)
                            is LcResult.Fail -> state.copy(status = result.error.userMessage(), isError = true)
                        }
                    }
                }
            )
        }

        is UiState.CardEditor -> {
            CardEditorScreen(
                card = state.card,
                isDirty = state.isDirty,
                statusMessage = state.status,
                isError = state.isError,
                onRename = { state.card.rename(it); uiState = state.copy(isDirty = true, status = null) },
                onChangeType = { state.card.changeType(it); uiState = state.copy(isDirty = true, status = null) },
                onUpdateContent = { state.card.updateContent(it); uiState = state.copy(isDirty = true, status = null) },
                onAddTag = { state.card.addTag(it); uiState = state.copy(isDirty = true, status = null) },
                onSave = {
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { state.ctx.cardRepository.save(state.card) }
                        uiState = when (result) {
                            is LcResult.Ok -> state.copy(isDirty = false, status = "Saved.", isError = false)
                            is LcResult.Fail -> state.copy(status = result.error.userMessage(), isError = true)
                        }
                    }
                },
                onDelete = {
                    scope.launch {
                        withContext(Dispatchers.IO) { state.ctx.cardRepository.delete(state.card.id) }
                        val cards = withContext(Dispatchers.IO) { state.ctx.cardRepository.listForNode(state.node.id) }
                        val rels = withContext(Dispatchers.IO) { state.ctx.relationshipRepository.listForNode(state.node.id) }
                        val allNodes = withContext(Dispatchers.IO) { state.ctx.nodeRepository.list() }
                        uiState = UiState.NodeEditor(state.project, state.ctx, state.node, cards, rels, allNodes)
                    }
                },
                onBack = {
                    scope.launch {
                        val cards = withContext(Dispatchers.IO) { state.ctx.cardRepository.listForNode(state.node.id) }
                        val rels = withContext(Dispatchers.IO) { state.ctx.relationshipRepository.listForNode(state.node.id) }
                        val allNodes = withContext(Dispatchers.IO) { state.ctx.nodeRepository.list() }
                        uiState = UiState.NodeEditor(state.project, state.ctx, state.node, cards, rels, allNodes)
                    }
                }
            )
        }

        is UiState.TimelineList -> {
            TimelineListScreen(
                timelines = state.timelines,
                onBack = {
                    scope.launch {
                        val nodes = withContext(Dispatchers.IO) { state.ctx.nodeRepository.list() }
                        uiState = UiState.Workspace(state.project, state.ctx, nodes)
                    }
                },
                onCreateTimeline = { name ->
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { state.ctx.timelineRepository.create(name) }
                        uiState = when (result) {
                            is LcResult.Ok -> {
                                val timelines = withContext(Dispatchers.IO) { state.ctx.timelineRepository.list() }
                                state.copy(timelines = timelines)
                            }
                            is LcResult.Fail -> state // silently ignore for now — name is guaranteed non-blank by the dialog
                        }
                    }
                },
                onOpenTimeline = { timeline ->
                    scope.launch {
                        val allNodes = withContext(Dispatchers.IO) { state.ctx.nodeRepository.list() }
                        uiState = UiState.TimelineEditor(state.project, state.ctx, timeline, allNodes)
                    }
                }
            )
        }

        is UiState.TimelineEditor -> {
            TimelineEditorScreen(
                timeline = state.timeline,
                sortedEvents = state.timeline.sortedEvents(),
                allNodes = state.allNodes,
                statusMessage = state.status,
                isError = state.isError,
                onRename = { newName ->
                    if (newName.isNotBlank()) {
                        state.timeline.rename(newName)
                        uiState = state.copy(status = null)
                    }
                },
                onAddEvent = { date, title, description ->
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            state.ctx.timelineRepository.addEvent(state.timeline, date, title, description)
                        }
                        uiState = when (result) {
                            is LcResult.Ok -> state.copy(status = null)
                            is LcResult.Fail -> state.copy(status = result.error.userMessage(), isError = true)
                        }
                    }
                },
                onEditEvent = { updated ->
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            state.ctx.timelineRepository.updateEvent(state.timeline, updated)
                        }
                        uiState = when (result) {
                            is LcResult.Ok -> state.copy(status = null)
                            is LcResult.Fail -> state.copy(status = result.error.userMessage(), isError = true)
                        }
                    }
                },
                onRemoveEvent = { eventId ->
                    scope.launch {
                        withContext(Dispatchers.IO) { state.ctx.timelineRepository.removeEvent(state.timeline, eventId) }
                        uiState = state.copy(status = null)
                    }
                },
                onBack = {
                    scope.launch {
                        val timelines = withContext(Dispatchers.IO) { state.ctx.timelineRepository.list() }
                        uiState = UiState.TimelineList(state.project, state.ctx, timelines)
                    }
                }
            )
        }

        is UiState.Search -> {
            SearchScreen(
                query = state.query,
                results = state.results,
                activeFilters = state.activeFilters,
                onQueryChange = { newQuery ->
                    scope.launch {
                        val results = withContext(Dispatchers.IO) { state.ctx.searchCache.search(newQuery, state.activeFilters) }
                        uiState = state.copy(query = newQuery, results = results)
                    }
                },
                onToggleFilter = { kind ->
                    val newFilters = if (kind in state.activeFilters) state.activeFilters - kind else state.activeFilters + kind
                    scope.launch {
                        val results = withContext(Dispatchers.IO) { state.ctx.searchCache.search(state.query, newFilters) }
                        uiState = state.copy(activeFilters = newFilters, results = results)
                    }
                },
                onSelectResult = { result ->
                    scope.launch {
                        when (result) {
                            is SearchResult.NodeResult -> {
                                val allNodes = withContext(Dispatchers.IO) { state.ctx.nodeRepository.list() }
                                enterNodeEditor(state.project, state.ctx, result.node, allNodes)
                            }
                            is SearchResult.CardResult -> {
                                val node = withContext(Dispatchers.IO) { state.ctx.nodeRepository.get(result.card.parentNodeId) }
                                if (node != null) {
                                    uiState = UiState.CardEditor(state.project, state.ctx, node, result.card)
                                }
                            }
                            is SearchResult.RelationshipResult -> {
                                val node = withContext(Dispatchers.IO) { state.ctx.nodeRepository.get(result.relationship.sourceNodeId) }
                                val allNodes = withContext(Dispatchers.IO) { state.ctx.nodeRepository.list() }
                                if (node != null) enterNodeEditor(state.project, state.ctx, node, allNodes)
                            }
                            is SearchResult.TimelineEventResult -> {
                                val timeline = withContext(Dispatchers.IO) { state.ctx.timelineRepository.get(result.timelineId) }
                                val allNodes = withContext(Dispatchers.IO) { state.ctx.nodeRepository.list() }
                                if (timeline != null) uiState = UiState.TimelineEditor(state.project, state.ctx, timeline, allNodes)
                            }
                            is SearchResult.TemplateResult -> {
                                val templates = withContext(Dispatchers.IO) { state.ctx.templateRepository.list() }
                                uiState = UiState.TemplateListState(state.project, state.ctx, templates)
                            }
                        }
                    }
                },
                onBack = {
                    scope.launch {
                        val nodes = withContext(Dispatchers.IO) { state.ctx.nodeRepository.list() }
                        uiState = UiState.Workspace(state.project, state.ctx, nodes)
                    }
                }
            )
        }

        is UiState.Graph -> {
            GraphScreen(
                graph = state.graph,
                onSelectNode = { nodeId ->
                    scope.launch {
                        val allNodes = withContext(Dispatchers.IO) { state.ctx.nodeRepository.list() }
                        val node = allNodes.find { it.id == nodeId }
                        if (node != null) enterNodeEditor(state.project, state.ctx, node, allNodes)
                    }
                },
                onBack = {
                    scope.launch {
                        val nodes = withContext(Dispatchers.IO) { state.ctx.nodeRepository.list() }
                        uiState = UiState.Workspace(state.project, state.ctx, nodes)
                    }
                }
            )
        }

        is UiState.RelationshipEditor -> {
            val sourceLabel = state.allNodes.find { it.id == state.relationship.sourceNodeId }?.name ?: "(unknown)"
            val targetLabel = state.allNodes.find { it.id == state.relationship.targetNodeId }?.name ?: "(unknown)"
            RelationshipEditorScreen(
                relationship = state.relationship,
                sourceLabel = sourceLabel,
                targetLabel = targetLabel,
                statusMessage = state.status,
                isError = state.isError,
                onChangeType = { state.relationship.changeType(it); uiState = state.copy(status = null) },
                onUpdateDescription = { state.relationship.updateDescription(it); uiState = state.copy(status = null) },
                onAddContext = { startDate, description, endDate ->
                    state.relationship.addContext(
                        com.lorecanvas.domain.RelationshipContext.create(startDate = startDate, description = description, endDate = endDate)
                    )
                    uiState = state.copy(status = null)
                },
                onSave = {
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { state.ctx.relationshipRepository.save(state.relationship) }
                        uiState = when (result) {
                            is LcResult.Ok -> state.copy(status = "Saved.", isError = false)
                            is LcResult.Fail -> state.copy(status = result.error.userMessage(), isError = true)
                        }
                    }
                },
                onDelete = {
                    scope.launch {
                        withContext(Dispatchers.IO) { state.ctx.relationshipRepository.delete(state.relationship.id) }
                        val cards = withContext(Dispatchers.IO) { state.ctx.cardRepository.listForNode(state.node.id) }
                        val rels = withContext(Dispatchers.IO) { state.ctx.relationshipRepository.listForNode(state.node.id) }
                        uiState = UiState.NodeEditor(state.project, state.ctx, state.node, cards, rels, state.allNodes)
                    }
                },
                onBack = {
                    scope.launch {
                        val cards = withContext(Dispatchers.IO) { state.ctx.cardRepository.listForNode(state.node.id) }
                        val rels = withContext(Dispatchers.IO) { state.ctx.relationshipRepository.listForNode(state.node.id) }
                        uiState = UiState.NodeEditor(state.project, state.ctx, state.node, cards, rels, state.allNodes)
                    }
                }
            )
        }

        is UiState.TemplateListState -> {
            TemplateListScreen(
                templates = state.templates,
                statusMessage = state.status,
                isError = state.isError,
                onBack = {
                    scope.launch {
                        val nodes = withContext(Dispatchers.IO) { state.ctx.nodeRepository.list() }
                        uiState = UiState.Workspace(state.project, state.ctx, nodes)
                    }
                },
                onApplyTemplate = { template, nodeName ->
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { state.ctx.templateRepository.apply(template, nodeName) }
                        when (result) {
                            is LcResult.Ok -> {
                                val nodes = withContext(Dispatchers.IO) { state.ctx.nodeRepository.list() }
                                enterNodeEditor(state.project, state.ctx, result.value, nodes)
                            }
                            is LcResult.Fail -> uiState = state.copy(status = result.error.userMessage(), isError = true)
                        }
                    }
                },
                onDeleteTemplate = { templateId ->
                    scope.launch {
                        withContext(Dispatchers.IO) { state.ctx.templateRepository.delete(templateId) }
                        val templates = withContext(Dispatchers.IO) { state.ctx.templateRepository.list() }
                        uiState = state.copy(templates = templates)
                    }
                }
            )
        }

        is UiState.ImportPicker -> {
            ImportPickerScreen(
                exportFiles = state.exportFiles,
                statusMessage = state.status,
                isError = state.isError,
                onSelectFile = { file ->
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { state.ctx.importExportRepository.import(file) }
                        when (result) {
                            is LcResult.Ok -> {
                                val nodes = withContext(Dispatchers.IO) { state.ctx.nodeRepository.list() }
                                uiState = UiState.Workspace(
                                    state.project, state.ctx, nodes,
                                    status = "Imported ${result.value.nodesImported} node(s).", isError = false
                                )
                            }
                            is LcResult.Fail -> uiState = state.copy(status = result.error.userMessage(), isError = true)
                        }
                    }
                },
                onBack = {
                    scope.launch {
                        val nodes = withContext(Dispatchers.IO) { state.ctx.nodeRepository.list() }
                        uiState = UiState.Workspace(state.project, state.ctx, nodes)
                    }
                }
            )
        }
    }
}
