package com.helix.core.lifecycle.api

/**
 * Contract every managed platform component implements. Methods are called
 * strictly in order by [LifecycleManager]: onInit -> onStart -> onStop ->
 * onDestroy. onStop/onDestroy are always attempted even if a later
 * component's onStart failed, so implementations must be safe to call on a
 * partially-initialized component (i.e. guard on current state).
 */
public interface LifecycleAware {
    public val state: LifecycleState

    public fun onInit(context: LifecycleContext)
    public fun onStart()
    public fun onStop()
    public fun onDestroy()
}
