package com.helix.core.lifecycle.runtime

import com.helix.core.lifecycle.api.LifecycleAware
import com.helix.core.lifecycle.api.LifecycleContext
import com.helix.core.lifecycle.api.LifecycleState
import java.util.concurrent.atomic.AtomicReference

/**
 * Convenience base class for platform components.
 *
 * Handles lifecycle state transitions and automatically moves a component
 * into FAILED when a lifecycle hook throws.
 */
public abstract class BaseLifecycleComponent : LifecycleAware {

    private val stateRef = AtomicReference(LifecycleState.CREATED)

    final override val state: LifecycleState
        get() = stateRef.get()

    protected lateinit var context: LifecycleContext
        private set

    final override fun onInit(context: LifecycleContext) {
        transitionTo(LifecycleState.INITIALIZING)

        try {
            this.context = context
            doInit(context)
            transitionTo(LifecycleState.INITIALIZED)
        } catch (t: Throwable) {
            transitionToFailed()
            throw t
        }
    }

    final override fun onStart() {
        transitionTo(LifecycleState.STARTING)

        try {
            doStart()
            transitionTo(LifecycleState.RUNNING)
        } catch (t: Throwable) {
            transitionToFailed()
            throw t
        }
    }

    final override fun onStop() {
        transitionTo(LifecycleState.STOPPING)

        try {
            doStop()
            transitionTo(LifecycleState.STOPPED)
        } catch (t: Throwable) {
            transitionToFailed()
            throw t
        }
    }

    final override fun onDestroy() {
        transitionTo(LifecycleState.DESTROYING)

        try {
            doDestroy()
            transitionTo(LifecycleState.DESTROYED)
        } catch (t: Throwable) {
            transitionToFailed()
            throw t
        }
    }

    protected open fun doInit(context: LifecycleContext) {}

    protected open fun doStart() {}

    protected open fun doStop() {}

    protected open fun doDestroy() {}

    private fun transitionTo(target: LifecycleState) {
        stateRef.updateAndGet { current ->
            check(current.canTransitionTo(target)) {
                "Invalid lifecycle state transition: $current -> $target"
            }
            target
        }
    }

    private fun transitionToFailed() {
        stateRef.updateAndGet { current ->
            if (current == LifecycleState.FAILED) {
                current
            } else {
                check(current.canTransitionTo(LifecycleState.FAILED)) {
                    "Cannot transition lifecycle component from $current to FAILED"
                }
                LifecycleState.FAILED
            }
        }
    }
}