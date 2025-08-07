plugins {
    kotlin("multiplatform")
    id("gitlab-publishing")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            api(project(":reqcover-engine"))
            implementation(libs.jackson.dataformat.yaml)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
