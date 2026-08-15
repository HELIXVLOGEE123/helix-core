// helix-ui — root build file. Applies plugins at root scope (apply false)
// so every module inherits identical, deliberately version-locked tooling.
// Mirrors the convention already established in HELIX Core's own root
// build.gradle.kts, adapted for an Android/Compose target instead of a
// pure-JVM one.

plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
