// helix-eventbus-api depends only on logging-api (events may carry log-level metadata).
dependencies {
    api(project(":helix-logging-api"))
}
