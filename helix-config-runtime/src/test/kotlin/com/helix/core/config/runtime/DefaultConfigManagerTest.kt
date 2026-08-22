package com.helix.core.config.runtime

import com.helix.core.config.api.ConfigChangeEvent
import com.helix.core.config.api.ConfigSource
import com.helix.core.eventbus.api.EventBus
import com.helix.core.eventbus.api.EventHandler
import com.helix.core.eventbus.api.HelixEvent
import com.helix.core.eventbus.api.Subscription
import com.helix.core.logging.api.LogLevel
import com.helix.core.logging.api.LogSink
import com.helix.core.logging.api.Logger
import com.helix.core.logging.api.LoggerFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/**
 * Unit tests for [DefaultConfigManager].
 *
 * Built strictly against contracts verified directly from the real
 * `phase3` branch (fetched via raw.githubusercontent.com, not assumed from
 * a local sandbox copy): [com.helix.core.eventbus.api.EventBus] on this
 * branch has NO `shutdown()` method, so [FakeEventBus] below implements
 * only what the real interface actually declares. If a later branch state
 * adds `shutdown()`, this fake does not need to change (interfaces are
 * additive-compatible here), but it's worth knowing this file predates
 * that method.
 *
 * Uses hand-written fakes rather than MockK, per this project's own
 * stated preference (CODING_STANDARDS.md, if that file is present on this
 * branch in the form previously seen: "favor fakes/stubs... over mocking
 * frameworks") -- not re-verified from the real branch for this task, but
 * consistent with it regardless.
 */
class DefaultConfigManagerTest {

    // -----------------------------------------------------------------
    // Fakes
    // -----------------------------------------------------------------

    private class FakeConfigSource(
        override val name: String,
        override val priority: Int,
        private val data: Map<String, String>,
        private val throwOnLoad: Boolean = false
    ) : ConfigSource {
        override fun load(): Map<String, String> {
            if (throwOnLoad) throw IllegalStateException("simulated load failure for '$name'")
            return data
        }
    }

    private class FakeEventBus : EventBus {
        val published = mutableListOf<HelixEvent>()

        @Synchronized
        override fun publish(event: HelixEvent) {
            published.add(event)
        }

        @Synchronized
        override fun publishAsync(event: HelixEvent) {
            published.add(event)
        }

        override fun <T : HelixEvent> subscribe(eventType: KClass<T>, handler: EventHandler<T>): Subscription =
            NoopSubscription

        override fun subscribeTopic(topicPattern: String, handler: EventHandler<HelixEvent>): Subscription =
            NoopSubscription

        override fun activeSubscriptionCount(): Int = 0

        @Synchronized
        fun configChangeEvents(): List<ConfigChangeEvent> = published.filterIsInstance<ConfigChangeEvent>()

        private object NoopSubscription : Subscription {
            override val isActive: Boolean = false
            override fun unsubscribe() {}
        }
    }

    private class FakeLogger(override val name: String) : Logger {
        override fun isEnabledFor(level: LogLevel): Boolean = true
        override fun trace(message: String, metadata: Map<String, Any?>) {}
        override fun debug(message: String, metadata: Map<String, Any?>) {}
        override fun info(message: String, metadata: Map<String, Any?>) {}
        override fun warn(message: String, metadata: Map<String, Any?>) {}
        override fun error(message: String, throwable: Throwable?, metadata: Map<String, Any?>) {}
        override fun fatal(message: String, throwable: Throwable?, metadata: Map<String, Any?>) {}
    }

    private class FakeLoggerFactory : LoggerFactory {
        override fun getLogger(name: String): Logger = FakeLogger(name)
        override fun addSink(sink: LogSink) {}
        override fun removeSink(sink: LogSink) {}
        override fun setGlobalMinimumLevel(level: LogLevel) {}
    }

    private fun newManager(eventBus: FakeEventBus = FakeEventBus()): Pair<DefaultConfigManager, FakeEventBus> {
        val manager = DefaultConfigManager(eventBus, FakeLoggerFactory())
        return manager to eventBus
    }

    // -----------------------------------------------------------------
    // 1. Single source, no overlap
    // -----------------------------------------------------------------

    @Test
    fun `single source values are readable via getString`() {
        val (manager, _) = newManager()
        manager.addSource(FakeConfigSource("only", priority = 100, data = mapOf("a" to "1")))

        assertEquals("1", manager.getString("a"))
    }

    // -----------------------------------------------------------------
    // 2. Higher priority overrides lower priority on key collision
    // -----------------------------------------------------------------

    @Test
    fun `higher priority source overrides lower priority source on key collision`() {
        val (manager, _) = newManager()
        manager.addSource(FakeConfigSource("low", priority = 100, data = mapOf("k" to "low-value")))
        manager.addSource(FakeConfigSource("high", priority = 200, data = mapOf("k" to "high-value")))

        assertEquals("high-value", manager.getString("k"))
    }

    @Test
    fun `higher priority source overrides lower priority source regardless of addition order`() {
        val (manager, _) = newManager()
        // Add the higher-priority one first this time.
        manager.addSource(FakeConfigSource("high", priority = 200, data = mapOf("k" to "high-value")))
        manager.addSource(FakeConfigSource("low", priority = 100, data = mapOf("k" to "low-value")))

        assertEquals("high-value", manager.getString("k"))
    }

    // -----------------------------------------------------------------
    // 3. Non-overlapping keys from multiple sources
    // -----------------------------------------------------------------

    @Test
    fun `non-overlapping keys from multiple sources are all present`() {
        val (manager, _) = newManager()
        manager.addSource(FakeConfigSource("a", priority = 100, data = mapOf("x" to "1")))
        manager.addSource(FakeConfigSource("b", priority = 200, data = mapOf("y" to "2")))

        assertEquals("1", manager.getString("x"))
        assertEquals("2", manager.getString("y"))
        assertEquals(setOf("x", "y"), manager.allKeys())
    }

    // -----------------------------------------------------------------
    // 4. Same priority: later-added source wins (stable sort + fold semantics)
    // -----------------------------------------------------------------

    @Test
    fun `when priorities tie, the later-added source wins on key collision`() {
        val (manager, _) = newManager()
        manager.addSource(FakeConfigSource("first", priority = 100, data = mapOf("k" to "first-value")))
        manager.addSource(FakeConfigSource("second", priority = 100, data = mapOf("k" to "second-value")))

        assertEquals("second-value", manager.getString("k"))
    }

    // -----------------------------------------------------------------
    // 5. reload() with unchanged data publishes no additional event
    // -----------------------------------------------------------------

    @Test
    fun `reload with unchanged source data does not publish an additional event`() {
        val (manager, bus) = newManager()
        manager.addSource(FakeConfigSource("a", priority = 100, data = mapOf("k" to "v")))
        val eventCountAfterAdd = bus.configChangeEvents().size

        manager.reload() // same data, nothing changed

        assertEquals(eventCountAfterAdd, bus.configChangeEvents().size)
    }

    // -----------------------------------------------------------------
    // 6. reload() after a source's data changes
    // -----------------------------------------------------------------

    @Test
    fun `reload after a source's data changes produces the correct changedKeys`() {
        val (manager, bus) = newManager()
        val mutableSource = object : ConfigSource {
            override val name = "mutable"
            override val priority = 100
            var current: Map<String, String> = mapOf("k1" to "v1")
            override fun load(): Map<String, String> = current
        }
        manager.addSource(mutableSource)
        bus.published.clear()

        mutableSource.current = mapOf("k1" to "v1-changed", "k2" to "v2")
        manager.reload()

        val event = bus.configChangeEvents().single()
        assertEquals(setOf("k1", "k2"), event.changedKeys)
        assertEquals("v1-changed", manager.getString("k1"))
        assertEquals("v2", manager.getString("k2"))
    }

    // -----------------------------------------------------------------
    // 7. First addSource() ever still fires an event (previous snapshot was empty)
    // -----------------------------------------------------------------

    @Test
    fun `the very first addSource publishes a change event even though there was no prior snapshot`() {
        val (manager, bus) = newManager()
        manager.addSource(FakeConfigSource("first", priority = 100, data = mapOf("k" to "v")))

        val event = bus.configChangeEvents().single()
        assertEquals(setOf("k"), event.changedKeys)
    }

    // -----------------------------------------------------------------
    // 8. Typed getters on valid values
    // -----------------------------------------------------------------

    @Test
    fun `typed getters parse valid values correctly`() {
        val (manager, _) = newManager()
        manager.addSource(
            FakeConfigSource(
                "typed", priority = 100,
                data = mapOf("int" to "42", "long" to "9000000000", "bool" to "true")
            )
        )

        assertEquals(42, manager.getInt("int"))
        assertEquals(9_000_000_000L, manager.getLong("long"))
        assertEquals(true, manager.getBoolean("bool"))
    }

    // -----------------------------------------------------------------
    // 9. Typed getters on unparseable values fall back to default
    // -----------------------------------------------------------------

    @Test
    fun `typed getters fall back to default when the stored value does not parse`() {
        val (manager, _) = newManager()
        manager.addSource(
            FakeConfigSource(
                "bad", priority = 100,
                data = mapOf("int" to "not-a-number", "long" to "also-not", "bool" to "not-a-bool")
            )
        )

        assertEquals(-1, manager.getInt("int", default = -1))
        assertEquals(-1L, manager.getLong("long", default = -1L))
        assertEquals(false, manager.getBoolean("bool", default = false))
    }

    // -----------------------------------------------------------------
    // 10. getBoolean is case-sensitive (toBooleanStrictOrNull semantics)
    // -----------------------------------------------------------------

    @Test
    fun `getBoolean is case-sensitive and rejects non-lowercase true false`() {
        val (manager, _) = newManager()
        manager.addSource(
            FakeConfigSource(
                "case", priority = 100,
                data = mapOf("upper" to "TRUE", "capitalized" to "True", "lower" to "true")
            )
        )

        assertEquals(true, manager.getBoolean("lower"))
        assertEquals(false, manager.getBoolean("upper", default = false))
        assertEquals(false, manager.getBoolean("capitalized", default = false))
    }

    // -----------------------------------------------------------------
    // 11. Missing key, no default
    // -----------------------------------------------------------------

    @Test
    fun `missing key with no default returns null for every typed getter`() {
        val (manager, _) = newManager()

        assertNull(manager.getString("missing"))
        assertNull(manager.getInt("missing"))
        assertNull(manager.getLong("missing"))
        assertNull(manager.getBoolean("missing"))
    }

    // -----------------------------------------------------------------
    // 12. containsKey true but typed getter still falls back to default
    // -----------------------------------------------------------------

    @Test
    fun `containsKey is true for an unparseable value while the typed getter still returns default`() {
        val (manager, _) = newManager()
        manager.addSource(FakeConfigSource("a", priority = 100, data = mapOf("k" to "not-an-int")))

        assertTrue(manager.containsKey("k"))
        assertEquals(-1, manager.getInt("k", default = -1))
    }

    @Test
    fun `containsKey is false for a key that was never added`() {
        val (manager, _) = newManager()
        assertFalse(manager.containsKey("nope"))
    }

    // -----------------------------------------------------------------
    // 13. allKeys() reflects merged state across sources
    // -----------------------------------------------------------------

    @Test
    fun `allKeys reflects the merged state, not any single source`() {
        val (manager, _) = newManager()
        manager.addSource(FakeConfigSource("a", priority = 100, data = mapOf("x" to "1")))
        manager.addSource(FakeConfigSource("b", priority = 200, data = mapOf("y" to "2", "z" to "3")))

        assertEquals(setOf("x", "y", "z"), manager.allKeys())
    }

    // -----------------------------------------------------------------
    // 14. A source whose load() throws is caught, logged, contributes nothing
    // -----------------------------------------------------------------

    @Test
    fun `a source that throws on load is skipped without crashing reload`() {
        val (manager, _) = newManager()
        manager.addSource(FakeConfigSource("good", priority = 100, data = mapOf("k" to "v")))
        manager.addSource(FakeConfigSource("bad", priority = 200, data = emptyMap(), throwOnLoad = true))

        // reload() ran as part of the second addSource() call; it must not
        // have thrown, and the good source's data must still be present.
        assertEquals("v", manager.getString("k"))
    }

    // -----------------------------------------------------------------
    // 15. ConfigChangeEvent published with correct changedKeys and shape
    // -----------------------------------------------------------------

    @Test
    fun `ConfigChangeEvent carries the correct topic and source defaults`() {
        val (manager, bus) = newManager()
        manager.addSource(FakeConfigSource("a", priority = 100, data = mapOf("k" to "v")))

        val event = bus.configChangeEvents().single()
        assertEquals("helix.config.changed", event.topic)
        assertEquals("helix.config", event.source)
        assertEquals(setOf("k"), event.changedKeys)
    }

    // -----------------------------------------------------------------
    // 16. Concurrent addSource/reload does not corrupt state
    // -----------------------------------------------------------------

    @Test
    fun `concurrent addSource calls do not corrupt the final merged snapshot`() {
        val (manager, _) = newManager()
        val threadCount = 8
        val pool = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)

        repeat(threadCount) { index ->
            pool.submit {
                startLatch.await()
                // Every source shares one overlapping key "shared" plus a
                // key unique to itself, so the final state is verifiable
                // regardless of interleaving.
                manager.addSource(
                    FakeConfigSource(
                        name = "source-$index",
                        priority = index,
                        data = mapOf("shared" to "value-from-$index", "unique-$index" to "u$index")
                    )
                )
                doneLatch.countDown()
            }
        }

        startLatch.countDown()
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "concurrent addSource calls did not finish in time")
        pool.shutdown()

        // Highest priority among 0..threadCount-1 is threadCount-1, so it
        // must have won the "shared" key regardless of thread interleaving.
        assertEquals("value-from-${threadCount - 1}", manager.getString("shared"))
        // Every thread's unique key must be present -- no lost updates.
        repeat(threadCount) { index ->
            assertEquals("u$index", manager.getString("unique-$index"))
        }
    }
}