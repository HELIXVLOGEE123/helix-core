package com.helix.core.lifecycle.api

import com.helix.core.config.api.ConfigManager
import com.helix.core.eventbus.api.EventBus
import com.helix.core.logging.api.Logger
import com.helix.core.registry.api.ServiceRegistry

/**
 * Everything a [LifecycleAware] component is given at [LifecycleAware.onInit]
 * time. This is the ONLY way components should reach the rest of the
 * platform — never via static/global singletons — which is what keeps
 * modules testable and keeps HELIX Core decoupled from any one component.
 */
public interface LifecycleContext {
    public val serviceRegistry: ServiceRegistry
    public val eventBus: EventBus
    public val configManager: ConfigManager
    public val logger: Logger
}
