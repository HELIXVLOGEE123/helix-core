package com.helix.core.eventbus.api

/**
 * Handle returned by every subscribe call. Callers MUST hold on to this and
 * call [unsubscribe] when their component stops (typically from
 * [com.helix.core.lifecycle.api.LifecycleAware.onStop]) to avoid leaking
 * handlers for destroyed components.
 */
public interface Subscription {
    public val isActive: Boolean
    public fun unsubscribe()
}
