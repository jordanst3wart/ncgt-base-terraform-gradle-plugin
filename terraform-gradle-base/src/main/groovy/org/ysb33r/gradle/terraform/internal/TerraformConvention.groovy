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
package org.ysb33r.gradle.terraform.internal

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet
import org.ysb33r.gradle.terraform.remotestate.TerraformRemoteStateExtension
import org.ysb33r.gradle.terraform.tasks.AbstractTerraformTask
import org.ysb33r.gradle.terraform.tasks.RemoteStateConfigGenerator
import org.ysb33r.gradle.terraform.tasks.TerraformInit
import org.ysb33r.grolifant.api.core.ProjectOperations

import static org.ysb33r.gradle.terraform.internal.DefaultTerraformTasks.APPLY
import static org.ysb33r.gradle.terraform.internal.DefaultTerraformTasks.INIT
import static org.ysb33r.gradle.terraform.plugins.TerraformBasePlugin.TERRAFORM_TASK_GROUP

/** Provide convention naming.
 *
 * @author Schalk W. Cronjé
 */
@CompileStatic
class TerraformConvention {
    /** Provides a task name
     *
     * @param sourceSetName Name of source set the task will be associated with.
     * @param commandType The Terraform command that this task will wrap.
     * @return Name of task
     *
     * @since 0.10
     */
    static String taskName(String sourceSetName, String commandType) {
        "tf${sourceSetName.capitalize()}${commandType.capitalize()}"
    }

    /**
     * The name of the backend configuration task.
     *
     * @param sourceSetName Name of source set.
     *
     * @return Name of task
     *
     * @since 0.12
     */
    static String backendTaskName(String sourceSetName) {
        "create${taskName(sourceSetName, 'backendConfiguration').capitalize()}"
    }

    /** Returns the default text used for a Terraform source set
     *
     * @param sourceSetName Name of the source set
     * @return Display name
     */
    static String sourceSetDisplayName(String sourceSetName) {
        "Terraform source set for ${sourceSetName}"
    }

    /** Creates or registers the tasks associated with an additional workspace in a sourceset.
     *
     * For any source set other than {@code main}, tasks will be named using a pattern such as
     * {@code terraform<SourceSetName>                  Init<WorkspaceName>}.
     *
     * @param project Project to attach source set to.
     * @param sourceSetName Name of Terraform source set.
     */
    static void createTasksByConvention(Project project, TerraformSourceDirectorySet sourceSet) {
        if (!hasTaskRegistered(project.tasks, taskName(sourceSet.name, APPLY.command))) {
            registerBackendConfigurationTask(sourceSet, project)

            DefaultTerraformTasks.ordered().each {
                String newTaskName = taskName(sourceSet.name, it.command)
                registerTask(sourceSet, project, it, newTaskName)
            }
        }
    }

    private static Action<AbstractTerraformTask> taskConfigurator(
        TerraformSourceDirectorySet sourceSet,
        DefaultTerraformTasks type
    ) {
        String name = sourceSet.name
        new Action<AbstractTerraformTask>() {
            @Override
            void execute(AbstractTerraformTask t) {
                t.sourceSet = sourceSet
                t.group = TERRAFORM_TASK_GROUP
                t.description = "${type.description} for '${name}'"
                if (type != INIT) {
                    t.mustRunAfter taskName(name, INIT.command)
                }
            }
        }
    }

    private static Action<RemoteStateConfigGenerator> remoteStateConfigurator(
        TerraformSourceDirectorySet sourceSet,
        Provider<File> destDir
    ) {
        def remote = ((ExtensionAware) sourceSet).extensions.getByType(TerraformRemoteStateExtension)
        String name = sourceSet.name
        new Action<RemoteStateConfigGenerator>() {
            @Override
            void execute(RemoteStateConfigGenerator t) {
                t.remoteState = remote
                t.group = TERRAFORM_TASK_GROUP
                t.description = "Write partial backend configuration file for '${name}'"
                t.destinationDir = destDir
            }
        }
    }

    private static void registerBackendConfigurationTask(
        TerraformSourceDirectorySet sourceSet,
        Project project
    ) {
        String folderName = "tf${sourceSet.name.capitalize()}BackendConfiguration"

        TaskProvider<RemoteStateConfigGenerator> generator = project.tasks.register(
            backendTaskName(sourceSet.name),
            RemoteStateConfigGenerator,
        )

        generator.configure(remoteStateConfigurator(
            sourceSet,
            ProjectOperations.find(project).buildDirDescendant("tfRemoteState/${folderName}")
        ))

        project.tasks.configureEach { Task t ->
            if (t.name == taskName(sourceSet.name, INIT.command)) {
                TerraformInit newTask = (TerraformInit) t
                newTask.dependsOn(generator)
                newTask.backendConfigFile = ProjectOperations.find(project)
                    .providerTools.flatMap(generator) { it.backendConfigFile }
                newTask.useBackendFile = generator.map {
                    it.backendFileRequired
                }
            }
        }
    }

    private static void registerTask(
        TerraformSourceDirectorySet sourceSet,
        Project project,
        DefaultTerraformTasks taskDetails,
        String newTaskName
    ) {
        def taskConfigurator = taskConfigurator(sourceSet, taskDetails)
        TaskProvider<AbstractTerraformTask> taskProvider = project.tasks.register(
            newTaskName,
            taskDetails.type
        )
        taskProvider.configure(taskConfigurator)
    }

    private static boolean hasTaskRegistered(TaskContainer tasks, String name) {
        tasks.names.contains(name)
    }
}