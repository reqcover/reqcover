plugins {
    kotlin("multiplatform")
    id("gitlab-publishing")
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
            implementation(libs.junit.platform.launcher)
        }
        jvmTest.dependencies {
            implementation(libs.junit.jupiter.engine)
            implementation(libs.bundles.junit)
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}
