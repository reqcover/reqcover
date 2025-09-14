pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "reqcover-parent"

include(
    "reqcover-core",
    "reqcover-engine",
    "reqcover-junit-jupiter",
    "reqcover-reporter-xml",
    "reqcover-source-openapi",
)
