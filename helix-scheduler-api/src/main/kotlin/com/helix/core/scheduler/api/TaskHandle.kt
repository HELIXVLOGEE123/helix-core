package com.helix.core.scheduler.api

public interface TaskHandle {
    public val isCancelled: Boolean
    public fun cancel()
}
