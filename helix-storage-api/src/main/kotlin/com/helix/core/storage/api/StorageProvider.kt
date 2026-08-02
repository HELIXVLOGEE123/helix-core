package com.helix.core.storage.api

/** A backend capable of opening [KeyValueStore]s, e.g. in-memory, file-based, or a remote DB adapter. */
public interface StorageProvider {
    public val id: String
    public fun openStore(namespace: String): KeyValueStore
}
