package com.helix.core.lifecycle.api

/**
 * Orchestrates init/start/stop/destroy for every registered component in
 * correct dependency order. This is what lets HELIX Core add future
 * modules (Launcher, Voice, AI, Automation, Vision, Memory) without any
 * module needing to know how to sequence itself relative to the others —
 * they simply declare `dependsOn` and the manager topologically sorts them.
 */
public interface LifecycleManager {

    public fun register(name: String, component: LifecycleAware, dependsOn: List<String> = emptyList())

    /** Runs onInit for every registered component in dependency order. */
    public fun initAll()

    /** Runs onStart for every registered component in dependency order. */
    public fun startAll()

    /** Runs onStop for every registered component in REVERSE dependency order. */
    public fun stopAll()

    /** Runs onDestroy for every registered component in REVERSE dependency order. */
    public fun destroyAll()

    public fun stateOf(name: String): LifecycleState?

    public fun registeredComponentNames(): List<String>
}
