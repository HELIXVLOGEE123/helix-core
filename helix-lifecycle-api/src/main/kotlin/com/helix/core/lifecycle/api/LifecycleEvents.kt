package com.helix.core.lifecycle.api

import com.helix.core.eventbus.api.HelixEvent
import java.time.Instant

public data class ComponentInitializedEvent(
    val componentName: String,
    override val timestamp: Instant = Instant.now(),
    override val source: String = "helix.lifecycle"
) : HelixEvent {
    override val topic: String = "helix.lifecycle.component.initialized"
}

public data class ComponentStartedEvent(
    val componentName: String,
    override val timestamp: Instant = Instant.now(),
    override val source: String = "helix.lifecycle"
) : HelixEvent {
    override val topic: String = "helix.lifecycle.component.started"
}

public data class ComponentStoppedEvent(
    val componentName: String,
    override val timestamp: Instant = Instant.now(),
    override val source: String = "helix.lifecycle"
) : HelixEvent {
    override val topic: String = "helix.lifecycle.component.stopped"
}

public data class ComponentFailedEvent(
    val componentName: String,
    val phase: LifecycleState,
    val error: String,
    override val timestamp: Instant = Instant.now(),
    override val source: String = "helix.lifecycle"
) : HelixEvent {
    override val topic: String = "helix.lifecycle.component.failed"
}
