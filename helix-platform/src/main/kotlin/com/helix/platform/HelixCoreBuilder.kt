package com.helix.platform

import com.helix.core.config.api.ConfigManager
import com.helix.core.config.runtime.DefaultConfigManager
import com.helix.core.config.runtime.EnvConfigSource
import com.helix.core.eventbus.api.EventBus
import com.helix.core.eventbus.runtime.DefaultEventBus
import com.helix.core.lifecycle.api.LifecycleContext
import com.helix.core.lifecycle.api.LifecycleManager
import com.helix.core.lifecycle.runtime.DefaultLifecycleManager
import com.helix.core.logging.api.LoggerFactory
import com.helix.core.logging.runtime.ConsoleLogSink
import com.helix.core.logging.runtime.DefaultLoggerFactory
import com.helix.core.plugin.api.PluginContext
import com.helix.core.plugin.api.PluginManager
import com.helix.core.plugin.runtime.DefaultPluginManager
import com.helix.core.registry.api.ServiceRegistry
import com.helix.core.registry.api.register
import com.helix.core.registry.runtime.DefaultServiceRegistry
import com.helix.core.scheduler.api.Scheduler
import com.helix.core.scheduler.runtime.DefaultScheduler
import com.helix.core.security.api.SecurityManager
import com.helix.core.security.runtime.DefaultSecurityManager
import com.helix.core.storage.api.StorageManager
import com.helix.core.storage.runtime.DefaultStorageManager

/**
 * Composition root for [HelixCore]. Every subsystem defaults to its
 * `-runtime` implementation but can be swapped by callers (Open/Closed
 * Principle: extend the platform by substituting an implementation behind
 * an interface, never by modifying HELIX Core's source).
 *
 * Example:
 * ```
 * val core = HelixCoreBuilder()
 *     .withStorageManager { MyCloudStorageManager(it.loggerFactory) }
 *     .build()
 * core.start()
 * ```
 */
public class HelixCoreBuilder {

    private var loggerFactoryOverride: (() -> LoggerFactory)? = null
    private var eventBusOverride: ((LoggerFactory) -> EventBus)? = null
    private var configManagerOverride: ((EventBus, LoggerFactory) -> ConfigManager)? = null
    private var serviceRegistryOverride: ((EventBus, LoggerFactory) -> ServiceRegistry)? = null
    private var securityManagerOverride: ((EventBus, LoggerFactory) -> SecurityManager)? = null
    private var storageManagerOverride: ((LoggerFactory) -> StorageManager)? = null
    private var schedulerOverride: ((EventBus, LoggerFactory) -> Scheduler)? = null

    public fun withLoggerFactory(factory: () -> LoggerFactory): HelixCoreBuilder =
        apply { loggerFactoryOverride = factory }

    public fun withEventBus(factory: (LoggerFactory) -> EventBus): HelixCoreBuilder =
        apply { eventBusOverride = factory }

    public fun withConfigManager(factory: (EventBus, LoggerFactory) -> ConfigManager): HelixCoreBuilder =
        apply { configManagerOverride = factory }

    public fun withServiceRegistry(factory: (EventBus, LoggerFactory) -> ServiceRegistry): HelixCoreBuilder =
        apply { serviceRegistryOverride = factory }

    public fun withSecurityManager(factory: (EventBus, LoggerFactory) -> SecurityManager): HelixCoreBuilder =
        apply { securityManagerOverride = factory }

    public fun withStorageManager(factory: (LoggerFactory) -> StorageManager): HelixCoreBuilder =
        apply { storageManagerOverride = factory }

    public fun withScheduler(factory: (EventBus, LoggerFactory) -> Scheduler): HelixCoreBuilder =
        apply { schedulerOverride = factory }

    public fun build(): HelixCore {
        val loggerFactory = (loggerFactoryOverride ?: { DefaultLoggerFactory() }).invoke()
            .also { it.addSink(ConsoleLogSink()) }

        val eventBus = (eventBusOverride ?: { lf: LoggerFactory -> DefaultEventBus(lf) }).invoke(loggerFactory)

        val configManager = (configManagerOverride ?: { eb: EventBus, lf: LoggerFactory -> DefaultConfigManager(eb, lf) })
            .invoke(eventBus, loggerFactory)
            .also { it.addSource(EnvConfigSource()) }

        val serviceRegistry = (serviceRegistryOverride ?: { eb: EventBus, lf: LoggerFactory -> DefaultServiceRegistry(eb, lf) })
            .invoke(eventBus, loggerFactory)

        val securityManager = (securityManagerOverride ?: { eb: EventBus, lf: LoggerFactory -> DefaultSecurityManager(eb, lf) })
            .invoke(eventBus, loggerFactory)

        val storageManager = (storageManagerOverride ?: { lf: LoggerFactory -> DefaultStorageManager(lf) })
            .invoke(loggerFactory)

        val scheduler = (schedulerOverride ?: { eb: EventBus, lf: LoggerFactory -> DefaultScheduler(eb, lf) })
            .invoke(eventBus, loggerFactory)

        // Register every subsystem interface into the ServiceRegistry too, so
        // components resolve dependencies uniformly (via ServiceRegistry)
        // regardless of whether they were constructed by the builder or
        // added later as a plugin.
        serviceRegistry.register<LoggerFactory>(loggerFactory)
        serviceRegistry.register<EventBus>(eventBus)
        serviceRegistry.register<ConfigManager>(configManager)
        serviceRegistry.register<SecurityManager>(securityManager)
        serviceRegistry.register<StorageManager>(storageManager)
        serviceRegistry.register<Scheduler>(scheduler)

        val lifecycleContext = object : LifecycleContext {
            override val serviceRegistry: ServiceRegistry = serviceRegistry
            override val eventBus: EventBus = eventBus
            override val configManager: ConfigManager = configManager
            override val logger = loggerFactory.getLogger("helix.lifecycle.component")
        }
        val lifecycleManager: LifecycleManager = DefaultLifecycleManager(lifecycleContext, eventBus, loggerFactory)

        val pluginContext = object : PluginContext {
            override val serviceRegistry: ServiceRegistry = serviceRegistry
            override val eventBus: EventBus = eventBus
            override val configManager: ConfigManager = configManager
            override val logger = loggerFactory.getLogger("helix.plugin.host")
        }
        val pluginManager: PluginManager = DefaultPluginManager(pluginContext, eventBus, loggerFactory)

        return HelixCore(
            loggerFactory = loggerFactory,
            eventBus = eventBus,
            configManager = configManager,
            serviceRegistry = serviceRegistry,
            securityManager = securityManager,
            storageManager = storageManager,
            scheduler = scheduler,
            lifecycleManager = lifecycleManager,
            pluginManager = pluginManager
        )
    }
}
