package com.helix.core.registry.api

import com.helix.core.eventbus.api.HelixEvent
import java.time.Instant

public data class ServiceRegisteredEvent(
    val serviceType: String,
    val name: String,
    override val timestamp: Instant = Instant.now(),
    override val source: String = "helix.registry"
) : HelixEvent {
    override val topic: String = "helix.registry.service_registered"
}

public data class ServiceUnregisteredEvent(
    val serviceType: String,
    val name: String,
    override val timestamp: Instant = Instant.now(),
    override val source: String = "helix.registry"
) : HelixEvent {
    override val topic: String = "helix.registry.service_unregistered"
}
