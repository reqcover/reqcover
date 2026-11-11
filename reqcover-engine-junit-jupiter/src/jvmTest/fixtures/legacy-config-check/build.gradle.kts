plugins {
    java
}

dependencies {
    implementation(files(
        __REQCOVER_TEST_CLASSPATH__
    ))
}

tasks.register<JavaExec>("legacyConfigCheck") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "example.LegacyConfigMain"
}
