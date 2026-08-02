package com.helix.core.security.api

/** Ambient "who is currently acting" context, scoped per call chain. */
public interface SecurityContext {
    public val principal: Principal
    public fun hasPermission(permission: Permission): Boolean
}
