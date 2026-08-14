package com.helix.core.lifecycle.api

/**
 * Every managed component moves through these states in order.
 * FAILED is reachable when a lifecycle operation throws an exception.
 */
public enum class LifecycleState {
    CREATED,
    INITIALIZING,
    INITIALIZED,
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
    DESTROYING,
    DESTROYED,
    FAILED;

    public fun canTransitionTo(target: LifecycleState): Boolean {
        return when (this) {
            CREATED -> target in setOf(
                INITIALIZING,
                DESTROYING,
                FAILED
            )

            INITIALIZING -> target in setOf(
                INITIALIZED,
                FAILED
            )

            INITIALIZED -> target in setOf(
                STARTING,
                DESTROYING,
                FAILED
            )

            STARTING -> target in setOf(
                RUNNING,
                FAILED
            )

            RUNNING -> target in setOf(
                STOPPING,
                FAILED
            )

            STOPPING -> target in setOf(
                STOPPED,
                FAILED
            )

            STOPPED -> target in setOf(
                STARTING,
                DESTROYING,
                FAILED
            )

            DESTROYING -> target in setOf(
                DESTROYED,
                FAILED
            )

            DESTROYED -> false

            FAILED -> target in setOf(
                DESTROYING,
                FAILED
            )
        }
    }
}