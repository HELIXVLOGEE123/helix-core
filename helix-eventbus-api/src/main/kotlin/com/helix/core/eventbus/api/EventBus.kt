package com.helix.core.eventbus.api

import kotlin.reflect.KClass

/**
 * The single, platform-wide publish/subscribe mechanism. This is the
 * primary decoupling seam of HELIX Core: modules never hold references to
 * each other, only to the EventBus (and to whichever narrow interfaces the
 * ServiceRegistry hands them).
 *
 * Delivery semantics:
 *  - [publish] dispatches synchronously by default, on the caller's thread,
 *    to keep behaviour predictable for simple use cases.
 *  - [publishAsync] dispatches on the bus's internal executor and returns
 *    immediately; use this for anything that must not block the publisher.
 *  - Handler exceptions are always isolated: one failing subscriber never
 *    prevents delivery to the others, and never propagates to the publisher.
 */
public interface EventBus {

    public fun publish(event: HelixEvent)

    public fun publishAsync(event: HelixEvent)

    public fun <T : HelixEvent> subscribe(eventType: KClass<T>, handler: EventHandler<T>): Subscription

    public fun subscribeTopic(topicPattern: String, handler: EventHandler<HelixEvent>): Subscription

    /** Number of currently active subscriptions; primarily for diagnostics/tests. */
    public fun activeSubscriptionCount(): Int
}

/** Reified convenience so call sites read as `eventBus.subscribe<ComponentStartedEvent> { ... }`. */
public inline fun <reified T : HelixEvent> EventBus.subscribe(handler: EventHandler<T>): Subscription =
    subscribe(T::class, handler)
