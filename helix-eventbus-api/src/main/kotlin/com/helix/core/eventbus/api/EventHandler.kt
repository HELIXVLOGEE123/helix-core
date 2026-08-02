package com.helix.core.eventbus.api

/**
 * Callback invoked for each matching event. Implementations must return
 * promptly and must not throw for control flow; unhandled exceptions are
 * caught by the bus, logged, and reported via a dead-letter mechanism —
 * they do not propagate to the publisher or other subscribers.
 */
public fun interface EventHandler<in T : HelixEvent> {
    public fun handle(event: T)
}
