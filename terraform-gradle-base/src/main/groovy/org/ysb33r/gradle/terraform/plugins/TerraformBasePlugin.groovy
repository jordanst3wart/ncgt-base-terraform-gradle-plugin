/*
 * Copyright 2017-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ysb33r.gradle.terraform.plugins

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.TerraformSourceSets
import org.ysb33r.gradle.terraform.internal.DefaultTerraformSourceSets
import org.ysb33r.gradle.terraform.tasks.AbstractTerraformTask
import org.ysb33r.grolifant.api.core.ProjectOperations

import static org.ysb33r.gradle.terraform.internal.TerraformConfigUtils.locateTerraformRCGenerator

/** Provide the basic capabilities for dealing with Terraform tasks. Allow for downloading & caching of
 * Terraform distributions on a variety of the most common development platforms.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformBasePlugin implements Plugin<Project> {
    public static final String TERRAFORM_SOURCESETS = 'terraformSourceSets'
    public static final String TERRAFORM_TASK_GROUP = 'Terraform'

    void apply(Project project) {
        if (project == project.rootProject) {
            project.apply plugin: TerraformRCPlugin
        }
        ProjectOperations.maybeCreateExtension(project)
        project.extensions.create(TerraformExtension.NAME, TerraformExtension, project)

        project.extensions.create(TerraformSourceSets, TERRAFORM_SOURCESETS, DefaultTerraformSourceSets, project)

        project.pluginManager.withPlugin('org.ysb33r.cloudci') {
            project.tasks.withType(AbstractTerraformTask) { AbstractTerraformTask t ->
                t.environment TF_AUTOMATION: 1
            }
        }

        project.tasks.withType(AbstractTerraformTask) { AbstractTerraformTask t ->
            t.dependsOn(locateTerraformRCGenerator(t.project))
        }
    }
}
