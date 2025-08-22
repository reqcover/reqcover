package dev.reqcover.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.credentials.HttpHeaderCredentials
import org.gradle.api.publish.PublishingExtension
import org.gradle.authentication.http.HttpHeaderAuthentication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.maven

class GitPublishingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("maven-publish")
        project.extensions.getByType<PublishingExtension>().repositories {
            if (System.getenv("GITHUB_ACTIONS") != null) {
                maven("https://maven.pkg.github.com/reqcover/reqcover") {
                    name = "GitHub"
                    credentials {
                        username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("github.username") as String?
                        password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("github.token") as String?
                    }
                }
            }
            if (System.getenv("GITLAB_CI") != null) {
                maven("https://gitlabx.esol.de/api/v4/projects/85/packages/maven") {
                    name = "GitLab"
                    credentials(HttpHeaderCredentials::class.java) {
                        name = "Job-Token"
                        value = System.getenv("CI_JOB_TOKEN")
                    }
                    authentication {
                        create<HttpHeaderAuthentication>("header")
                    }
                }
            }
        }
    }
}