package com.helix.core.scheduler.api

/**
 * Abstraction over "run this later / repeatedly." HELIX Core provides a
 * single, thread-pool-backed default; modules requiring OS-level scheduling
 * (e.g. an Automation module driving external cron) implement this
 * interface against their own backend and register it in the
 * ServiceRegistry instead.
 */
public interface Scheduler {
    public fun schedule(name: String, schedule: Schedule, task: Task): TaskHandle
    public fun shutdown()
}
