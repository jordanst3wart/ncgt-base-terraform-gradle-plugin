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
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskProvider
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet
import org.ysb33r.gradle.terraform.TerraformSourceSets
import org.ysb33r.gradle.terraform.config.VariablesSpec
import org.ysb33r.gradle.terraform.internal.remotestate.BackendFactory
import org.ysb33r.gradle.terraform.remotestate.LocalBackendSpec
import org.ysb33r.gradle.terraform.remotestate.TerraformRemoteStateExtension
import org.ysb33r.gradle.terraform.tasks.TerraformCustomFmtApply
import org.ysb33r.gradle.terraform.tasks.TerraformCustomFmtCheck
import org.ysb33r.grolifant.api.core.ProjectOperations

import static org.ysb33r.gradle.terraform.internal.DefaultTerraformTasks.FMT_APPLY
import static org.ysb33r.gradle.terraform.internal.TerraformConvention.DEFAULT_SOURCESET_NAME
import static org.ysb33r.gradle.terraform.internal.TerraformConvention.createTasksByConvention
import static org.ysb33r.gradle.terraform.internal.TerraformConvention.taskName
import static org.ysb33r.gradle.terraform.plugins.TerraformBasePlugin.REMOTE_STATE_VARIABLE
import static org.ysb33r.gradle.terraform.plugins.TerraformBasePlugin.addRemoteStateExtension

/** Provide the basic capabilities for dealing with Terraform tasks. Allow for downloading & caching of
 * Terraform distributions on a variety of the most common development platforms.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformPlugin implements Plugin<Project> {
    public static final String CUSTOM_FMT_CHECK = 'tfFmtCheckCustomDirectories'
    public static final String CUSTOM_FMT_APPLY = 'tfFmtApplyCustomDirectories'
    public static final String FORMAT_ALL = 'tfFormatAll'

    void apply(Project project) {
        project.apply plugin: TerraformBasePlugin

        TaskProvider<Task> formatAll = project.tasks.register(FORMAT_ALL)
        formatAll.configure {
            it.group = 'terraform'
            it.description = 'Formats all terraform source'
        }

        TerraformSourceSets terraformSourceSets = project.extensions.getByType(TerraformSourceSets)
        def globalRemoteState = ((ExtensionAware) project.extensions.getByType(TerraformExtension))
            .extensions
            .getByType(TerraformRemoteStateExtension)

        terraformSourceSetRemoteStateRules(project, globalRemoteState)
        terraformSourceSetConventionTaskRules(project, formatAll)
        terraformSourceSets.create(DEFAULT_SOURCESET_NAME)

        def checkProvider = project.tasks.register(CUSTOM_FMT_CHECK, TerraformCustomFmtCheck)
        def applyProvider = project.tasks.register(CUSTOM_FMT_APPLY, TerraformCustomFmtApply, checkProvider)
        formatAll.configure { it.dependsOn applyProvider }
    }

    private void terraformSourceSetRemoteStateRules(
        Project project,
        TerraformRemoteStateExtension globalRemoteState
    ) {
        project.extensions.getByType(TerraformSourceSets).configureEach { TerraformSourceDirectorySet tsds ->
            TerraformRemoteStateExtension tsdsBackends = addRemoteStateExtension(project, ((ExtensionAware) tsds))
            BackendFactory.createBackend(
                ProjectOperations.find(project),
                project.objects,
                tsds,
                LocalBackendSpec.NAME,
                LocalBackendSpec
            )
            tsdsBackends.follow(globalRemoteState)
            tsds.variables.provider(new Action<VariablesSpec>() {
                @Override
                void execute(VariablesSpec variablesSpec) {
                    if (tsdsBackends.remoteStateVarProvider.getOrElse(false)) {
                        variablesSpec.map(REMOTE_STATE_VARIABLE, tsdsBackends.tokenProvider)
                    }
                }
            })
        }
    }

    private void terraformSourceSetConventionTaskRules(
        Project project,
        TaskProvider<Task> formatAll
    ) {
        project.extensions.getByType(TerraformSourceSets).configureEach(new Action<TerraformSourceDirectorySet>() {
            @Override
            void execute(TerraformSourceDirectorySet tsds) {
                createTasksByConvention(project, tsds)
                formatAll.configure {
                    it.dependsOn(project.tasks.named(taskName(tsds.name, FMT_APPLY.command, null)))
                }
            }
        })
    }
}
