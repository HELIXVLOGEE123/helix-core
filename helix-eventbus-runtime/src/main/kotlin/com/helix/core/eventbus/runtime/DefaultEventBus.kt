package com.helix.core.eventbus.runtime

import com.helix.core.eventbus.api.EventBus
import com.helix.core.eventbus.api.EventHandler
import com.helix.core.eventbus.api.HelixEvent
import com.helix.core.eventbus.api.Subscription
import com.helix.core.logging.api.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.reflect.KClass

/**
 * Default, in-process [EventBus]. Type-based subscriptions are stored per
 * [KClass]; topic-based subscriptions use simple glob matching (`*` as a
 * single-segment wildcard, e.g. "helix.lifecycle.*"). Both dispatch through
 * the same isolated-failure path.
 */
public class DefaultEventBus(
    loggerFactory: LoggerFactory,
    private val asyncExecutor: ExecutorService = Executors.newCachedThreadPool { r ->
        Thread(r, "helix-eventbus-async").apply { isDaemon = true }
    }
) : EventBus {

    private val logger = loggerFactory.getLogger("helix.eventbus")

    private val typeSubscribers = ConcurrentHashMap<KClass<*>, CopyOnWriteArrayList<HandlerSubscription<*>>>()
    private val topicSubscribers = CopyOnWriteArrayList<TopicSubscription>()

    override fun publish(event: HelixEvent): Unit = dispatch(event)

    override fun publishAsync(event: HelixEvent) {
        asyncExecutor.submit { dispatch(event) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : HelixEvent> subscribe(eventType: KClass<T>, handler: EventHandler<T>): Subscription {
        val list = typeSubscribers.computeIfAbsent(eventType) { CopyOnWriteArrayList() }
        val sub = HandlerSubscription(eventType, handler as EventHandler<HelixEvent>) { s -> list.remove(s) }
        list.add(sub)
        return sub
    }

    override fun subscribeTopic(topicPattern: String, handler: EventHandler<HelixEvent>): Subscription {
        val sub = TopicSubscription(topicPattern, handler) { s -> topicSubscribers.remove(s) }
        topicSubscribers.add(sub)
        return sub
    }

    override fun activeSubscriptionCount(): Int =
        typeSubscribers.values.sumOf { it.size } + topicSubscribers.size

    private fun dispatch(event: HelixEvent) {
        typeSubscribers[event::class]?.forEach { sub -> safeInvoke(sub, event) }
        topicSubscribers.forEach { sub ->
            if (matches(sub.pattern, event.topic)) safeInvoke(sub, event)
        }
    }

    private fun safeInvoke(sub: Subscription, event: HelixEvent) {
        val handler = when (sub) {
            is HandlerSubscription<*> -> sub.handler
            is TopicSubscription -> sub.handler
            else -> return
        }
        try {
            handler.handle(event)
        } catch (t: Throwable) {
            logger.error(
                "Subscriber threw while handling event on topic '${event.topic}'",
                throwable = t,
                metadata = mapOf("eventType" to event::class.simpleName)
            )
        }
    }

    private fun matches(pattern: String, topic: String): Boolean {
        if (pattern == topic) return true
        val patternSegments = pattern.split(".")
        val topicSegments = topic.split(".")
        if (patternSegments.size != topicSegments.size) return false
        return patternSegments.zip(topicSegments).all { (p, t) -> p == "*" || p == t }
    }

    private class HandlerSubscription<T : HelixEvent>(
        val eventType: KClass<T>,
        val handler: EventHandler<HelixEvent>,
        private val onUnsubscribe: (HandlerSubscription<*>) -> Unit
    ) : Subscription {
        @Volatile override var isActive: Boolean = true
            private set

        override fun unsubscribe() {
            if (!isActive) return
            isActive = false
            onUnsubscribe(this)
        }
    }

    private class TopicSubscription(
        val pattern: String,
        val handler: EventHandler<HelixEvent>,
        private val onUnsubscribe: (TopicSubscription) -> Unit
    ) : Subscription {
        @Volatile override var isActive: Boolean = true
            private set

        override fun unsubscribe() {
            if (!isActive) return
            isActive = false
            onUnsubscribe(this)
        }
    }
}
