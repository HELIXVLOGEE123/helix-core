package com.helix.core.storage.runtime

import com.helix.core.logging.api.LoggerFactory
import com.helix.core.storage.api.KeyValueStore
import com.helix.core.storage.api.StorageException
import com.helix.core.storage.api.StorageManager
import com.helix.core.storage.api.StorageProvider
import java.util.concurrent.ConcurrentHashMap

/**
 * Default [StorageManager]. Ships with [InMemoryStorageProvider]
 * pre-registered under [StorageManager.DEFAULT_PROVIDER_ID] so the platform
 * is usable out of the box; additional providers are added via
 * [registerProvider].
 */
public class DefaultStorageManager(
    loggerFactory: LoggerFactory
) : StorageManager {

    private val logger = loggerFactory.getLogger("helix.storage")
    private val providers = ConcurrentHashMap<String, StorageProvider>()

    init {
        registerProvider(InMemoryStorageProvider())
    }

    override fun registerProvider(provider: StorageProvider) {
        providers[provider.id] = provider
        logger.info("Storage provider registered", mapOf("providerId" to provider.id))
    }

    override fun getStore(namespace: String, providerId: String): KeyValueStore {
        val provider = providers[providerId]
            ?: throw StorageException("No storage provider registered with id '$providerId'")
        return provider.openStore(namespace)
    }
}
