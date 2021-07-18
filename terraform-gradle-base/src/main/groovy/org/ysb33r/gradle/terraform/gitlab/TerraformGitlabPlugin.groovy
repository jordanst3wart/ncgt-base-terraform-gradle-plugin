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
package org.ysb33r.gradle.terraform.gitlab

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet
import org.ysb33r.gradle.terraform.TerraformSourceSets
import org.ysb33r.gradle.terraform.plugins.TerraformPlugin

@CompileStatic
class TerraformGitlabPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.pluginManager.apply(TerraformPlugin)

        TerraformSourceSets terraformSourceSets = project.extensions.getByType(TerraformSourceSets)
        terraformSourceSets.configureEach(new Action<TerraformSourceDirectorySet>() {
            @Override
            void execute(TerraformSourceDirectorySet tsds) {
                GitlabExtension gitlabExtension = ((ExtensionAware) tsds).extensions.create(
                    GitlabExtension.NAME,
                    GitlabExtension,
                    project
                )

                tsds.registerCredentialProvider(gitlabExtension)
            }
        })
    }
}
