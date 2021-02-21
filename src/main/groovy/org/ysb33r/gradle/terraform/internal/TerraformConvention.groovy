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
import org.ysb33r.grolifant.api.core.LegacyLevel

import static org.ysb33r.gradle.terraform.plugins.TerraformBasePlugin.TERRAFORM_TASK_GROUP

/** Provide convention naming.
 *
 * @author Schalk W. Cronjé
 */
@CompileStatic
class TerraformConvention {

    public static final String DEFAULT_SOURCESET_NAME = 'main'
    public static final String TERRAFORM_INIT = DefaultTerraformTasks.INIT.command

    /** Provides a task name
     *
     * @param sourceSetName Name of source set the task will be associated with.
     * @param commandType The Terraform command that this task will wrap.
     * @return Name of take
     */
    static String taskName(String sourceSetName, String commandType) {
        sourceSetName == DEFAULT_SOURCESET_NAME ?
            "tf${commandType.capitalize()}" :
            "tf${sourceSetName.capitalize()}${commandType.capitalize()}"
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
     * {@code terraform<SourceSetName>         Init} and source directories will be {@code src/tf/<sourceSetName>}.
     *
     * @param project Project Project to attache source set to.
     * @param sourceSetName Name of Terraform source set.
     */
    static void createTasksByConvention(Project project, TerraformSourceDirectorySet sourceSet) {
        if (LegacyLevel.PRE_4_10) {
            createTasks(sourceSet, project)
        } else {
            registerTasks(sourceSet, project)
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
        Project project
    ) {
        if (!project.tasks.findByName(taskName(sourceSet.name, TERRAFORM_INIT))) {
            String name = sourceSet.name
            DefaultTerraformTasks.ordered().each {
                def taskConfigurator = taskConfigurator(sourceSet, it)
                TaskProvider<AbstractTerraformTask> taskProvider
                if (it.dependsOnProvider) {
                    taskProvider = project.tasks.register(
                        taskName(name, it.command),
                        it.type,
                        it.dependsOnProvider.newInstance(project, name)
                    ) as TaskProvider<AbstractTerraformTask>
                } else {
                    taskProvider = project.tasks.register(
                        taskName(name, it.command),
                        it.type
                    ) as TaskProvider<AbstractTerraformTask>
                }

                taskProvider.configure(taskConfigurator)
            }
        }
    }

    @CompileDynamic
    private static void createTasks(
        TerraformSourceDirectorySet sourceSet,
        Project project
    ) {
        String name = sourceSet.name
        if (!project.tasks.findByName(taskName(sourceSet.name, TERRAFORM_INIT))) {
            DefaultTerraformTasks.ordered().each {
                AbstractTerraformTask newTask
                if (it.dependsOnProvider) {
                    newTask = project.tasks.create(
                        taskName(name, it.command),
                        it.type,
                        it.dependsOnProvider.newInstance(project, name)
                    )
                } else {
                    newTask = project.tasks.create(
                        taskName(name, it.command),
                        it.type
                    )
                }
                newTask.sourceSet = sourceSet
                newTask.group = TERRAFORM_TASK_GROUP
                newTask.description = "${it.description} for '${name}'"
                if (it != DefaultTerraformTasks.INIT) {
                    newTask.mustRunAfter taskName(name, TERRAFORM_INIT)
                }
            }
        }
    }
}