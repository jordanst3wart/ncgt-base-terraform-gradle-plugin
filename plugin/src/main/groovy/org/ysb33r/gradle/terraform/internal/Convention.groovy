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
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet
import org.ysb33r.gradle.terraform.tasks.TerraformTask
import org.ysb33r.gradle.terraform.tasks.DefaultTerraformTasks
import org.ysb33r.gradle.terraform.tasks.RemoteStateTask
import org.ysb33r.gradle.terraform.tasks.TerraformInit

import static org.ysb33r.gradle.terraform.tasks.DefaultTerraformTasks.APPLY
import static org.ysb33r.gradle.terraform.tasks.DefaultTerraformTasks.INIT
import static org.ysb33r.gradle.terraform.plugins.TerraformPlugin.TERRAFORM_TASK_GROUP

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
        "${commandType}${sourceSetName.capitalize()}"
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
    @SuppressWarnings('InvertedIfElse')
    static void createTasksByConvention(Project project, TerraformSourceDirectorySet sourceSet) {
        if (!hasTaskRegistered(project.tasks, taskName(sourceSet.name, APPLY.command))) {
            DefaultTerraformTasks.tasks().each {
                String newTaskName = taskName(sourceSet.name, it.command)
                registerTask(sourceSet, project, it, newTaskName)
            }
            registerBackendConfigurationTask(sourceSet, project)
        } else {
            throw new IllegalStateException("duplicate tasks creation running for sourceSet $sourceSet.name")
        }
    }

    private static void registerBackendConfigurationTask(
        TerraformSourceDirectorySet sourceSet,
        Project project
    ) {
        TaskProvider<RemoteStateTask> remoteStateTask = project.tasks.register(
            backendTaskName(sourceSet.name),
            RemoteStateTask,
        ) {
            it.group = TERRAFORM_TASK_GROUP
            it.description = "Write partial backend configuration file for '${sourceSet.name}'"
            it.backendText = sourceSet.backendPropertyText().map { it }
            it.destinationDir = new File(
                "${project.buildDir}/${sourceSet.name}/tf/remoteState")
        }

        project.tasks.named(taskName(sourceSet.name, 'init'), TerraformInit).configure {
            it.dependsOn(remoteStateTask)
            it.backendConfigFile = remoteStateTask.get().backendConfigFile.get()
            it.useBackendFile = remoteStateTask.get().backendFileRequired
        }
    }

    private static void registerTask(
        TerraformSourceDirectorySet sourceSet,
        Project project,
        DefaultTerraformTasks taskDetails,
        String newTaskName
    ) {
        TaskProvider<TerraformTask> taskProvider = project.tasks.register(
            newTaskName,
            taskDetails.type
        )
        taskProvider.configure(taskConfigurator(sourceSet, taskDetails))
    }

    private static Action<TerraformTask> taskConfigurator(
        TerraformSourceDirectorySet sourceSet,
        DefaultTerraformTasks type
    ) {
        String name = sourceSet.name
        new Action<TerraformTask>() {
            @Override
            void execute(TerraformTask t) {
                t.sourceSet = sourceSet
                t.group = TERRAFORM_TASK_GROUP
                t.description = "${type.description} for '${name}'"
                if (type != INIT) {
                    t.mustRunAfter taskName(name, INIT.command)
                }
            }
        }
    }

    private static boolean hasTaskRegistered(TaskContainer tasks, String name) {
        tasks.names.contains(name)
    }
}