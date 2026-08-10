package com.helix.core.lifecycle.runtime

import com.helix.core.eventbus.api.EventBus
import com.helix.core.lifecycle.api.ComponentFailedEvent
import com.helix.core.lifecycle.api.ComponentInitializedEvent
import com.helix.core.lifecycle.api.ComponentStartedEvent
import com.helix.core.lifecycle.api.ComponentStoppedEvent
import com.helix.core.lifecycle.api.LifecycleAware
import com.helix.core.lifecycle.api.LifecycleContext
import com.helix.core.lifecycle.api.LifecycleManager
import com.helix.core.lifecycle.api.LifecycleState
import com.helix.core.logging.api.LoggerFactory

/**
 * Default [LifecycleManager]. Components are topologically sorted by their
 * declared `dependsOn` edges (Kahn's algorithm); init/start run in that
 * order, stop/destroy run in reverse. A cycle in the dependency graph is a
 * programming error and fails fast at [initAll] time with a clear message.
 *
 * Failure policy: if a component throws during onInit/onStart, it is marked
 * FAILED, a [ComponentFailedEvent] is published, and the manager continues
 * with remaining components rather than aborting the whole platform boot —
 * callers that need fail-fast semantics should subscribe to
 * ComponentFailedEvent and decide themselves whether to halt.
 */
public class DefaultLifecycleManager(
    private val context: LifecycleContext,
    private val eventBus: EventBus,
    loggerFactory: LoggerFactory
) : LifecycleManager {

    private val logger = loggerFactory.getLogger("helix.lifecycle")
    private val entries = LinkedHashMap<String, Entry>()

    private class Entry(val component: LifecycleAware, val dependsOn: List<String>)

    override fun register(name: String, component: LifecycleAware, dependsOn: List<String>) {
        require(name !in entries) { "Component '$name' is already registered" }
        entries[name] = Entry(component, dependsOn)
    }

    override fun initAll() = runPhase(LifecycleState.INITIALIZING, forward = true) { name, entry ->
        entry.component.onInit(context)
        eventBus.publish(ComponentInitializedEvent(name))
    }

    override fun startAll() = runPhase(LifecycleState.STARTING, forward = true) { name, entry ->
        entry.component.onStart()
        eventBus.publish(ComponentStartedEvent(name))
    }

    override fun stopAll() = runPhase(LifecycleState.STOPPING, forward = false) { name, entry ->
        entry.component.onStop()
        eventBus.publish(ComponentStoppedEvent(name))
    }

    override fun destroyAll() = runPhase(LifecycleState.DESTROYING, forward = false) { _, entry ->
        entry.component.onDestroy()
    }

    override fun stateOf(name: String): LifecycleState? = entries[name]?.component?.state

    override fun registeredComponentNames(): List<String> = topologicalOrder()

    private inline fun runPhase(
        phase: LifecycleState,
        forward: Boolean,
        action: (String, Entry) -> Unit
    ) {
        val order = topologicalOrder().let { if (forward) it else it.asReversed() }
        for (name in order) {
            val entry = entries.getValue(name)
            try {
                action(name, entry)
            } catch (t: Throwable) {
                logger.error("Component '$name' failed during $phase", throwable = t)
                eventBus.publish(ComponentFailedEvent(name, phase, t.message ?: t::class.simpleName.orEmpty()))
            }
        }
    }

    /** Kahn's algorithm; throws IllegalStateException with the offending component names if a cycle is detected. */
    private fun topologicalOrder(): List<String> {
        val inDegree = entries.mapValues { (_, entry) -> entry.dependsOn.size }.toMutableMap()
        val dependents = mutableMapOf<String, MutableList<String>>()
        for ((name, entry) in entries) {
            for (dep in entry.dependsOn) {
                require(dep in entries) { "Component '$name' depends on unregistered component '$dep'" }
                dependents.getOrPut(dep) { mutableListOf() }.add(name)
            }
        }

        val queue = ArrayDeque(inDegree.filterValues { it == 0 }.keys)
        val order = mutableListOf<String>()
        while (queue.isNotEmpty()) {
            val name = queue.removeFirst()
            order.add(name)
            for (dependent in dependents[name].orEmpty()) {
                inDegree[dependent] = inDegree.getValue(dependent) - 1
                if (inDegree.getValue(dependent) == 0) queue.addLast(dependent)
            }
        }

        check(order.size == entries.size) {
            val cyclic = entries.keys - order.toSet()
            "Cyclic dependency detected among lifecycle components: $cyclic"
        }
        return order
    }
}
