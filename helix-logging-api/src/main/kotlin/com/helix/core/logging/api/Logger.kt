package com.helix.core.logging.api

/**
 * Contract for a named logger. Obtain instances exclusively through
 * [LoggerFactory] — never instantiate implementations directly, so callers
 * remain decoupled from whichever [LogSink]s are configured at runtime.
 */
public interface Logger {

    public val name: String

    public fun isEnabledFor(level: LogLevel): Boolean

    public fun trace(message: String, metadata: Map<String, Any?> = emptyMap())
    public fun debug(message: String, metadata: Map<String, Any?> = emptyMap())
    public fun info(message: String, metadata: Map<String, Any?> = emptyMap())
    public fun warn(message: String, metadata: Map<String, Any?> = emptyMap())
    public fun error(message: String, throwable: Throwable? = null, metadata: Map<String, Any?> = emptyMap())
    public fun fatal(message: String, throwable: Throwable? = null, metadata: Map<String, Any?> = emptyMap())
}
