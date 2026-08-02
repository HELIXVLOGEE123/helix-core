package com.helix.core.logging.runtime

import com.helix.core.logging.api.LogLevel
import com.helix.core.logging.api.LogRecord
import com.helix.core.logging.api.Logger
import java.time.Instant

/**
 * Default [Logger] implementation. Stateless aside from its name; all
 * dispatch/filtering logic is delegated back to the owning
 * [DefaultLoggerFactory] so sink configuration changes apply immediately
 * to every already-obtained Logger instance.
 */
internal class DefaultLogger(
    override val name: String,
    private val dispatch: (LogRecord) -> Unit,
    private val enabled: (LogLevel) -> Boolean
) : Logger {

    override fun isEnabledFor(level: LogLevel): Boolean = enabled(level)

    private fun emit(level: LogLevel, message: String, throwable: Throwable?, metadata: Map<String, Any?>) {
        if (!isEnabledFor(level)) return
        dispatch(LogRecord(Instant.now(), level, name, message, throwable, metadata))
    }

    override fun trace(message: String, metadata: Map<String, Any?>) = emit(LogLevel.TRACE, message, null, metadata)
    override fun debug(message: String, metadata: Map<String, Any?>) = emit(LogLevel.DEBUG, message, null, metadata)
    override fun info(message: String, metadata: Map<String, Any?>) = emit(LogLevel.INFO, message, null, metadata)
    override fun warn(message: String, metadata: Map<String, Any?>) = emit(LogLevel.WARN, message, null, metadata)
    override fun error(message: String, throwable: Throwable?, metadata: Map<String, Any?>) =
        emit(LogLevel.ERROR, message, throwable, metadata)
    override fun fatal(message: String, throwable: Throwable?, metadata: Map<String, Any?>) =
        emit(LogLevel.FATAL, message, throwable, metadata)
}
