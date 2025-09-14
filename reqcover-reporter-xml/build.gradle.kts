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
        commonTest.dependencies {
            implementation(libs.junit.platform.launcher)
            implementation(libs.junit.jupiter.api)
            implementation(libs.junit.jupiter.engine)
            implementation(libs.bundles.junit)
            implementation(libs.xmlunit.assertj)
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}
