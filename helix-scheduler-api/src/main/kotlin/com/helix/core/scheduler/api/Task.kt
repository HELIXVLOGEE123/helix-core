package com.helix.core.scheduler.api

/** A unit of scheduled work. Implementations should be idempotent where possible, since retries are policy-driven. */
public fun interface Task {
    public fun run()
}
