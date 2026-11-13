plugins {
    kotlin("multiplatform")
    id("git-publishing")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(project(":reqcover-core"))
            api(project(":reqcover-engine"))
        }
        jvmMain.dependencies {
            compileOnly(libs.cucumber.plugin)
        }
        jvmTest.dependencies {
            implementation(libs.cucumber.core)
            implementation(libs.cucumber.java)
            implementation(project(":reqcover-source-openapi"))
            implementation(libs.junit.jupiter.engine)
            implementation(libs.bundles.junit)
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    systemProperty("reqcover.version", project.version.toString())
}

tasks.named("jvmTest") {
    dependsOn(
        ":reqcover-core:jvmJar",
        ":reqcover-engine:jvmJar",
        ":reqcover-source-openapi:jvmJar",
        ":reqcover-reporter-xml:jvmJar",
        "jvmJar",
    )
}
