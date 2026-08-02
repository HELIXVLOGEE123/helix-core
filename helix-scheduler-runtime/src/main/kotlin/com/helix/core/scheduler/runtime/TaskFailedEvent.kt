package com.helix.core.scheduler.runtime

import com.helix.core.eventbus.api.HelixEvent
import java.time.Instant

public data class TaskFailedEvent(
    val taskName: String,
    val error: String,
    override val timestamp: Instant = Instant.now(),
    override val source: String = "helix.scheduler"
) : HelixEvent {
    override val topic: String = "helix.scheduler.task_failed"
}
