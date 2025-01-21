/*
 * Copyright 2017-2022 the original author or authors.
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
import org.ysb33r.gradle.terraform.TerraformRCExtension
import org.ysb33r.grolifant.api.core.ProjectOperations

/** A plugin that deals with Terraform tool configuration.
 *
 * Normally in a multi-project, this plugin should be applied to the root project.
 */
@CompileStatic
class TerraformRCPlugin implements Plugin<Project> {
    public static final String TERRAFORM_RC_EXT = 'terraformrc'
    public static final String TERRAFORM_RC_TASK = 'generateTerraformConfig'

    @Override
    void apply(Project project) {
        ProjectOperations.maybeCreateExtension(project)
        TerraformRCExtension terraformRcExt = project.extensions.create(TERRAFORM_RC_EXT, TerraformRCExtension, project)
        project.tasks.register(TERRAFORM_RC_TASK) { it ->
            it.group = TerraformBasePlugin.TERRAFORM_TASK_GROUP
            it.description = 'Generates Terraform configuration file'
            it.onlyIf { !terraformRcExt.useGlobalConfig }
            it.inputs.property'details', { ->
                StringWriter w = new StringWriter()
                terraformRcExt.toHCL(w).toString()
            }
            it.outputs.file terraformRcExt.terraformRC
            it.doLast {
                terraformRcExt.terraformRC.get().withWriter { w ->
                    terraformRcExt.toHCL(w)
                }
            }
        }
    }
}
