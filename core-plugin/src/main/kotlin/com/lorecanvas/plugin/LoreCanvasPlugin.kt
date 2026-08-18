package com.lorecanvas.plugin

import com.lorecanvas.common.Logger
import com.lorecanvas.common.createLogger
import com.lorecanvas.events.DomainEvent
import com.lorecanvas.events.EventBus

/**
 * Plugin & Extension Architecture (LCD-014).
 *
 * Scope, stated plainly: this is the *extension-point infrastructure* —
 * a stable interface plugins implement, a context that gives them
 * (read-only, event-driven) access to what happens in a project, and a
 * registry that manages their lifecycle. What this deliberately does
 * **not** do is dynamically load arbitrary third-party code from external
 * files at runtime (e.g. loading a separate .jar/.dex a user downloaded).
 * That's a substantially larger undertaking with real security
 * implications (arbitrary code execution inside the app's process) and
 * Android-specific constraints (Play Store restrictions on dynamic code
 * loading) — a genuinely different, much bigger feature than "plugin
 * infrastructure exists and works." What's here is real and functional
 * for in-process, compiled-in extensions (see [StatisticsPlugin] for a
 * working example); a dynamic third-party loading system would need its
 * own dedicated design pass, not a few extra classes bolted on here.
 */
interface LoreCanvasPlugin {
    val id: String
    val name: String

    fun onLoad(context: PluginContext)
    fun onUnload()
}

/**
 * What a plugin can see: the same event stream every other subsystem
 * (Search, the UI) reacts to, and a logger scoped to the plugin's own id.
 * No direct Repository/Storage access — a plugin observes what already
 * happened via events, rather than being able to silently mutate project
 * data behind the Repository's back (LCD-004's "UI must never access
 * Storage directly" applies here in spirit too: plugins aren't UI, but
 * they're just as much an outside consumer of the system as the UI is).
 */
class PluginContext(val eventBus: EventBus, val logger: Logger)

/**
 * Manages plugin lifecycle: load, unload, lookup. Deliberately simple —
 * an ordered list, not a dependency graph — since nothing yet requires
 * plugins to depend on each other.
 */
class PluginRegistry(private val eventBus: EventBus) {
    private val loaded = mutableMapOf<String, LoreCanvasPlugin>()

    fun load(plugin: LoreCanvasPlugin) {
        if (loaded.containsKey(plugin.id)) return
        val context = PluginContext(eventBus, createLogger("Plugin:${plugin.id}"))
        plugin.onLoad(context)
        loaded[plugin.id] = plugin
    }

    fun unload(pluginId: String) {
        loaded.remove(pluginId)?.onUnload()
    }

    fun unloadAll() {
        loaded.keys.toList().forEach { unload(it) }
    }

    fun isLoaded(pluginId: String): Boolean = loaded.containsKey(pluginId)

    fun loadedPlugins(): List<LoreCanvasPlugin> = loaded.values.toList()
}

/** Convenience for plugins that don't care about event payload fields, only that something of a given type happened. */
internal fun EventBus.subscribeAny(eventType: String, onAny: () -> Unit) {
    subscribe(eventType, com.lorecanvas.events.EventListener<DomainEvent> { onAny() })
}
