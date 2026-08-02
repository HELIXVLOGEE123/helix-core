package com.helix.core.config.runtime

import com.helix.core.config.api.ConfigSource

/** Reads process environment variables. Default priority: 100 (low — easily overridden). */
public class EnvConfigSource(override val priority: Int = 100) : ConfigSource {
    override val name: String = "environment"
    override fun load(): Map<String, String> = System.getenv().toMap()
}
