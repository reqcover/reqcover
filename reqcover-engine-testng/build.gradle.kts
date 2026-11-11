plugins {
    kotlin("multiplatform")
    id("git-publishing")
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
            implementation(project(":reqcover-source-openapi"))
            implementation(libs.junit.jupiter.engine)
            implementation(libs.bundles.junit)
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}
