package com.helix.core.config.runtime

import com.helix.core.config.api.ConfigChangeEvent
import com.helix.core.config.api.ConfigManager
import com.helix.core.config.api.ConfigSource
import com.helix.core.eventbus.api.EventBus
import com.helix.core.logging.api.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

/**
 * Merges all registered [ConfigSource]s by ascending priority (higher
 * priority wins on key collision) and caches the result. Thread-safe:
 * reads see a consistent snapshot even while [reload] is running.
 */
public class DefaultConfigManager(
    private val eventBus: EventBus,
    loggerFactory: LoggerFactory
) : ConfigManager {

    private val logger = loggerFactory.getLogger("helix.config")
    private val sources = CopyOnWriteArrayList<ConfigSource>()
    private val snapshot = AtomicReference<Map<String, String>>(emptyMap())

    override fun getString(key: String, default: String?): String? = snapshot.get()[key] ?: default

    override fun getInt(key: String, default: Int?): Int? = snapshot.get()[key]?.toIntOrNull() ?: default

    override fun getLong(key: String, default: Long?): Long? = snapshot.get()[key]?.toLongOrNull() ?: default

    override fun getBoolean(key: String, default: Boolean?): Boolean? =
        snapshot.get()[key]?.toBooleanStrictOrNull() ?: default

    override fun containsKey(key: String): Boolean = snapshot.get().containsKey(key)

    override fun allKeys(): Set<String> = snapshot.get().keys

    override fun addSource(source: ConfigSource) {
        sources.add(source)
        reload()
    }

    override fun reload() {
        val merged = sources.sortedBy { it.priority }
            .fold(emptyMap<String, String>()) { acc, source ->
                acc + runCatching { source.load() }.getOrElse {
                    logger.warn("Config source '${source.name}' failed to load; skipping", mapOf("error" to it.message))
                    emptyMap()
                }
            }
        val previous = snapshot.getAndSet(merged)
        val changedKeys = (previous.keys + merged.keys).filterTo(mutableSetOf()) { key ->
            previous[key] != merged[key]
        }
        if (changedKeys.isNotEmpty()) {
            logger.info("Configuration reloaded", mapOf("changedKeyCount" to changedKeys.size))
            eventBus.publish(ConfigChangeEvent(changedKeys))
        }
    }
}
