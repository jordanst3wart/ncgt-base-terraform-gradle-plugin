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
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet
import org.ysb33r.gradle.terraform.tasks.TerraformFmtCheck

import static org.gradle.language.base.plugins.LifecycleBasePlugin.CHECK_TASK_NAME
import static org.ysb33r.gradle.terraform.tasks.DefaultTerraformTasks.FMT_APPLY
import static org.ysb33r.gradle.terraform.internal.TerraformConvention.createTasksByConvention
import static org.ysb33r.gradle.terraform.internal.TerraformConvention.taskName
import static org.ysb33r.gradle.terraform.plugins.TerraformBasePlugin.TERRAFORM_TASK_GROUP

/** Provide the basic capabilities for dealing with Terraform tasks. Allow for downloading & caching of
 * Terraform distributions on a variety of the most common development platforms.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformPlugin implements Plugin<Project> {
    public static final String FORMAT_ALL = 'tfFormatAll'

    void apply(Project project) {
        project.apply plugin: TerraformBasePlugin
        def formatAll = project.tasks.register(FORMAT_ALL)
        formatAll.configure {
            it.group = TERRAFORM_TASK_GROUP
            it.description = 'Formats all terraform source'
        }
        terraformSourceSetConventionTaskRules(project, formatAll)
        configureCheck(project)
    }

    private static void configureCheck(Project project) {
        project.pluginManager.apply(LifecycleBasePlugin)
        def check = project.tasks.named(CHECK_TASK_NAME)
        check.configure {
            it.dependsOn(project.tasks.withType(TerraformFmtCheck))
        }
    }

    private static void terraformSourceSetConventionTaskRules(
        Project project,
        TaskProvider<Task> formatAll
    ) {
        project.extensions.getByType(NamedDomainObjectContainer<TerraformSourceDirectorySet>).configureEach(
            new Action<TerraformSourceDirectorySet>() {
            @Override
            void execute(TerraformSourceDirectorySet tsds) {
                createTasksByConvention(project, tsds)
                formatAll.configure {
                    it.dependsOn(project.tasks.named(taskName(tsds.name, FMT_APPLY.command)))
                }
            }
        })
    }
}
