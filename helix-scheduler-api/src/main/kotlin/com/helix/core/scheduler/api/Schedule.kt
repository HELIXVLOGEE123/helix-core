package com.helix.core.scheduler.api

import java.time.Duration

/** How a [Task] should be scheduled. Sealed so the runtime can exhaustively handle every case. */
public sealed class Schedule {
    public data class Once(val delay: Duration) : Schedule()
    public data class FixedDelay(val initialDelay: Duration, val delay: Duration) : Schedule()
    public data class FixedRate(val initialDelay: Duration, val period: Duration) : Schedule()
}
