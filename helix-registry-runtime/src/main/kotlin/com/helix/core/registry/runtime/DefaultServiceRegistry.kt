package com.helix.core.registry.runtime

import com.helix.core.eventbus.api.EventBus
import com.helix.core.logging.api.LoggerFactory
import com.helix.core.registry.api.ServiceAlreadyRegisteredException
import com.helix.core.registry.api.ServiceKey
import com.helix.core.registry.api.ServiceNotFoundException
import com.helix.core.registry.api.ServiceRegisteredEvent
import com.helix.core.registry.api.ServiceRegistry
import com.helix.core.registry.api.ServiceUnregisteredEvent
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Thread-safe default [ServiceRegistry]. Registration is intentionally
 * strict (throws on duplicate registration) to surface wiring mistakes at
 * startup rather than silently shadowing a service.
 */
public class DefaultServiceRegistry(
    private val eventBus: EventBus,
    loggerFactory: LoggerFactory
) : ServiceRegistry {

    private val logger = loggerFactory.getLogger("helix.registry")
    private val services = ConcurrentHashMap<ServiceKey, Any>()

    override fun <T : Any> register(type: KClass<T>, instance: T, name: String) {
        val key = ServiceKey(type, name)
        val previous = services.putIfAbsent(key, instance)
        if (previous != null) {
            throw ServiceAlreadyRegisteredException(
                "Service already registered for ${type.simpleName} (name='$name'). " +
                    "Unregister the existing instance first if replacement is intentional."
            )
        }
        logger.info("Service registered", mapOf("type" to type.simpleName, "name" to name))
        eventBus.publish(ServiceRegisteredEvent(type.simpleName ?: type.toString(), name))
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> resolve(type: KClass<T>, name: String): T =
        services[ServiceKey(type, name)] as? T
            ?: throw ServiceNotFoundException("No service registered for ${type.simpleName} (name='$name')")

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> resolveOrNull(type: KClass<T>, name: String): T? =
        services[ServiceKey(type, name)] as? T

    override fun <T : Any> unregister(type: KClass<T>, name: String) {
        val key = ServiceKey(type, name)
        if (services.remove(key) != null) {
            logger.info("Service unregistered", mapOf("type" to type.simpleName, "name" to name))
            eventBus.publish(ServiceUnregisteredEvent(type.simpleName ?: type.toString(), name))
        }
    }

    override fun listServices(): Set<ServiceKey> = services.keys.toSet()
}
