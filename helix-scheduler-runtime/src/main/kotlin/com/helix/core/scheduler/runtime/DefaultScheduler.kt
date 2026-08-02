package com.helix.core.scheduler.runtime

import com.helix.core.eventbus.api.EventBus
import com.helix.core.logging.api.LoggerFactory
import com.helix.core.scheduler.api.Schedule
import com.helix.core.scheduler.api.Scheduler
import com.helix.core.scheduler.api.Task
import com.helix.core.scheduler.api.TaskHandle
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Default [Scheduler] backed by a single [ScheduledExecutorService]. Task
 * exceptions are caught so one failing task can never kill the shared
 * executor thread; failures are logged and published as [TaskFailedEvent].
 */
public class DefaultScheduler(
    private val eventBus: EventBus,
    loggerFactory: LoggerFactory,
    poolSize: Int = 4
) : Scheduler {

    private val logger = loggerFactory.getLogger("helix.scheduler")
    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(poolSize) { r ->
        Thread(r, "helix-scheduler").apply { isDaemon = true }
    }

    override fun schedule(name: String, schedule: Schedule, task: Task): TaskHandle {
        val wrapped = Runnable {
            try {
                task.run()
            } catch (t: Throwable) {
                logger.error("Scheduled task '$name' threw", throwable = t)
                eventBus.publishAsync(TaskFailedEvent(name, t.message ?: t::class.simpleName.orEmpty()))
            }
        }

        val future: ScheduledFuture<*> = when (schedule) {
            is Schedule.Once ->
                executor.schedule(wrapped, schedule.delay.toMillis(), TimeUnit.MILLISECONDS)
            is Schedule.FixedDelay ->
                executor.scheduleWithFixedDelay(
                    wrapped, schedule.initialDelay.toMillis(), schedule.delay.toMillis(), TimeUnit.MILLISECONDS
                )
            is Schedule.FixedRate ->
                executor.scheduleAtFixedRate(
                    wrapped, schedule.initialDelay.toMillis(), schedule.period.toMillis(), TimeUnit.MILLISECONDS
                )
        }

        return object : TaskHandle {
            override val isCancelled: Boolean get() = future.isCancelled
            override fun cancel() { future.cancel(false) }
        }
    }

    override fun shutdown() {
        executor.shutdown()
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            executor.shutdownNow()
        }
    }
}
