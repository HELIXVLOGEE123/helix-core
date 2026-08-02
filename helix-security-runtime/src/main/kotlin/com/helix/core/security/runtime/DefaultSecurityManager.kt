package com.helix.core.security.runtime

import com.helix.core.eventbus.api.EventBus
import com.helix.core.eventbus.api.HelixEvent
import com.helix.core.logging.api.LoggerFactory
import com.helix.core.security.api.AccessDecision
import com.helix.core.security.api.AuthorizationPolicy
import com.helix.core.security.api.Permission
import com.helix.core.security.api.Principal
import com.helix.core.security.api.SecurityContext
import com.helix.core.security.api.SecurityManager
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

/** Published whenever [SecurityManager.authorize] returns DENIED, for auditing. */
public data class AccessDeniedEvent(
    val principalId: String,
    val permission: String,
    override val timestamp: Instant = Instant.now(),
    override val source: String = "helix.security"
) : HelixEvent {
    override val topic: String = "helix.security.access_denied"
}

/**
 * Deny-overrides evaluation across all registered policies. [runAs] uses a
 * ThreadLocal-backed context so nested/async work can still ask "who is
 * currently acting" without explicit parameter threading, while remaining
 * safe under HELIX's single-writer-per-thread threading model.
 */
public class DefaultSecurityManager(
    private val eventBus: EventBus,
    loggerFactory: LoggerFactory
) : SecurityManager {

    private val logger = loggerFactory.getLogger("helix.security")
    private val policies = CopyOnWriteArrayList<AuthorizationPolicy>()
    private val contextHolder = ThreadLocal<SecurityContext?>()

    override fun currentContext(): SecurityContext? = contextHolder.get()

    override fun runAs(principal: Principal, block: () -> Unit) {
        val previous = contextHolder.get()
        contextHolder.set(DefaultSecurityContext(principal, this))
        try {
            block()
        } finally {
            contextHolder.set(previous)
        }
    }

    override fun authorize(principal: Principal, permission: Permission): AccessDecision {
        if (policies.isEmpty()) return AccessDecision.DENIED
        var granted = false
        for (policy in policies) {
            when (policy.evaluate(principal, permission)) {
                AccessDecision.DENIED -> {
                    logger.warn("Access denied", mapOf("principal" to principal.id, "permission" to permission.name))
                    eventBus.publishAsync(AccessDeniedEvent(principal.id, permission.name))
                    return AccessDecision.DENIED
                }
                AccessDecision.GRANTED -> granted = true
            }
        }
        return if (granted) AccessDecision.GRANTED else AccessDecision.DENIED
    }

    override fun registerPolicy(policy: AuthorizationPolicy) {
        policies.add(policy)
    }

    private class DefaultSecurityContext(
        override val principal: Principal,
        private val manager: DefaultSecurityManager
    ) : SecurityContext {
        override fun hasPermission(permission: Permission): Boolean =
            manager.authorize(principal, permission) == AccessDecision.GRANTED
    }
}
