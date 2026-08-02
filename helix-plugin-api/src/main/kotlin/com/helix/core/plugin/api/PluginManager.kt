package com.helix.core.plugin.api

/**
 * Loads/unloads [HelixPlugin] instances and tracks them by id. This is the
 * ONLY supported way for a feature module to attach to HELIX Core — there
 * is no other integration seam, which is what enforces the "Core does not
 * depend on Launcher/AI/Voice" rule structurally rather than by convention.
 */
public interface PluginManager {
    public fun loadPlugin(plugin: HelixPlugin)
    public fun unloadPlugin(id: String)
    public fun getPlugin(id: String): HelixPlugin?
    public fun listPlugins(): List<PluginDescriptor>
}
