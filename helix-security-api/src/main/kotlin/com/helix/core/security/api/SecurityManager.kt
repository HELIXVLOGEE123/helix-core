package com.helix.core.security.api

/**
 * Central authentication/authorization entry point. Deliberately minimal
 * at the platform layer — concrete auth mechanisms (OAuth, device pairing,
 * biometrics for a Voice module, etc.) are supplied by feature modules as
 * additional [AuthorizationPolicy] registrations, never by modifying this
 * interface.
 */
public interface SecurityManager {
    public fun currentContext(): SecurityContext?
    public fun runAs(principal: Principal, block: () -> Unit)
    public fun authorize(principal: Principal, permission: Permission): AccessDecision
    public fun registerPolicy(policy: AuthorizationPolicy)
}
