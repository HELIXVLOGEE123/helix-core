// helix-platform is the ONLY module allowed to see every runtime module.
// It is the composition root: it wires default implementations behind
// their interfaces and exposes a single HelixCore facade. Feature modules
// (Launcher, Voice, AI, Automation, Vision, Memory) depend on
// helix-platform ONLY — never on any -runtime module directly.
dependencies {
    api(project(":helix-logging-api"))
    api(project(":helix-eventbus-api"))
    api(project(":helix-config-api"))
    api(project(":helix-security-api"))
    api(project(":helix-storage-api"))
    api(project(":helix-scheduler-api"))
    api(project(":helix-registry-api"))
    api(project(":helix-lifecycle-api"))
    api(project(":helix-plugin-api"))

    implementation(project(":helix-logging-runtime"))
    implementation(project(":helix-eventbus-runtime"))
    implementation(project(":helix-config-runtime"))
    implementation(project(":helix-security-runtime"))
    implementation(project(":helix-storage-runtime"))
    implementation(project(":helix-scheduler-runtime"))
    implementation(project(":helix-registry-runtime"))
    implementation(project(":helix-lifecycle-runtime"))
    implementation(project(":helix-plugin-runtime"))
}
