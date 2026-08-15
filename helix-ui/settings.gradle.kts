rootProject.name = "helix-ui"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// ---------------------------------------------------------------------------
// helix-ui — separate repository from HELIX Core (helix-platform).
//
// Per ARCHITECTURAL_RECONNAISSANCE (see docs/DECISIONS.md): HELIX Core is a
// pure Kotlin/JVM backend platform (org.jetbrains.kotlin.jvm only, zero
// Android/Compose dependency anywhere). This repository holds everything on
// the other side of that boundary — Compose UI, Android framework APIs, and
// anything that needs to render. It depends on Core (once Core is published
// as an artifact); Core must never depend on this repository.
// ---------------------------------------------------------------------------
include(":neuralstrand")
