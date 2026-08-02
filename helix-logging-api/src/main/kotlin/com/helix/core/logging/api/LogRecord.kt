package com.helix.core.logging.api

import java.time.Instant

/**
 * A single, immutable log entry. [LogSink] implementations receive these
 * and are responsible for formatting/persisting them; the logging API
 * itself never dictates output format.
 */
public data class LogRecord(
    val timestamp: Instant,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
    val metadata: Map<String, Any?> = emptyMap()
)
