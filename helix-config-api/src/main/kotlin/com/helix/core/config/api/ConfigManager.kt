package com.helix.core.config.api

/**
 * Read access to merged platform configuration, with typed convenience
 * getters. Configuration is intentionally string-keyed and flat at this
 * layer; feature modules should define their own typed config data classes
 * on top of this if they need structure.
 */
public interface ConfigManager {

    public fun getString(key: String, default: String? = null): String?
    public fun getInt(key: String, default: Int? = null): Int?
    public fun getLong(key: String, default: Long? = null): Long?
    public fun getBoolean(key: String, default: Boolean? = null): Boolean?

    public fun containsKey(key: String): Boolean
    public fun allKeys(): Set<String>

    public fun addSource(source: ConfigSource)

    /** Re-reads every registered source, re-merges, and publishes [ConfigChangeEvent] if anything changed. */
    public fun reload()
}
