/*
 * Copyright (c) 2008-2025 eSol GmbH
 * All Rights Reserved.
 *
 * This source code, including all associated documentation (collectively, the "Material"),
 * is the exclusive property of eSol GmbH. The Material is protected by the German Copyright Act
 * (Urheberrechtsgesetz), trade secret laws, and international intellectual property treaties.
 * Unauthorized use, reproduction, modification, disclosure, distribution, or publication
 * of the Material, in whole or in part, without the prior written consent of eSol GmbH,
 * is strictly prohibited.
 *
 * NO LICENSE OR RIGHT, EXPRESS OR IMPLIED, IS GRANTED BY THE DISCLOSURE OR DELIVERY OF THE
 * MATERIAL. ANY LICENSE UNDER PATENTS, COPYRIGHTS, TRADEMARKS, OR OTHER INTELLECTUAL PROPERTY
 * RIGHTS MUST BE EXPRESSLY SET FORTH IN A WRITTEN AGREEMENT SIGNED BY ESOL GMBH.
 *
 * THE MATERIAL IS PROVIDED "AS IS," WITHOUT WARRANTY OF ANY KIND, WHETHER EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. TO THE MAXIMUM EXTENT PERMITTED BY LAW, ESOL GMBH SHALL NOT
 * BE LIABLE FOR ANY DAMAGES ARISING FROM THE USE OF THE MATERIAL.
 *
 * THIS NOTICE MUST NOT BE REMOVED OR ALTERED.
 */

package de.esol.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.credentials.HttpHeaderCredentials
import org.gradle.api.publish.PublishingExtension
import org.gradle.authentication.http.HttpHeaderAuthentication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.maven
import kotlin.jvm.java

class GitLabPublishingPlugin : Plugin<Project> {
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
