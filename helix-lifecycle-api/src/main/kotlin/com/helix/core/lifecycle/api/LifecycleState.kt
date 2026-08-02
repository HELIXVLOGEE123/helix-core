package com.helix.core.lifecycle.api

/**
 * Every managed component moves through these states in order (barring
 * FAILED, which is reachable from any transitional state). See
 * docs/ARCHITECTURE.md "Lifecycle Contracts" for the full state diagram.
 */
public enum class LifecycleState {
    CREATED,
    INITIALIZING,
    INITIALIZED,
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
    DESTROYED,
    FAILED
}
