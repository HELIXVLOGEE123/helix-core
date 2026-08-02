package com.helix.core.plugin.api

/** Static metadata about a plugin, independent of whether it is currently loaded. */
public data class PluginDescriptor(
    val id: String,
    val version: String,
    val displayName: String,
    val dependsOn: List<String> = emptyList()
)
