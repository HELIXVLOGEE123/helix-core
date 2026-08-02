package com.helix.platform

import com.helix.core.lifecycle.api.LifecycleAware
import com.helix.core.lifecycle.api.LifecycleContext
import com.helix.core.lifecycle.api.LifecycleState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * End-to-end smoke test: builds a HelixCore with all defaults, registers a
 * trivial component, and verifies the full start/stop lifecycle executes
 * without error and in the correct order. This is the test every future
 * feature module's own integration test should mirror.
 */
class HelixCoreSmokeTest {

    private class RecordingComponent : LifecycleAware {
        val events = mutableListOf<String>()
        override var state: LifecycleState = LifecycleState.CREATED
            private set

        override fun onInit(context: LifecycleContext) {
            state = LifecycleState.INITIALIZED
            events += "init"
        }
        override fun onStart() {
            state = LifecycleState.RUNNING
            events += "start"
        }
        override fun onStop() {
            state = LifecycleState.STOPPED
            events += "stop"
        }
        override fun onDestroy() {
            state = LifecycleState.DESTROYED
            events += "destroy"
        }
    }

    @Test
    fun `builds and runs full lifecycle with default wiring`() {
        val core = HelixCoreBuilder().build()
        val component = RecordingComponent()
        core.lifecycleManager.register("test-component", component)

        core.start()
        assertEquals(LifecycleState.RUNNING, component.state)

        core.stop()
        assertEquals(LifecycleState.DESTROYED, component.state)

        assertEquals(listOf("init", "start", "stop", "destroy"), component.events)
    }

    @Test
    fun `event bus delivers events published during lifecycle`() {
        val core = HelixCoreBuilder().build()
        var started = false
        core.eventBus.subscribeTopic("helix.lifecycle.component.started") { started = true }

        core.lifecycleManager.register("noop", RecordingComponent())
        core.start()

        assertTrue(started)
        core.stop()
    }

    @Test
    fun `dependency order is respected across components`() {
        val core = HelixCoreBuilder().build()
        val order = mutableListOf<String>()

        val a = object : LifecycleAware {
            override var state: LifecycleState = LifecycleState.CREATED
            override fun onInit(context: LifecycleContext) { order += "a-init" }
            override fun onStart() { order += "a-start" }
            override fun onStop() {}
            override fun onDestroy() {}
        }
        val b = object : LifecycleAware {
            override var state: LifecycleState = LifecycleState.CREATED
            override fun onInit(context: LifecycleContext) { order += "b-init" }
            override fun onStart() { order += "b-start" }
            override fun onStop() {}
            override fun onDestroy() {}
        }

        core.lifecycleManager.register("a", a)
        core.lifecycleManager.register("b", b, dependsOn = listOf("a"))

        core.start()

        assertEquals(listOf("a-init", "b-init", "a-start", "b-start"), order)
        core.stop()
    }
}
