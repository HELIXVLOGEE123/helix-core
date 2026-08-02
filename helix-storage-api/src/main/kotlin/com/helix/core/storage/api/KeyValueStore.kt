package com.helix.core.storage.api

/**
 * A single namespaced key/value store. Values are opaque strings at this
 * layer to keep the abstraction storage-engine-agnostic (in-memory, file,
 * SQL, cloud); modules that need structured data should serialize
 * (e.g. JSON) before calling [put] and deserialize after [get].
 */
public interface KeyValueStore {
    public val namespace: String
    public fun get(key: String): String?
    public fun put(key: String, value: String)
    public fun delete(key: String)
    public fun keys(): Set<String>
    public fun clear()
}
