package com.helix.core.lifecycle.runtime

import com.helix.core.config.api.ConfigManager
import com.helix.core.config.api.ConfigSource
import com.helix.core.eventbus.api.EventBus
import com.helix.core.eventbus.api.EventHandler
import com.helix.core.eventbus.api.HelixEvent
import com.helix.core.eventbus.api.Subscription
import com.helix.core.lifecycle.api.ComponentInitializedEvent
import com.helix.core.lifecycle.api.ComponentStartedEvent
import com.helix.core.lifecycle.api.ComponentStoppedEvent
import com.helix.core.lifecycle.api.LifecycleContext
import com.helix.core.lifecycle.api.LifecycleState
import com.helix.core.logging.api.LogLevel
import com.helix.core.logging.api.LogSink
import com.helix.core.logging.api.Logger
import com.helix.core.logging.api.LoggerFactory
import com.helix.core.registry.api.ServiceKey
import com.helix.core.registry.api.ServiceRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.reflect.KClass

class DefaultLifecycleManagerTest {

    private lateinit var eventBus: TestEventBus
    private lateinit var context: TestLifecycleContext
    private lateinit var manager: DefaultLifecycleManager

    @BeforeEach
    fun setUp() {
        eventBus = TestEventBus()
        context = TestLifecycleContext(eventBus)
        manager = DefaultLifecycleManager(
            context = context,
            eventBus = eventBus,
            loggerFactory = DummyLoggerFactory()
        )
    }

    private class TestComponent(
        private val failOnStart: Boolean = false
    ) : BaseLifecycleComponent() {

        val actionLog = mutableListOf<String>()

        override fun doInit(context: LifecycleContext) {
            actionLog.add("init")
        }

        override fun doStart() {
            if (failOnStart) {
                throw RuntimeException("Start failure")
            }
            actionLog.add("start")
        }

        override fun doStop() {
            actionLog.add("stop")
        }

        override fun doDestroy() {
            actionLog.add("destroy")
        }
    }

    private class TestEventBus : EventBus {

        val events = CopyOnWriteArrayList<HelixEvent>()

        override fun publish(event: HelixEvent) {
            events.add(event)
        }

        override fun publishAsync(event: HelixEvent) {
            events.add(event)
        }

        override fun <T : HelixEvent> subscribe(
            eventType: KClass<T>,
            handler: EventHandler<T>
        ): Subscription {
            return DummySubscription()
        }

        override fun subscribeTopic(
            topicPattern: String,
            handler: EventHandler<HelixEvent>
        ): Subscription {
            return DummySubscription()
        }

        override fun activeSubscriptionCount(): Int = 0
    }

    private class DummySubscription : Subscription {

        override var isActive: Boolean = true
            private set

        override fun unsubscribe() {
            isActive = false
        }
    }

    private class TestLifecycleContext(
        override val eventBus: EventBus,
        override val serviceRegistry: ServiceRegistry = DummyServiceRegistry(),
        override val configManager: ConfigManager = DummyConfigManager(),
        override val logger: Logger = DummyLogger()
    ) : LifecycleContext

    private class DummyServiceRegistry : ServiceRegistry {

        override fun <T : Any> register(
            type: KClass<T>,
            instance: T,
            name: String
        ) = Unit

        override fun <T : Any> resolve(
            type: KClass<T>,
            name: String
        ): T {
            throw NoSuchElementException("No test service registered")
        }

        override fun <T : Any> resolveOrNull(
            type: KClass<T>,
            name: String
        ): T? = null

        override fun <T : Any> unregister(
            type: KClass<T>,
            name: String
        ) = Unit

        override fun listServices(): Set<ServiceKey> = emptySet()
    }

    private class DummyConfigManager : ConfigManager {

        override fun getString(
            key: String,
            default: String?
        ): String? = default

        override fun getInt(
            key: String,
            default: Int?
        ): Int? = default

        override fun getLong(
            key: String,
            default: Long?
        ): Long? = default

        override fun getBoolean(
            key: String,
            default: Boolean?
        ): Boolean? = default

        override fun containsKey(key: String): Boolean = false

        override fun allKeys(): Set<String> = emptySet()

        override fun addSource(source: ConfigSource) = Unit

        override fun reload() = Unit
    }

    private class DummyLogger : Logger {

        override val name: String = "test"

        override fun isEnabledFor(level: LogLevel): Boolean = true

        override fun trace(
            message: String,
            metadata: Map<String, Any?>
        ) = Unit

        override fun debug(
            message: String,
            metadata: Map<String, Any?>
        ) = Unit

        override fun info(
            message: String,
            metadata: Map<String, Any?>
        ) = Unit

        override fun warn(
            message: String,
            metadata: Map<String, Any?>
        ) = Unit

        override fun error(
            message: String,
            throwable: Throwable?,
            metadata: Map<String, Any?>
        ) = Unit

        override fun fatal(
            message: String,
            throwable: Throwable?,
            metadata: Map<String, Any?>
        ) = Unit
    }

    private class DummyLoggerFactory : LoggerFactory {

        private val logger = DummyLogger()

        override fun getLogger(name: String): Logger = logger

        override fun addSink(sink: LogSink) = Unit

        override fun removeSink(sink: LogSink) = Unit

        override fun setGlobalMinimumLevel(level: LogLevel) = Unit
    }

    @Test
    fun testDependencyOrdering() {
        val executionOrder = mutableListOf<String>()

        class OrderingComponent(
            private val componentName: String
        ) : BaseLifecycleComponent() {

            override fun doStart() {
                executionOrder.add(componentName)
            }
        }

        val compA = OrderingComponent("A")
        val compB = OrderingComponent("B")
        val compC = OrderingComponent("C")

        manager.register("A", compA, dependsOn = listOf("B"))
        manager.register("C", compC)
        manager.register("B", compB, dependsOn = listOf("C"))

        manager.initAll()
        manager.startAll()

        assertEquals(
            listOf("C", "B", "A"),
            executionOrder
        )
    }

    @Test
    fun testCircularDependencyDetection() {
        manager.register(
            "A",
            TestComponent(),
            dependsOn = listOf("B")
        )

        manager.register(
            "B",
            TestComponent(),
            dependsOn = listOf("A")
        )

        val exception = assertThrows(IllegalStateException::class.java) {
            manager.initAll()
        }

        assertTrue(
            exception.message!!.contains("Circular dependency")
        )
    }

    @Test
    fun testMissingDependencyDetection() {
        manager.register(
            "A",
            TestComponent(),
            dependsOn = listOf("NonExistent")
        )

        val exception = assertThrows(IllegalStateException::class.java) {
            manager.initAll()
        }

        assertTrue(
            exception.message!!.contains("Missing dependency")
        )
    }

    @Test
    fun testInvalidComponentTransitions() {
        val component = TestComponent()

        assertThrows(IllegalStateException::class.java) {
            component.onStart()
        }

        assertThrows(IllegalStateException::class.java) {
            component.onStop()
        }

        component.onInit(context)

        assertEquals(
            LifecycleState.INITIALIZED,
            component.state
        )

        component.onStart()

        assertEquals(
            LifecycleState.RUNNING,
            component.state
        )

        assertThrows(IllegalStateException::class.java) {
            component.onStart()
        }
    }

    @Test
    fun testInvalidManagerStateTransitions() {
        assertThrows(IllegalStateException::class.java) {
            manager.startAll()
        }

        assertThrows(IllegalStateException::class.java) {
            manager.stopAll()
        }

        manager.initAll()

        assertThrows(IllegalStateException::class.java) {
            manager.initAll()
        }
    }

    @Test
    fun testStartupFailureAndRollback() {
        val comp1 = TestComponent()
        val comp2 = TestComponent()
        val comp3 = TestComponent(failOnStart = true)

        manager.register("comp1", comp1)

        manager.register(
            "comp2",
            comp2,
            dependsOn = listOf("comp1")
        )

        manager.register(
            "comp3",
            comp3,
            dependsOn = listOf("comp2")
        )

        manager.initAll()

        val exception = assertThrows(RuntimeException::class.java) {
            manager.startAll()
        }

        assertTrue(
            exception.message!!.contains("Start failure")
        )

        assertEquals(
            LifecycleState.STOPPED,
            comp1.state
        )

        assertEquals(
            LifecycleState.STOPPED,
            comp2.state
        )

        assertEquals(
            LifecycleState.FAILED,
            comp3.state
        )

        assertTrue(comp1.actionLog.contains("stop"))
        assertTrue(comp2.actionLog.contains("stop"))
    }

    @Test
    fun testShutdownOrdering() {
        val stopOrder = mutableListOf<String>()
        val destroyOrder = mutableListOf<String>()

        class ShutdownComponent(
            private val componentName: String
        ) : BaseLifecycleComponent() {

            override fun doStop() {
                stopOrder.add(componentName)
            }

            override fun doDestroy() {
                destroyOrder.add(componentName)
            }
        }

        val compA = ShutdownComponent("A")
        val compB = ShutdownComponent("B")

        manager.register(
            "A",
            compA,
            dependsOn = listOf("B")
        )

        manager.register("B", compB)

        manager.initAll()
        manager.startAll()

        manager.stopAll()

        assertEquals(
            listOf("A", "B"),
            stopOrder
        )

        manager.destroyAll()

        assertEquals(
            listOf("A", "B"),
            destroyOrder
        )
    }

    @Test
    fun testRepeatedStartStop() {
        val component = TestComponent()

        manager.register("comp", component)

        manager.initAll()
        manager.startAll()
        manager.stopAll()
        manager.startAll()
        manager.stopAll()
        manager.destroyAll()

        assertEquals(
            LifecycleState.DESTROYED,
            component.state
        )
    }

    @Test
    fun testStateOfAndRegisteredNames() {
        val component = TestComponent()

        manager.register("comp", component)

        assertEquals(
            listOf("comp"),
            manager.registeredComponentNames()
        )

        assertEquals(
            LifecycleState.CREATED,
            manager.stateOf("comp")
        )

        assertEquals(
            null,
            manager.stateOf("missing")
        )
    }

    @Test
    fun testEventBusIntegration() {
        val component = TestComponent()

        manager.register("comp", component)

        manager.initAll()
        manager.startAll()
        manager.stopAll()

        assertTrue(
            eventBus.events.any {
                it is ComponentInitializedEvent &&
                    it.componentName == "comp"
            }
        )

        assertTrue(
            eventBus.events.any {
                it is ComponentStartedEvent &&
                    it.componentName == "comp"
            }
        )

        assertTrue(
            eventBus.events.any {
                it is ComponentStoppedEvent &&
                    it.componentName == "comp"
            }
        )
    }

    @Test
    fun testThreadSafeRegistration() {
        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        repeat(threadCount) { index ->
            executor.submit {
                try {
                    manager.register(
                        "comp-$index",
                        TestComponent()
                    )
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        assertEquals(
            threadCount,
            manager.registeredComponentNames().size
        )
    }
}