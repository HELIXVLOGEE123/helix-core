package com.helix.core.eventbus.api

import java.time.Instant

/**
 * Base contract for every event published on the HELIX event bus. All
 * cross-module communication that is not a direct interface call MUST be
 * expressed as a HelixEvent — this is the mechanism that lets HELIX Core
 * stay decoupled from UI, AI, Launcher, and Voice implementations while
 * still letting them react to platform state.
 */
public interface HelixEvent {
    /** Dot-delimited topic, e.g. "helix.lifecycle.component.started". */
    public val topic: String
    public val timestamp: Instant
    /** Logical identifier of whatever published this event (module/component name). */
    public val source: String
}
