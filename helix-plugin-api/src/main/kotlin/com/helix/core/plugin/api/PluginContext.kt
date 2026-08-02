package com.helix.core.plugin.api

import com.helix.core.config.api.ConfigManager
import com.helix.core.eventbus.api.EventBus
import com.helix.core.logging.api.Logger
import com.helix.core.registry.api.ServiceRegistry

/**
 * What a plugin receives at load time. Intentionally the same shape as
 * LifecycleContext — a plugin IS a lifecycle-managed component under the
 * hood — but kept as its own type so the plugin surface can evolve
 * independently (e.g. adding plugin-scoped storage/security helpers later)
 * without changing the core LifecycleContext contract.
 */
public interface PluginContext {
    public val serviceRegistry: ServiceRegistry
    public val eventBus: EventBus
    public val configManager: ConfigManager
    public val logger: Logger
}
