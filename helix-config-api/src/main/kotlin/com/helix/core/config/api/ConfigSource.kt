package com.helix.core.config.api

/**
 * A source of raw configuration key/value pairs (env vars, a properties
 * file, a remote config service, CLI flags, ...). Higher [priority] sources
 * override lower ones when [ConfigManager] merges them.
 */
public interface ConfigSource {
    public val name: String
    public val priority: Int
    public fun load(): Map<String, String>
}
