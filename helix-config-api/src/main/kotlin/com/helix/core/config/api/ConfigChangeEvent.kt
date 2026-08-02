package com.helix.core.config.api

import com.helix.core.eventbus.api.HelixEvent
import java.time.Instant

/** Published by [ConfigManager] on every reload that changes at least one value. */
public data class ConfigChangeEvent(
    val changedKeys: Set<String>,
    override val timestamp: Instant = Instant.now(),
    override val source: String = "helix.config"
) : HelixEvent {
    override val topic: String = "helix.config.changed"
}
