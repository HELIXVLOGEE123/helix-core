package com.helix.core.logging.api

/**
 * Central entry point for obtaining [Logger] instances and configuring
 * where log output goes. HELIX Core provides exactly one instance of this,
 * resolved through the ServiceRegistry; feature modules never construct
 * their own logging pipeline.
 */
public interface LoggerFactory {
    public fun getLogger(name: String): Logger
    public fun addSink(sink: LogSink)
    public fun removeSink(sink: LogSink)
    public fun setGlobalMinimumLevel(level: LogLevel)
}
