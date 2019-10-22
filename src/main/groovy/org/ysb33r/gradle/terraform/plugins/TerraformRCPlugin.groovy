/*
 * Copyright 2017-2019 the original author or authors.
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
import org.gradle.api.Task
import org.ysb33r.gradle.terraform.TerraformRCExtension

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
        TerraformRCExtension terraformrc = project.extensions.create(TERRAFORM_RC_EXT, TerraformRCExtension, project)
        Task generator = project.tasks.create(TERRAFORM_RC_TASK)
        generator.identity {
            group = 'Terraform'
            description = 'Generates Terraform configuration file'
            onlyIf { !terraformrc.useGlobalConfig }
            inputs.property'details', { ->
                StringWriter w = new StringWriter()
                terraformrc.toHCL(w).toString()
            }
            outputs.file terraformrc.terraformRC
        }

        generator.doLast {
            terraformrc.terraformRC.get().withWriter { w ->
                terraformrc.toHCL(w)
            }
        }
    }
}
