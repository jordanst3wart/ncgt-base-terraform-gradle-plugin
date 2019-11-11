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
package org.ysb33r.gradle.terraform.internal

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.util.GradleVersion
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet
import org.ysb33r.gradle.terraform.TerraformSourceSets
import org.ysb33r.gradle.terraform.tasks.AbstractTerraformTask
import org.ysb33r.gradle.terraform.tasks.TerraformApply
import org.ysb33r.gradle.terraform.tasks.TerraformImport
import org.ysb33r.gradle.terraform.tasks.TerraformInit
import org.ysb33r.gradle.terraform.tasks.TerraformPlan

import static org.ysb33r.gradle.terraform.plugins.TerraformBasePlugin.TERRAFORM_TASK_GROUP

/** Provide convention naming.
 *
 * @author Schalk W. Cronjé
 */
@CompileStatic
class TerraformConvention {

    public static final String DEFAULT_SOURCESET_NAME = 'main'

    /** Provides a task name
     *
     * @param sourceSetName Name of source set the task will be associated with.
     * @param commandType THe Terraform command that this task will wrap.
     * @return Name of take
     */
    static String taskName(String sourceSetName, String commandType) {
        sourceSetName == DEFAULT_SOURCESET_NAME ?
            "terraform${commandType.capitalize()}" :
            "terraform${sourceSetName.capitalize()}${commandType.capitalize()}"
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

    /** Creates a sourceset using specific conventions
     *
     * For any sourceset other than {@code main}, tasks will be named using a pattern such as
     *   {@code terraform<SourceSetName>Init} and source directories will be {@code src/tf/<sourceSetName>}.
     *
     * @param project Project Project to attache source set to.
     * @param sourceSetName Name of Terraform source set.
     */
    static void createSourceSetByConvention(Project project, String sourceSetName) {
        final TerraformSourceSets tss = project.extensions.getByType(TerraformSourceSets)
        if (GradleVersion.current() < GradleVersion.version('4.10')) {
            createSourceSetAndTasks(sourceSetName, project, tss)
        } else {
            registerSourceSetAndTasks(sourceSetName, project, tss)
        }
    }

    @CompileDynamic
    private static void createSourceSetAndTasks(
        String name,
        Project project,
        TerraformSourceSets tss
    ) {
        TerraformSourceDirectorySet sourceSet = tss.create(name)

        DefaultTerraformTasks.values().each {
            def newTask = (project.tasks.create(
                taskName(name, it.name),
                it.type
            )) as AbstractTerraformTask
            newTask.sourceSet = sourceSet
            newTask.group = TERRAFORM_TASK_GROUP
            newTask.description = "${it.description} for '${name}'"
            if (it != DefaultTerraformTasks.INIT) {
                newTask.mustRunAfter taskName(name, TERRAFORM_INIT)
            }
        }
    }

    @CompileDynamic
    private static void registerSourceSetAndTasks(
        String name,
        Project project,
        TerraformSourceSets tss
    ) {
        NamedDomainObjectProvider<TerraformSourceDirectorySet> sourceSet = tss.register(name)

        DefaultTerraformTasks.values().each {
            project.tasks.register(
                taskName(name, it.name),
                it.type,
                new Action<AbstractTerraformTask>() {
                    @Override
                    void execute(AbstractTerraformTask t) {
                        t.sourceSet = sourceSet.get()
                        t.group = TERRAFORM_TASK_GROUP
                        t.description = "${it.description} for '${name}'"
                        if (it != DefaultTerraformTasks.INIT) {
                            t.mustRunAfter taskName(name, TERRAFORM_INIT)
                        }
                    }
                }
            )
        }
    }

    private enum DefaultTerraformTasks {

        INIT( 'init', TerraformInit, 'Initialises Terraform'),
        PLAN( 'plan', TerraformPlan, 'Generates Terraform execution plan'),
        APPLY('apply',TerraformApply,'Builds or changes infrastructure'),
        IMPORT('import', TerraformImport,'Imports a resource')

        final String name
        final Class type
        final String description

        private DefaultTerraformTasks(String name, Class type, String description) {
            this.name = name
            this.type = type
            this.description = description
        }
    }

    private static final String TERRAFORM_INIT = DefaultTerraformTasks.INIT.name
}