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
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.TerraformRCExtension
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet
import org.ysb33r.gradle.terraform.tasks.AbstractTerraformTask
import org.ysb33r.gradle.terraform.tasks.RemoteStateTask
import org.ysb33r.gradle.terraform.tasks.TerraformFmtCheck
import org.ysb33r.grolifant.api.core.ProjectOperations

import static org.gradle.language.base.plugins.LifecycleBasePlugin.CHECK_TASK_NAME
import static org.ysb33r.gradle.terraform.internal.TerraformConfigUtils.locateTerraformRCGenerator
import static org.ysb33r.gradle.terraform.internal.TerraformConvention.sourceSetDisplayName
import static org.ysb33r.gradle.terraform.tasks.DefaultTerraformTasks.FMT_APPLY
import static org.ysb33r.gradle.terraform.internal.TerraformConvention.createTasksByConvention
import static org.ysb33r.gradle.terraform.internal.TerraformConvention.taskName

/** Provide the basic capabilities for dealing with Terraform tasks. Allow for downloading & caching of
 * Terraform distributions on a variety of the most common development platforms.
 *
 */
@CompileStatic
class TerraformPlugin implements Plugin<Project> {
    public static final String FORMAT_ALL = 'fmtAll'
    // TODO terraformrc should be a root object, might be a bug with sub-projects
    public static final String TERRAFORM_RC_EXT = 'terraformrc'
    public static final String TERRAFORM_RC_TASK = 'generateTerraformConfig'
    public static final String TERRAFORM_SOURCESETS = 'terraformSourceSets'
    public static final String TERRAFORM_TASK_GROUP = 'Terraform'

    void apply(Project project) {
        ProjectOperations.maybeCreateExtension(project)
        configureTerraformRC(project.getRootProject())

        project.tasks.withType(RemoteStateTask).configureEach { RemoteStateTask t ->
            t.dependsOn(locateTerraformRCGenerator(t.project))
        }

        project.tasks.withType(AbstractTerraformTask).configureEach { AbstractTerraformTask t ->
            t.dependsOn(locateTerraformRCGenerator(t.project))
        }

        project.extensions.create(TerraformExtension.NAME, TerraformExtension, project)
        createTerraformSourceSetsExtension(project)

        def formatAll = project.tasks.register(FORMAT_ALL)
        formatAll.configure {
            it.group = TERRAFORM_TASK_GROUP
            it.description = 'Formats all terraform source'
        }
        terraformSourceSetConventionTaskRules(project, formatAll)
        configureCheck(project)
    }

    private static void configureTerraformRC(Project rootProject) {
        // create projections for root rootProject
        ProjectOperations.maybeCreateExtension(rootProject)
        TerraformRCExtension terraformRcExt = rootProject.extensions.create(TERRAFORM_RC_EXT, TerraformRCExtension, rootProject)
        rootProject.tasks.register(TERRAFORM_RC_TASK) { it ->
            it.group = TERRAFORM_TASK_GROUP
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

    private static NamedDomainObjectContainer<TerraformSourceDirectorySet> createTerraformSourceSetsExtension(
        Project project
    ) {
        NamedDomainObjectFactory<TerraformSourceDirectorySet> factory = { String name ->
            project.objects.newInstance(
                TerraformSourceDirectorySet,
                project,
                name,
                sourceSetDisplayName(name)
            )
        }
        NamedDomainObjectContainer<TerraformSourceDirectorySet> sourceSetContainer =
            project.objects.domainObjectContainer(TerraformSourceDirectorySet, factory)
        project.extensions.add(TERRAFORM_SOURCESETS, sourceSetContainer)
        sourceSetContainer
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
