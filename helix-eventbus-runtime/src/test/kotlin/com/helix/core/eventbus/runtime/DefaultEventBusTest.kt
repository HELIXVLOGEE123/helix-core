package com.helix.core.eventbus.runtime

import com.helix.core.eventbus.api.EventHandler
import com.helix.core.eventbus.api.HelixEvent
import com.helix.core.logging.api.LoggerFactory
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unit tests for [DefaultEventBus].
 *
 * Scope: this suite tests the EventBus implementation as it currently
 * exists. It does NOT test executor shutdown/termination — see the
 * "Shutdown" section at the bottom of this file for why that coverage
 * item is blocked pending an architecture decision.
 */
internal class DefaultEventBusTest {

    private lateinit var bus: DefaultEventBus

    @BeforeEach
    fun setUp() {
        // Relaxed mock: DefaultEventBus only calls loggerFactory.getLogger(...) once
        // at construction time and logger.error(...) on handler failure. We don't
        // assert on logging behavior, so a relaxed mock satisfies the dependency
        // without needing the exact Logger/LoggerFactory method signatures.
        val loggerFactory = mockk<LoggerFactory>(relaxed = true)
        bus = DefaultEventBus(loggerFactory)
    }

    // ---------------------------------------------------------------------
    // Test fixtures
    // ---------------------------------------------------------------------

    private data class TestEventA(
        override val topic: String,
        override val source: String = "test",
        override val timestamp: Instant = Instant.now()
    ) : HelixEvent

    private data class TestEventB(
        override val topic: String,
        override val source: String = "test",
        override val timestamp: Instant = Instant.now()
    ) : HelixEvent

    // ---------------------------------------------------------------------
    // 1. Type-based subscriptions
    // ---------------------------------------------------------------------

    @Test
    fun `type subscriber receives matching event`() {
        var received: TestEventA? = null
        bus.subscribe(TestEventA::class, EventHandler { received = it })

        val event = TestEventA(topic = "helix.test.a")
        bus.publish(event)

        assertEquals(event, received)
    }

    @Test
    fun `type subscriber does not receive unrelated event type`() {
        var receivedCount = 0
        bus.subscribe(TestEventA::class, EventHandler { receivedCount++ })

        bus.publish(TestEventB(topic = "helix.test.b"))

        assertEquals(0, receivedCount)
    }

    @Test
    fun `multiple type subscribers on same type all receive event`() {
        val counter = AtomicInteger(0)
        bus.subscribe(TestEventA::class, EventHandler { counter.incrementAndGet() })
        bus.subscribe(TestEventA::class, EventHandler { counter.incrementAndGet() })

        bus.publish(TestEventA(topic = "helix.test.a"))

        assertEquals(2, counter.get())
    }

    // ---------------------------------------------------------------------
    // 2. Topic subscriptions
    // ---------------------------------------------------------------------

    @Test
    fun `topic subscriber receives event on exact topic match`() {
        var received: HelixEvent? = null
        bus.subscribeTopic("helix.lifecycle.started", EventHandler { received = it })

        val event = TestEventA(topic = "helix.lifecycle.started")
        bus.publish(event)

        assertEquals(event, received)
    }

    @Test
    fun `topic subscriber matches single-segment wildcard`() {
        var received: HelixEvent? = null
        bus.subscribeTopic("helix.lifecycle.*", EventHandler { received = it })

        val event = TestEventA(topic = "helix.lifecycle.started")
        bus.publish(event)

        assertEquals(event, received)
    }

    @Test
    fun `topic subscriber does not match when segment count differs`() {
        var receivedCount = 0
        bus.subscribeTopic("helix.lifecycle.*", EventHandler { receivedCount++ })

        // One extra segment beyond what the pattern's wildcard covers.
        bus.publish(TestEventA(topic = "helix.lifecycle.component.started"))

        assertEquals(0, receivedCount)
    }

    @Test
    fun `topic subscriber does not match unrelated topic`() {
        var receivedCount = 0
        bus.subscribeTopic("helix.lifecycle.*", EventHandler { receivedCount++ })

        bus.publish(TestEventA(topic = "helix.config.changed"))

        assertEquals(0, receivedCount)
    }

    // ---------------------------------------------------------------------
    // 3. Synchronous publishing
    // ---------------------------------------------------------------------

    @Test
    fun `publish delivers synchronously before returning`() {
        val handlerCompleted = AtomicBoolean(false)
        bus.subscribe(TestEventA::class, EventHandler {
            // Simulate a bit of work so a broken async implementation would
            // likely not have finished it by the time publish() returns.
            Thread.sleep(20)
            handlerCompleted.set(true)
        })

        bus.publish(TestEventA(topic = "helix.test.a"))

        // No waiting here at all: if publish() is truly synchronous, the
        // handler has already completed by this point.
        assertTrue(handlerCompleted.get())
    }

    // ---------------------------------------------------------------------
    // 4. Asynchronous publishing
    // ---------------------------------------------------------------------

    @Test
    fun `publishAsync returns before a slow handler completes`() {
        val latch = CountDownLatch(1)
        val handlerCompleted = AtomicBoolean(false)
        bus.subscribe(TestEventA::class, EventHandler {
            Thread.sleep(200)
            handlerCompleted.set(true)
            latch.countDown()
        })

        bus.publishAsync(TestEventA(topic = "helix.test.a"))

        // Immediately after the call returns, the slow handler should not
        // have finished yet (proves dispatch happened off-thread).
        assertFalse(handlerCompleted.get())

        assertTrue(latch.await(2, TimeUnit.SECONDS), "handler did not complete in time")
        assertTrue(handlerCompleted.get())
    }

    @Test
    fun `publishAsync eventually delivers to handler`() {
        val latch = CountDownLatch(1)
        var received: HelixEvent? = null
        bus.subscribe(TestEventA::class, EventHandler {
            received = it
            latch.countDown()
        })

        val event = TestEventA(topic = "helix.test.a")
        bus.publishAsync(event)

        assertTrue(latch.await(2, TimeUnit.SECONDS), "event was not delivered in time")
        assertEquals(event, received)
    }

    // ---------------------------------------------------------------------
    // 5. Unsubscription
    // ---------------------------------------------------------------------

    @Test
    fun `type subscription stops receiving events after unsubscribe`() {
        val counter = AtomicInteger(0)
        val subscription = bus.subscribe(TestEventA::class, EventHandler { counter.incrementAndGet() })

        bus.publish(TestEventA(topic = "helix.test.a"))
        assertEquals(1, counter.get())

        subscription.unsubscribe()
        bus.publish(TestEventA(topic = "helix.test.a"))

        assertEquals(1, counter.get(), "handler should not have been invoked after unsubscribe")
    }

    @Test
    fun `topic subscription stops receiving events after unsubscribe`() {
        val counter = AtomicInteger(0)
        val subscription = bus.subscribeTopic("helix.lifecycle.*", EventHandler { counter.incrementAndGet() })

        bus.publish(TestEventA(topic = "helix.lifecycle.started"))
        assertEquals(1, counter.get())

        subscription.unsubscribe()
        bus.publish(TestEventA(topic = "helix.lifecycle.started"))

        assertEquals(1, counter.get(), "handler should not have been invoked after unsubscribe")
    }

    @Test
    fun `isActive reflects subscription state before and after unsubscribe`() {
        val subscription = bus.subscribe(TestEventA::class, EventHandler { })

        assertTrue(subscription.isActive)

        subscription.unsubscribe()

        assertFalse(subscription.isActive)
    }

    @Test
    fun `calling unsubscribe twice is a safe no-op`() {
        val subscription = bus.subscribe(TestEventA::class, EventHandler { })

        subscription.unsubscribe()
        assertFalse(subscription.isActive)

        // Second call must not throw and must leave state unchanged.
        subscription.unsubscribe()
        assertFalse(subscription.isActive)
    }

    // ---------------------------------------------------------------------
    // 6. activeSubscriptionCount()
    // ---------------------------------------------------------------------

    @Test
    fun `activeSubscriptionCount is zero with no subscriptions`() {
        assertEquals(0, bus.activeSubscriptionCount())
    }

    @Test
    fun `activeSubscriptionCount reflects mixed type and topic subscriptions`() {
        bus.subscribe(TestEventA::class, EventHandler { })
        bus.subscribe(TestEventB::class, EventHandler { })
        bus.subscribeTopic("helix.lifecycle.*", EventHandler { })

        assertEquals(3, bus.activeSubscriptionCount())
    }

    @Test
    fun `activeSubscriptionCount decrements after unsubscribe`() {
        val sub1 = bus.subscribe(TestEventA::class, EventHandler { })
        bus.subscribeTopic("helix.lifecycle.*", EventHandler { })

        assertEquals(2, bus.activeSubscriptionCount())

        sub1.unsubscribe()

        assertEquals(1, bus.activeSubscriptionCount())
    }

    @Test
    fun `activeSubscriptionCount unaffected by redundant unsubscribe`() {
        val sub1 = bus.subscribe(TestEventA::class, EventHandler { })
        bus.subscribeTopic("helix.lifecycle.*", EventHandler { })

        sub1.unsubscribe()
        assertEquals(1, bus.activeSubscriptionCount())

        sub1.unsubscribe() // redundant
        assertEquals(1, bus.activeSubscriptionCount())
    }

    // ---------------------------------------------------------------------
    // 7. Failure isolation
    // ---------------------------------------------------------------------

    @Test
    fun `one failing type subscriber does not prevent delivery to another`() {
        var secondReceived = false
        bus.subscribe(TestEventA::class, EventHandler { throw RuntimeException("boom") })
        bus.subscribe(TestEventA::class, EventHandler { secondReceived = true })

        // Must not throw out of publish().
        bus.publish(TestEventA(topic = "helix.test.a"))

        assertTrue(secondReceived)
    }

    @Test
    fun `one failing topic subscriber does not prevent delivery to another`() {
        var secondReceived = false
        bus.subscribeTopic("helix.lifecycle.*", EventHandler { throw IllegalStateException("boom") })
        bus.subscribeTopic("helix.lifecycle.*", EventHandler { secondReceived = true })

        bus.publish(TestEventA(topic = "helix.lifecycle.started"))

        assertTrue(secondReceived)
    }

    @Test
    fun `exception from handler does not propagate to publisher`() {
        bus.subscribe(TestEventA::class, EventHandler { throw RuntimeException("boom") })

        // If this line throws, the test fails with the propagated exception.
        bus.publish(TestEventA(topic = "helix.test.a"))
    }

    // ---------------------------------------------------------------------
    // 8. Shutdown — BLOCKED / ARCHITECT DECISION REQUIRED
    // ---------------------------------------------------------------------
    //
    // DefaultEventBus's async executor is a private constructor-supplied
    // field with no exposed shutdown(), close(), or termination-check API
    // on either DefaultEventBus or the EventBus interface. There is
    // currently no way to deterministically observe or trigger executor
    // shutdown from a test without adding new production API surface.
    //
    // Per instructions, no shutdown() method has been added, and no
    // shutdown test has been written. This coverage item is intentionally
    // omitted pending an architect decision on whether/how EventBus should
    // expose lifecycle control over its async executor.

    // ---------------------------------------------------------------------
    // 9. Basic concurrency
    // ---------------------------------------------------------------------

    @Test
    fun `concurrent subscription followed by concurrent publish delivers exactly once per subscriber per event`() {
        val subscriberCount = 20
        val eventCount = 10
        val totalDeliveries = AtomicInteger(0)

        val pool: ExecutorService = Executors.newFixedThreadPool(8)
        try {
            // Phase 1: subscribe concurrently, wait for all subscriptions to land.
            val subscribeLatch = CountDownLatch(subscriberCount)
            repeat(subscriberCount) {
                pool.submit {
                    bus.subscribe(TestEventA::class, EventHandler { totalDeliveries.incrementAndGet() })
                    subscribeLatch.countDown()
                }
            }
            assertTrue(subscribeLatch.await(5, TimeUnit.SECONDS), "subscriptions did not complete in time")
            assertEquals(subscriberCount, bus.activeSubscriptionCount())

            // Phase 2: publish concurrently, wait for all publishes to complete.
            val publishLatch = CountDownLatch(eventCount)
            repeat(eventCount) { i ->
                pool.submit {
                    bus.publish(TestEventA(topic = "helix.test.concurrent.$i"))
                    publishLatch.countDown()
                }
            }
            assertTrue(publishLatch.await(5, TimeUnit.SECONDS), "publishes did not complete in time")

            assertEquals(subscriberCount * eventCount, totalDeliveries.get())
        } finally {
            pool.shutdown()
        }
    }

    @Test
    fun `concurrent async publishes all deliver without loss or duplication`() {
        val eventCount = 50
        val latch = CountDownLatch(eventCount)
        val deliveries = AtomicInteger(0)

        bus.subscribe(TestEventA::class, EventHandler {
            deliveries.incrementAndGet()
            latch.countDown()
        })

        repeat(eventCount) { i ->
            bus.publishAsync(TestEventA(topic = "helix.test.concurrent.async.$i"))
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "not all async events were delivered in time")
        assertEquals(eventCount, deliveries.get())
    }
}
