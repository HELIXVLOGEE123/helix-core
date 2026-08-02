import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// ---------------------------------------------------------------------------
// HELIX Core — root build script.
// Applies a single, consistent build convention to every subsystem module
// so that coding standards (see docs/CODING_STANDARDS.md) are enforced
// uniformly rather than per-module.
// ---------------------------------------------------------------------------

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

val helixVersion: String by project

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java-library")

    group = "com.helix.core"
    version = helixVersion

    repositories {
        mavenCentral()
    }

    dependencies {
        "implementation"(rootProject.libs.kotlin.stdlib)
        "testImplementation"(rootProject.libs.junit.jupiter)
        "testImplementation"(rootProject.libs.mockk)
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.addAll(
                "-Xjsr305=strict",       // strict null-safety interop
                "-Xcontext-receivers"
            )
            allWarningsAsErrors.set(providers.gradleProperty("helix.strict").map { it.toBoolean() }.getOrElse(false))
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    // Explicit API mode: every public API module must declare visibility
    // explicitly. This is what makes "communicate only through interfaces"
    // enforceable by the compiler, not just by convention.
    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        explicitApi()
        jvmToolchain(17)
    }
}
