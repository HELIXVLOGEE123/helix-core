package com.helix.core.lifecycle.runtime

import com.helix.core.lifecycle.api.LifecycleAware
import com.helix.core.lifecycle.api.LifecycleContext
import com.helix.core.lifecycle.api.LifecycleState

/**
 * Convenience base class for platform components: handles state transitions
 * and guards against invalid calls (e.g. onStart before onInit) so concrete
 * components only need to implement the four `do*` hooks. Not mandatory —
 * any class can implement [LifecycleAware] directly — but recommended for
 * consistency across the codebase (see docs/CODING_STANDARDS.md).
 */
public abstract class BaseLifecycleComponent : LifecycleAware {

    @Volatile
    final override var state: LifecycleState = LifecycleState.CREATED
        private set

    protected lateinit var context: LifecycleContext
        private set

    final override fun onInit(context: LifecycleContext) {
        check(state == LifecycleState.CREATED) { "onInit called from invalid state $state" }
        state = LifecycleState.INITIALIZING
        this.context = context
        doInit(context)
        state = LifecycleState.INITIALIZED
    }

    final override fun onStart() {
        check(state == LifecycleState.INITIALIZED || state == LifecycleState.STOPPED) {
            "onStart called from invalid state $state"
        }
        state = LifecycleState.STARTING
        doStart()
        state = LifecycleState.RUNNING
    }

    final override fun onStop() {
        if (state != LifecycleState.RUNNING) return
        state = LifecycleState.STOPPING
        doStop()
        state = LifecycleState.STOPPED
    }

    final override fun onDestroy() {
        if (state == LifecycleState.DESTROYED) return
        doDestroy()
        state = LifecycleState.DESTROYED
    }

    protected open fun doInit(context: LifecycleContext) {}
    protected open fun doStart() {}
    protected open fun doStop() {}
    protected open fun doDestroy() {}
}
