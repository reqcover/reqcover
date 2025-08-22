plugins {
    kotlin("multiplatform")
    id("git-publishing")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    jvm()

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}
