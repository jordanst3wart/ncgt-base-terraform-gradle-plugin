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
package org.ysb33r.gradle.terraform.internal

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet
import org.ysb33r.gradle.terraform.tasks.AbstractTerraformTask

import static org.ysb33r.gradle.terraform.internal.DefaultTerraformTasks.APPLY
import static org.ysb33r.gradle.terraform.plugins.TerraformBasePlugin.TERRAFORM_TASK_GROUP
import static org.ysb33r.grolifant.api.core.LegacyLevel.PRE_4_10

/** Provide convention naming.
 *
 * @author Schalk W. Cronjé
 */
@CompileStatic
class TerraformConvention {

    public static final String DEFAULT_SOURCESET_NAME = 'main'
    public static final String TERRAFORM_INIT = DefaultTerraformTasks.INIT.command
    public static final String DEFAULT_WORKSPACE = 'default'

    /** Provides a task name
     *
     * @param sourceSetName Name of source set the task will be associated with.
     * @param commandType The Terraform command that this task will wrap.
     * @return Name of take
     * @deprecated
     */
    @Deprecated
    static String taskName(String sourceSetName, String commandType) {
        taskName(sourceSetName, commandType, DEFAULT_WORKSPACE)
    }

    /** Provides a task name
     *
     * @param sourceSetName Name of source set the task will be associated with.
     * @param commandType The Terraform command that this task will wrap.
     * @param workspaceName Name of workspace. if {@code null} will act as if workspace-agnostic
     * @return Name of task
     *
     * @since 0.10
     */
    static String taskName(String sourceSetName, String commandType, String workspaceName) {
        boolean agnostic = workspaceName ? DefaultTerraformTasks.byCommand(commandType).workspaceAgnostic : true
        String workspace = workspaceName == DEFAULT_WORKSPACE || agnostic ? '' : workspaceName.capitalize()
        sourceSetName == DEFAULT_SOURCESET_NAME ?
            "tf${commandType.capitalize()}${workspace}" :
            "tf${sourceSetName.capitalize()}${commandType.capitalize()}${workspace}"
    }

    /** Returns the default text used for a Terraform source set
     *
     * @param sourceSetName Name of the source set
     * @return Display name
     */
    static String sourceSetDisplayName(String sourceSetName) {
        sourceSetName == DEFAULT_SOURCESET_NAME ?
            'Main Terraform source set' :
            "Terraform source set for ${sourceSetName}"
    }

    /** Creates or registers the tasks associated with a sourceset using specific conventions
     *
     * For any sourceset other than {@code main}, tasks will be named using a pattern such as
     * {@code terraform<SourceSetName>     Init} and source directories will be {@code src/tf/<sourceSetName>}.
     *
     * @param project Project Project to attache source set to.
     * @param sourceSetName Name of Terraform source set.
     */
    static void createTasksByConvention(Project project, TerraformSourceDirectorySet sourceSet) {
        createWorkspaceTasksByConvention(project, sourceSet, DEFAULT_WORKSPACE)
    }

    /** Creates or registers the tasks associated with an additional worksapce in a sourceset.
     *
     * For any source set other than {@code main}, tasks will be named using a pattern such as
     * {@code terraform<SourceSetName>            Init<WorkspaceName>}.
     *
     * @param project Project to attach source set to.
     * @param sourceSetName Name of Terraform source set.
     * @param workspaceName Name of workspace
     */
    static void createWorkspaceTasksByConvention(
        Project project,
        TerraformSourceDirectorySet sourceSet,
        String workspaceName
    ) {
        if (!project.tasks.findByName(taskName(sourceSet.name, APPLY.command, workspaceName))) {
            DefaultTerraformTasks.ordered().each {
                boolean requireTask = workspaceName == DEFAULT_WORKSPACE ||
                    workspaceName != DEFAULT_WORKSPACE && !it.workspaceAgnostic
                if (requireTask) {
                    String newTaskName = taskName(sourceSet.name, it.command, workspaceName)
                    if (PRE_4_10) {
                        createTasks(sourceSet, project, workspaceName, it, newTaskName)
                    } else {
                        registerTasks(sourceSet, project, workspaceName, it, newTaskName)
                    }
                }
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
                if (type != DefaultTerraformTasks.INIT) {
                    t.mustRunAfter taskName(name, TERRAFORM_INIT)
                }
            }
        }
    }

    private static void registerTasks(
        TerraformSourceDirectorySet sourceSet,
        Project project,
        String workspaceName,
        DefaultTerraformTasks taskDetails,
        String newTaskName
    ) {
        def taskConfigurator = taskConfigurator(sourceSet, taskDetails)
        TaskProvider<AbstractTerraformTask> taskProvider
        if (taskDetails.dependsOnProvider) {
            taskProvider = project.tasks.register(
                newTaskName,
                taskDetails.type,
                taskDetails.dependsOnProvider.newInstance(project, sourceSet.name, workspaceName),
                workspaceName
            ) as TaskProvider<AbstractTerraformTask>
        } else if (taskDetails.workspaceAgnostic) {
            taskProvider = project.tasks.register(
                newTaskName,
                taskDetails.type
            ) as TaskProvider<AbstractTerraformTask>
        } else {
            taskProvider = project.tasks.register(
                newTaskName,
                taskDetails.type,
                workspaceName
            ) as TaskProvider<AbstractTerraformTask>
        }

        taskProvider.configure(taskConfigurator)
    }

    @CompileDynamic
    private static void createTasks(
        TerraformSourceDirectorySet sourceSet,
        Project project,
        String workspaceName,
        DefaultTerraformTasks taskDetails,
        String newTaskName
    ) {
        String name = sourceSet.name
        AbstractTerraformTask newTask
        if (taskDetails.dependsOnProvider) {
            newTask = project.tasks.create(
                newTaskName,
                taskDetails.type,
                taskDetails.dependsOnProvider.newInstance(project, name, workspaceName),
                workspaceName
            )
        } else if (taskDetails.workspaceAgnostic) {
            newTask = project.tasks.create(
                newTaskName,
                taskDetails.type
            )
        } else {
            newTask = project.tasks.create(
                newTaskName,
                taskDetails.type,
                workspaceName
            )
        }
        newTask.sourceSet = sourceSet
        newTask.group = TERRAFORM_TASK_GROUP
        newTask.description = "${taskDetails.description} for '${name}'"
        if (taskDetails != DefaultTerraformTasks.INIT) {
            newTask.mustRunAfter taskName(name, TERRAFORM_INIT, workspaceName)
        }
    }
}