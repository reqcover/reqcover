plugins {
    alias(libs.plugins.axion.release)
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library) apply false
}

scmVersion {}
version = scmVersion.version

allprojects {
    group = "dev.reqcover"

    project.version = rootProject.version
}
