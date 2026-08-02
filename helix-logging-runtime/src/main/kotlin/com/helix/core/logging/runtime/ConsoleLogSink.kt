package com.helix.core.logging.runtime

import com.helix.core.logging.api.LogLevel
import com.helix.core.logging.api.LogRecord
import com.helix.core.logging.api.LogSink
import java.time.format.DateTimeFormatter

/**
 * Default, dependency-free sink that writes to stdout/stderr. Intended as
 * the platform's out-of-the-box sink during early bring-up; production
 * deployments are expected to add structured/file/remote sinks via
 * [com.helix.core.logging.api.LoggerFactory.addSink].
 */
public class ConsoleLogSink(
    override val minimumLevel: LogLevel = LogLevel.INFO
) : LogSink {

    private val formatter = DateTimeFormatter.ISO_INSTANT

    override fun write(record: LogRecord) {
        val line = buildString {
            append(formatter.format(record.timestamp))
            append(" [").append(record.level).append("] ")
            append(record.tag).append(" - ")
            append(record.message)
            if (record.metadata.isNotEmpty()) append(" ").append(record.metadata)
        }
        val target = if (record.level >= LogLevel.ERROR) System.err else System.out
        target.println(line)
        record.throwable?.printStackTrace(target)
    }
}
