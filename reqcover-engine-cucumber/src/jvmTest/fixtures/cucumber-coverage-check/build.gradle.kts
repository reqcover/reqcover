plugins {
    java
}

dependencies {
    testImplementation(files(
        __REQCOVER_TEST_CLASSPATH__
    ))
}

tasks.register<JavaExec>("cucumber") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("io.cucumber.core.cli.Main")
    args("--glue", "example", "classpath:features")
}
