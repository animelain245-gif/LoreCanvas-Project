package com.lorecanvas.plugin

/** Live counts, updated purely by observing events — never touches Storage/Repository directly. */
data class ProjectStatistics(
    val nodesCreated: Int = 0,
    val nodesDeleted: Int = 0,
    val cardsCreated: Int = 0,
    val cardsDeleted: Int = 0,
    val relationshipsCreated: Int = 0,
    val relationshipsDeleted: Int = 0,
    val timelinesCreated: Int = 0
)

/**
 * A real, working example plugin — proof that the extension points in
 * [LoreCanvasPlugin]/[PluginContext] actually function end-to-end, not
 * just that the interfaces compile. Counts session activity by listening
 * to the same events Search and the UI already react to.
 */
class StatisticsPlugin : LoreCanvasPlugin {
    override val id: String = "builtin.statistics"
    override val name: String = "Project Statistics"

    @Volatile private var stats = ProjectStatistics()

    override fun onLoad(context: PluginContext) {
        context.eventBus.subscribeAny("NodeCreated") { stats = stats.copy(nodesCreated = stats.nodesCreated + 1) }
        context.eventBus.subscribeAny("NodeDeleted") { stats = stats.copy(nodesDeleted = stats.nodesDeleted + 1) }
        context.eventBus.subscribeAny("CardCreated") { stats = stats.copy(cardsCreated = stats.cardsCreated + 1) }
        context.eventBus.subscribeAny("CardDeleted") { stats = stats.copy(cardsDeleted = stats.cardsDeleted + 1) }
        context.eventBus.subscribeAny("RelationshipCreated") { stats = stats.copy(relationshipsCreated = stats.relationshipsCreated + 1) }
        context.eventBus.subscribeAny("RelationshipDeleted") { stats = stats.copy(relationshipsDeleted = stats.relationshipsDeleted + 1) }
        context.eventBus.subscribeAny("TimelineCreated") { stats = stats.copy(timelinesCreated = stats.timelinesCreated + 1) }
        context.logger.info("Statistics plugin loaded")
    }

    override fun onUnload() {
        stats = ProjectStatistics()
    }

    fun snapshot(): ProjectStatistics = stats
}
