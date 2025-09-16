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
        commonMain.dependencies {
            api(project(":reqcover-core"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(project("reqcover-engine-test-source1"))
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}
