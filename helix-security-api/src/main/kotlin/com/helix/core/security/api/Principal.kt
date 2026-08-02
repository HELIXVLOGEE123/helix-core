package com.helix.core.security.api

/** Represents "who" is acting — a human user, a plugin, an automation, or the system itself. */
public interface Principal {
    public val id: String
    public val roles: Set<String>
}
