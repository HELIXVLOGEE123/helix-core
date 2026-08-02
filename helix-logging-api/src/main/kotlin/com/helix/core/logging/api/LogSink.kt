package com.helix.core.logging.api

/**
 * A destination for log records (console, file, remote collector, etc).
 * Sinks must be safe to invoke concurrently from multiple threads and must
 * never throw — the logging runtime treats a throwing sink as a bug and
 * will isolate/disable it rather than crash the caller.
 */
public interface LogSink {
    public val minimumLevel: LogLevel
    public fun write(record: LogRecord)
    public fun flush() {}
    public fun close() {}
}
