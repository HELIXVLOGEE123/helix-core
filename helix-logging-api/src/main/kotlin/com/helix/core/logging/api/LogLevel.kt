package com.helix.core.logging.api

/**
 * Ordered severity levels for HELIX log records. Ordinal order is used for
 * threshold comparisons, so do not reorder existing entries — append new
 * levels at the appropriate position only during a major version bump.
 */
public enum class LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    FATAL
}
