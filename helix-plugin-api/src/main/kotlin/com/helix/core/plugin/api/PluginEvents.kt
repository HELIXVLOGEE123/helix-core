package com.helix.core.plugin.api

import com.helix.core.eventbus.api.HelixEvent
import java.time.Instant

public data class PluginLoadedEvent(
    val pluginId: String,
    override val timestamp: Instant = Instant.now(),
    override val source: String = "helix.plugin"
) : HelixEvent {
    override val topic: String = "helix.plugin.loaded"
}

public data class PluginUnloadedEvent(
    val pluginId: String,
    override val timestamp: Instant = Instant.now(),
    override val source: String = "helix.plugin"
) : HelixEvent {
    override val topic: String = "helix.plugin.unloaded"
}

public data class PluginFailedEvent(
    val pluginId: String,
    val error: String,
    override val timestamp: Instant = Instant.now(),
    override val source: String = "helix.plugin"
) : HelixEvent {
    override val topic: String = "helix.plugin.failed"
}
