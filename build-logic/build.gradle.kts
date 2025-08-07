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
        create("gitlabPublishing") {
            id = "gitlab-publishing"
            implementationClass = "de.esol.buildlogic.GitLabPublishingPlugin"
        }
    }
}

dependencies {
    implementation(gradleApi())
}
