package com.helix.core.security.api

/**
 * Pluggable authorization rule. [SecurityManager] evaluates every
 * registered policy for a request and denies unless at least one policy
 * grants and none explicitly deny (deny-overrides).
 */
public fun interface AuthorizationPolicy {
    public fun evaluate(principal: Principal, permission: Permission): AccessDecision
}
