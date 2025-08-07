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
import java.io.File
import kotlin.io.readText
import kotlin.jvm.java
import kotlin.text.trim

class GitLabPublishingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("maven-publish")
        project.extensions.getByType<PublishingExtension>().repositories {
            maven("https://gitlabx.esol.de/api/v4/projects/61/packages/maven") {
                name = "GitLab"
                credentials(HttpHeaderCredentials::class.java) {
                    val jobToken = System.getenv("CI_JOB_TOKEN")
                    val privateTokenFile = File(".private-token")
                    val privateToken = System.getenv("GITLAB_TOKEN")
                    if (jobToken != null) {
                        this.name = "Job-Token"
                        this.value = jobToken
                    } else if (privateTokenFile.exists()) {
                        this.name = "Private-Token"
                        this.value = privateTokenFile.readText().trim()
                    } else if (privateToken != null) {
                        this.name = "Private-Token"
                        this.value = privateToken
                    } else {
                        this.name = "Job-Token"
                        this.value = ""
                    }
                }
                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
            }
        }
    }
}
