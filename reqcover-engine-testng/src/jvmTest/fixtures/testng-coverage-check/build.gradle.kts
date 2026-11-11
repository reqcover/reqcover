plugins {
    java
}

dependencies {
    testImplementation(files(
        __REQCOVER_TEST_CLASSPATH__
    ))
}

tasks.test {
    useTestNG()
    systemProperty("reqCover.requirementsUris", "requirements-a.yaml")
    systemProperty("reqCover.minimumRequiredCoveragePercent", "100")
    testLogging.showStandardStreams = true
}
