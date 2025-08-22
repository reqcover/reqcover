plugins {
    `kotlin-dsl`
}

group = "dev.reqcover.buildlogic"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

gradlePlugin {
    plugins {
        create("gitPublishing") {
            id = "git-publishing"
            implementationClass = "dev.reqcover.buildlogic.GitPublishingPlugin"
        }
    }
}

dependencies {
    implementation(gradleApi())
}
