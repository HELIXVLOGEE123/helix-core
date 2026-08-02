rootProject.name = "helix-core"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

// ---------------------------------------------------------------------------
// HELIX Core module map.
// Every subsystem is split into an `-api` module (pure contracts, zero
// runtime dependencies on implementation details) and a `-runtime` module
// (the default, swappable implementation). `helix-platform` is the
// composition root that wires everything together into a single HelixCore
// facade. No feature module (Launcher, Voice, AI, Automation, Vision,
// Memory) lives in this repository — they consume helix-platform as an
// external dependency and register themselves via the Plugin Manager.
// ---------------------------------------------------------------------------
include(
    ":helix-logging-api",
    ":helix-logging-runtime",
    ":helix-eventbus-api",
    ":helix-eventbus-runtime",
    ":helix-config-api",
    ":helix-config-runtime",
    ":helix-security-api",
    ":helix-security-runtime",
    ":helix-storage-api",
    ":helix-storage-runtime",
    ":helix-scheduler-api",
    ":helix-scheduler-runtime",
    ":helix-registry-api",
    ":helix-registry-runtime",
    ":helix-lifecycle-api",
    ":helix-lifecycle-runtime",
    ":helix-plugin-api",
    ":helix-plugin-runtime",
    ":helix-platform"
)
