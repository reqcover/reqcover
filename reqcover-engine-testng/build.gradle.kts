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
            implementation(libs.testng)
        }
        jvmTest.dependencies {
        }
    }
}

tasks.named<Test>("jvmTest") {
    useTestNG()
}
