package com.helix.core.lifecycle.api

/**
 * Orchestrates init/start/stop/destroy for every registered component in
 * correct dependency order.
 */
public interface LifecycleManager {

    public fun register(
        name: String,
        component: LifecycleAware,
        dependsOn: List<String> = emptyList()
    )

    /** Runs onInit for every registered component in dependency order. */
    public fun initAll()

    /** Runs onStart for every registered component in dependency order. */
    public fun startAll()

    /** Runs onStop for every registered component in reverse dependency order. */
    public fun stopAll()

    /** Runs onDestroy for every registered component in reverse dependency order. */
    public fun destroyAll()

    public fun stateOf(name: String): LifecycleState?

    public fun registeredComponentNames(): List<String>
}