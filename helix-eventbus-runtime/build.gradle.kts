dependencies {
    api(project(":helix-eventbus-api"))
    implementation(project(":helix-logging-api"))
    implementation(libs.kotlinx.coroutines.core)
}
