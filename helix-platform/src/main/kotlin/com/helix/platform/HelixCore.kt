package com.helix.platform

import com.helix.core.config.api.ConfigManager
import com.helix.core.eventbus.api.EventBus
import com.helix.core.lifecycle.api.LifecycleManager
import com.helix.core.logging.api.LoggerFactory
import com.helix.core.plugin.api.PluginManager
import com.helix.core.registry.api.ServiceRegistry
import com.helix.core.scheduler.api.Scheduler
import com.helix.core.security.api.SecurityManager
import com.helix.core.storage.api.StorageManager

/**
 * The public face of HELIX Core. Every field is an interface from an
 * `-api` module — HelixCore itself has zero knowledge of which concrete
 * implementations are behind them. Feature modules (Launcher, Voice, AI,
 * Automation, Vision, Memory) depend on this class and this class alone;
 * they never import a `-runtime` module directly.
 *
 * Obtain an instance via [HelixCoreBuilder], never by constructing this
 * class directly, so that construction order and default wiring stay in
 * exactly one place.
 */
public class HelixCore internal constructor(
    public val loggerFactory: LoggerFactory,
    public val eventBus: EventBus,
    public val configManager: ConfigManager,
    public val serviceRegistry: ServiceRegistry,
    public val securityManager: SecurityManager,
    public val storageManager: StorageManager,
    public val scheduler: Scheduler,
    public val lifecycleManager: LifecycleManager,
    public val pluginManager: PluginManager
) {
    private val logger = loggerFactory.getLogger("helix.platform")

    /** Runs onInit then onStart for every registered lifecycle component, in dependency order. */
    public fun start() {
        logger.info("HELIX Core starting")
        lifecycleManager.initAll()
        lifecycleManager.startAll()
        logger.info("HELIX Core running")
    }

    /** Runs onStop then onDestroy for every registered component, in reverse dependency order, then shuts the scheduler down. */
    public fun stop() {
        logger.info("HELIX Core stopping")
        lifecycleManager.stopAll()
        lifecycleManager.destroyAll()
        scheduler.shutdown()
        logger.info("HELIX Core stopped")
    }
}
