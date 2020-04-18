/*
 * Copyright 2017-2020 the original author or authors.
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
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.ysb33r.gradle.terraform.TerraformSourceSets

import static org.ysb33r.gradle.terraform.internal.TerraformConvention.DEFAULT_SOURCESET_NAME
import static org.ysb33r.gradle.terraform.internal.TerraformConvention.createSourceSetByConvention

/** Provide the basic capabilities for dealing with Terraform tasks. Allow for downloading & caching of
 * Terraform distributions on a variety of the most common development platforms.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.apply plugin: TerraformBasePlugin

        TerraformSourceSets terraformSourceSets = project.extensions.getByType(TerraformSourceSets)

        createSourceSetByConvention(project, DEFAULT_SOURCESET_NAME)
        terraformSourceSets.addRule('Add source set', new Action<String>() {
            @Override
            void execute(String requestedName) {
                createSourceSetByConvention(project, requestedName)
            }
        })
    }
}
