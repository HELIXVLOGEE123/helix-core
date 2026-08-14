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
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

public class DefaultLifecycleManager(
    private val context: LifecycleContext,
    private val eventBus: EventBus = context.eventBus,
    loggerFactory: LoggerFactory
) : LifecycleManager {

    private val lock = ReentrantLock()

    private val logger = loggerFactory.getLogger("helix.lifecycle")

    private data class Registration(
        val name: String,
        val component: LifecycleAware,
        val dependsOn: List<String>
    )

    private val registrations = LinkedHashMap<String, Registration>()

    private var managerState = LifecycleState.CREATED

    public val state: LifecycleState
        get() = lock.withLock { managerState }

    override fun register(
        name: String,
        component: LifecycleAware,
        dependsOn: List<String>
    ) {
        lock.withLock {
            check(
                managerState == LifecycleState.CREATED ||
                managerState == LifecycleState.INITIALIZED ||
                managerState == LifecycleState.STOPPED
            ) {
                "Cannot register component '$name' when LifecycleManager is in state $managerState"
            }

            check(name !in registrations) {
                "Component '$name' is already registered"
            }

            registrations[name] = Registration(
                name = name,
                component = component,
                dependsOn = dependsOn.toList()
            )
        }
    }

    override fun initAll() {
        lock.withLock {
            check(managerState == LifecycleState.CREATED) {
                "Cannot execute initAll when LifecycleManager is in state $managerState"
            }

            managerState = LifecycleState.INITIALIZING

            try {
                val ordered = topologicalOrder()

                for (registration in ordered) {
                    try {
                        registration.component.onInit(context)

                        eventBus.publish(
                            ComponentInitializedEvent(registration.name)
                        )
                    } catch (t: Throwable) {
                        eventBus.publish(
                            ComponentFailedEvent(
                                registration.name,
                                registration.component.state,
                                t.message ?: t::class.simpleName.orEmpty()
                            )
                        )

                        throw t
                    }
                }

                managerState = LifecycleState.INITIALIZED
            } catch (t: Throwable) {
                managerState = LifecycleState.FAILED
                throw t
            }
        }
    }

    override fun startAll() {
        lock.withLock {
            check(
                managerState == LifecycleState.INITIALIZED ||
                managerState == LifecycleState.STOPPED
            ) {
                "Cannot execute startAll when LifecycleManager is in state $managerState"
            }

            managerState = LifecycleState.STARTING

            val ordered = topologicalOrder()
            val started = mutableListOf<Registration>()

            try {
                for (registration in ordered) {
                    try {
                        registration.component.onStart()

                        started.add(registration)

                        eventBus.publish(
                            ComponentStartedEvent(registration.name)
                        )
                    } catch (t: Throwable) {
                        eventBus.publish(
                            ComponentFailedEvent(
                                registration.name,
                                registration.component.state,
                                t.message ?: t::class.simpleName.orEmpty()
                            )
                        )

                        throw t
                    }
                }

                managerState = LifecycleState.RUNNING
            } catch (t: Throwable) {

                // Roll back components that successfully started.
                for (registration in started.asReversed()) {
                    try {
                        registration.component.onStop()

                        eventBus.publish(
                            ComponentStoppedEvent(registration.name)
                        )
                    } catch (rollbackError: Throwable) {
                        logger.error(
                            "Error stopping component '${registration.name}' during startup rollback",
                            rollbackError
                        )
                    }
                }

                managerState = LifecycleState.FAILED
                throw t
            }
        }
    }

    override fun stopAll() {
        lock.withLock {
            check(managerState == LifecycleState.RUNNING) {
                "Cannot execute stopAll when LifecycleManager is in state $managerState"
            }

            managerState = LifecycleState.STOPPING

            try {
                for (registration in topologicalOrder().asReversed()) {
                    try {
                        registration.component.onStop()

                        eventBus.publish(
                            ComponentStoppedEvent(registration.name)
                        )
                    } catch (t: Throwable) {
                        eventBus.publish(
                            ComponentFailedEvent(
                                registration.name,
                                registration.component.state,
                                t.message ?: t::class.simpleName.orEmpty()
                            )
                        )

                        throw t
                    }
                }

                managerState = LifecycleState.STOPPED
            } catch (t: Throwable) {
                managerState = LifecycleState.FAILED
                throw t
            }
        }
    }

    override fun destroyAll() {
        lock.withLock {
            check(
                managerState == LifecycleState.INITIALIZED ||
                managerState == LifecycleState.STOPPED ||
                managerState == LifecycleState.FAILED ||
                managerState == LifecycleState.CREATED
            ) {
                "Cannot execute destroyAll when LifecycleManager is in state $managerState"
            }

            managerState = LifecycleState.DESTROYING

            try {
                for (registration in topologicalOrder().asReversed()) {
                    try {
                        registration.component.onDestroy()
                    } catch (t: Throwable) {
                        eventBus.publish(
                            ComponentFailedEvent(
                                registration.name,
                                registration.component.state,
                                t.message ?: t::class.simpleName.orEmpty()
                            )
                        )

                        throw t
                    }
                }

                managerState = LifecycleState.DESTROYED
            } catch (t: Throwable) {
                managerState = LifecycleState.FAILED
                throw t
            }
        }
    }

    override fun stateOf(name: String): LifecycleState? {
        return lock.withLock {
            registrations[name]?.component?.state
        }
    }

    override fun registeredComponentNames(): List<String> {
        return lock.withLock {
            registrations.keys.toList()
        }
    }

    /**
     * Returns components in dependency-first order.
     *
     * If A depends on B, B appears before A.
     */
    private fun topologicalOrder(): List<Registration> {

        val inDegree = LinkedHashMap<String, Int>()
        val dependents = LinkedHashMap<String, MutableList<String>>()

        for (name in registrations.keys) {
            inDegree[name] = 0
            dependents[name] = mutableListOf()
        }

        for (registration in registrations.values) {
            for (dependency in registration.dependsOn) {

                check(dependency in registrations) {
                    "Missing dependency '$dependency' required by component '${registration.name}'"
                }

                inDegree[registration.name] =
                    inDegree.getValue(registration.name) + 1

                dependents.getValue(dependency).add(registration.name)
            }
        }

        val queue = ArrayDeque<String>()

        for ((name, degree) in inDegree) {
            if (degree == 0) {
                queue.addLast(name)
            }
        }

        val sortedNames = mutableListOf<String>()

        while (queue.isNotEmpty()) {
            val name = queue.removeFirst()

            sortedNames.add(name)

            for (dependent in dependents.getValue(name)) {
                val newDegree =
                    inDegree.getValue(dependent) - 1

                inDegree[dependent] = newDegree

                if (newDegree == 0) {
                    queue.addLast(dependent)
                }
            }
        }

        check(sortedNames.size == registrations.size) {
            val cyclic = registrations.keys
                .filterNot { it in sortedNames }

            "Circular dependency detected: $cyclic"
        }

        return sortedNames.map { registrations.getValue(it) }
    }
}