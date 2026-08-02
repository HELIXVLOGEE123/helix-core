package com.helix.core.storage.api

/**
 * Registry of [StorageProvider]s and the entry point modules use to get a
 * [KeyValueStore] for their namespace, without knowing or caring which
 * backend actually persists it.
 */
public interface StorageManager {
    public fun registerProvider(provider: StorageProvider)
    public fun getStore(namespace: String, providerId: String = DEFAULT_PROVIDER_ID): KeyValueStore

    public companion object {
        public const val DEFAULT_PROVIDER_ID: String = "in-memory"
    }
}
