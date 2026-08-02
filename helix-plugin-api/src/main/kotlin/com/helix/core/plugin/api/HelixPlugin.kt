package com.helix.core.plugin.api

/**
 * Extension point implemented by every feature module (Launcher, Voice
 * Engine, AI Engine, Automation, Vision, Memory, ...). HELIX Core never
 * references any of these modules by name or type — it only ever holds a
 * `HelixPlugin` reference, which is what keeps Core decoupled from every
 * concrete feature.
 */
public interface HelixPlugin {
    public val descriptor: PluginDescriptor

    public fun onLoad(context: PluginContext)
    public fun onUnload()
}
