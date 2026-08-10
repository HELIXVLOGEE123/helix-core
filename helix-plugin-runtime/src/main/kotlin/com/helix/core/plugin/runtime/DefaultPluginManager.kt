package com.helix.core.plugin.runtime

import com.helix.core.eventbus.api.EventBus
import com.helix.core.logging.api.LoggerFactory
import com.helix.core.plugin.api.HelixPlugin
import com.helix.core.plugin.api.PluginContext
import com.helix.core.plugin.api.PluginDescriptor
import com.helix.core.plugin.api.PluginFailedEvent
import com.helix.core.plugin.api.PluginLoadedEvent
import com.helix.core.plugin.api.PluginManager
import com.helix.core.plugin.api.PluginUnloadedEvent
import java.util.concurrent.ConcurrentHashMap

/**
 * Default [PluginManager]. Validates declared `dependsOn` plugins are
 * already loaded before calling [HelixPlugin.onLoad], and isolates
 * exceptions thrown by a plugin so one misbehaving feature module can never
 * take down HELIX Core itself.
 */
public class DefaultPluginManager(
    private val pluginContext: PluginContext,
    private val eventBus: EventBus,
    loggerFactory: LoggerFactory
) : PluginManager {

    private val logger = loggerFactory.getLogger("helix.plugin")
    private val loaded = ConcurrentHashMap<String, HelixPlugin>()

    override fun loadPlugin(plugin: HelixPlugin) {
        val descriptor = plugin.descriptor
        val missingDeps = descriptor.dependsOn.filterNot { loaded.containsKey(it) }
        require(missingDeps.isEmpty()) {
            "Cannot load plugin '${descriptor.id}': missing dependencies $missingDeps"
        }
        require(!loaded.containsKey(descriptor.id)) { "Plugin '${descriptor.id}' is already loaded" }

        try {
            plugin.onLoad(pluginContext)
            loaded[descriptor.id] = plugin
            logger.info("Plugin loaded", mapOf("id" to descriptor.id, "version" to descriptor.version))
            eventBus.publish(PluginLoadedEvent(descriptor.id))
        } catch (t: Throwable) {
            logger.error("Plugin '${descriptor.id}' failed to load", throwable = t)
            eventBus.publish(PluginFailedEvent(descriptor.id, t.message ?: t::class.simpleName.orEmpty()))
        }
    }

    override fun unloadPlugin(id: String) {
        val plugin = loaded.remove(id) ?: return
        try {
            plugin.onUnload()
            logger.info("Plugin unloaded", mapOf("id" to id))
            eventBus.publish(PluginUnloadedEvent(id))
        } catch (t: Throwable) {
            logger.error("Plugin '$id' threw during unload", throwable = t)
            eventBus.publish(PluginFailedEvent(id, t.message ?: t::class.simpleName.orEmpty()))
        }
    }

    override fun getPlugin(id: String): HelixPlugin? = loaded[id]

    override fun listPlugins(): List<PluginDescriptor> = loaded.values.map { it.descriptor }
}
