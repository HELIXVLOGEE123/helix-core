package com.helix.core.storage.runtime

import com.helix.core.storage.api.KeyValueStore
import com.helix.core.storage.api.StorageProvider
import java.util.concurrent.ConcurrentHashMap

/**
 * Default, always-available storage provider backed by in-process maps.
 * Not persistent across restarts — suitable for tests and early bring-up.
 * Production deployments register a durable [StorageProvider]
 * (file/SQL/cloud) and pass its id explicitly to
 * [com.helix.core.storage.api.StorageManager.getStore].
 */
public class InMemoryStorageProvider : StorageProvider {
    override val id: String = "in-memory"

    private val stores = ConcurrentHashMap<String, KeyValueStore>()

    override fun openStore(namespace: String): KeyValueStore =
        stores.computeIfAbsent(namespace) { InMemoryKeyValueStore(it) }

    private class InMemoryKeyValueStore(override val namespace: String) : KeyValueStore {
        private val data = ConcurrentHashMap<String, String>()
        override fun get(key: String): String? = data[key]
        override fun put(key: String, value: String) { data[key] = value }
        override fun delete(key: String) { data.remove(key) }
        override fun keys(): Set<String> = data.keys.toSet()
        override fun clear() = data.clear()
    }
}
