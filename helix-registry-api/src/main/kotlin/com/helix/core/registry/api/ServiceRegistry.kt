package com.helix.core.registry.api

import kotlin.reflect.KClass

/**
 * Central directory of platform services, keyed by (interface type, name).
 * This is HELIX Core's Dependency Inversion mechanism: modules declare
 * "I need a Foo" and resolve it here rather than constructing or importing
 * a concrete implementation. Only interfaces should ever be registered —
 * never concrete implementation classes — so callers remain bound to the
 * contract, not the implementation.
 */
public interface ServiceRegistry {

    public fun <T : Any> register(type: KClass<T>, instance: T, name: String = DEFAULT_NAME)

    public fun <T : Any> resolve(type: KClass<T>, name: String = DEFAULT_NAME): T

    public fun <T : Any> resolveOrNull(type: KClass<T>, name: String = DEFAULT_NAME): T?

    public fun <T : Any> unregister(type: KClass<T>, name: String = DEFAULT_NAME)

    public fun listServices(): Set<ServiceKey>

    public companion object {
        public const val DEFAULT_NAME: String = "default"
    }
}

public data class ServiceKey(val type: KClass<*>, val name: String)

public inline fun <reified T : Any> ServiceRegistry.register(instance: T, name: String = ServiceRegistry.DEFAULT_NAME) {
    register(T::class, instance, name)
}

public inline fun <reified T : Any> ServiceRegistry.resolve(name: String = ServiceRegistry.DEFAULT_NAME): T =
    resolve(T::class, name)

public inline fun <reified T : Any> ServiceRegistry.resolveOrNull(name: String = ServiceRegistry.DEFAULT_NAME): T? =
    resolveOrNull(T::class, name)
