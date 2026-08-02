package com.helix.core.config.runtime

import com.helix.core.config.api.ConfigSource
import java.io.File
import java.util.Properties

/** Reads a `.properties` file from disk. Missing file yields an empty map rather than throwing. */
public class PropertiesFileConfigSource(
    private val file: File,
    override val priority: Int = 200
) : ConfigSource {
    override val name: String = "properties:${file.path}"

    override fun load(): Map<String, String> {
        if (!file.exists()) return emptyMap()
        val props = Properties()
        file.inputStream().use { props.load(it) }
        return props.entries.associate { (k, v) -> k.toString() to v.toString() }
    }
}
