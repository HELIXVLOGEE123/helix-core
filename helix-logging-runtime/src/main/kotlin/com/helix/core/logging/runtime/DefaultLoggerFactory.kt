package com.helix.core.logging.runtime

import com.helix.core.logging.api.LogLevel
import com.helix.core.logging.api.LogRecord
import com.helix.core.logging.api.LogSink
import com.helix.core.logging.api.Logger
import com.helix.core.logging.api.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe default [LoggerFactory]. Fans every record out to all
 * registered sinks; a sink that throws is caught, logged once to stderr,
 * and then skipped for that record (never disabled outright — sinks may be
 * transiently failing, e.g. a network sink during an outage).
 */
public class DefaultLoggerFactory(
    initialMinimumLevel: LogLevel = LogLevel.DEBUG
) : LoggerFactory {

    private val sinks = CopyOnWriteArrayList<LogSink>()
    private val loggers = ConcurrentHashMap<String, Logger>()

    @Volatile
    private var globalMinimumLevel: LogLevel = initialMinimumLevel

    override fun getLogger(name: String): Logger =
        loggers.computeIfAbsent(name) { DefaultLogger(it, ::dispatch, ::isEnabled) }

    override fun addSink(sink: LogSink) {
        sinks.add(sink)
    }

    override fun removeSink(sink: LogSink) {
        sinks.remove(sink)
    }

    override fun setGlobalMinimumLevel(level: LogLevel) {
        globalMinimumLevel = level
    }

    private fun isEnabled(level: LogLevel): Boolean = level >= globalMinimumLevel

    private fun dispatch(record: LogRecord) {
        for (sink in sinks) {
            if (record.level < sink.minimumLevel) continue
            try {
                sink.write(record)
            } catch (t: Throwable) {
                System.err.println("[helix-logging] sink ${sink::class.simpleName} threw while writing a record: $t")
            }
        }
    }
}
