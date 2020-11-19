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
package org.ysb33r.gradle.terraform.internal.remotestate

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet
import org.ysb33r.gradle.terraform.internal.TerraformConvention
import org.ysb33r.gradle.terraform.remotestate.RemoteStateS3
import org.ysb33r.gradle.terraform.remotestate.TerraformRemoteStateExtension
import org.ysb33r.gradle.terraform.tasks.RemoteStateAwsS3ConfigGenerator
import org.ysb33r.gradle.terraform.tasks.TerraformInit

import static org.ysb33r.gradle.terraform.internal.TerraformConvention.DEFAULT_SOURCESET_NAME
import static org.ysb33r.gradle.terraform.internal.TerraformConvention.TERRAFORM_INIT
import static org.ysb33r.gradle.terraform.internal.TerraformConvention.taskName
import static org.ysb33r.gradle.terraform.remotestate.TerraformRemoteStateExtension.findExtension

/** Conventions for when {@link org.ysb33r.gradle.terraform.plugins.TerraformRemoteStateAwsS3Plugin} is applied.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.8.0
 */
@CompileStatic
class S3Conventions {
    static void taskCreator(Project project, TerraformSourceDirectorySet tdds) {
        String name = tdds.name
        String configTaskName = newTaskName(name)
        RemoteStateAwsS3ConfigGenerator configTask = project.tasks.create(
            configTaskName,
            RemoteStateAwsS3ConfigGenerator
        )
        defaultRemoteStateName(name).execute(configTask)
        terraformInit(configTask).execute((TerraformInit) project.tasks.getByName(taskName(name, TERRAFORM_INIT)))
        addVariables(tdds, configTask.remoteStateName, configTask.awsRegion, configTask.s3BucketName)
    }

    @CompileDynamic
    @TypeChecked
    static void taskLazyCreator(Project project, TerraformSourceDirectorySet tsds) {
        String name = tsds.name
        if (!project.tasks.findByName(taskName(name, TERRAFORM_INIT))) {
            TerraformConvention.createTasksByConvention(project, tsds)
        }

        String configTaskName = newTaskName(name)
        TaskProvider<RemoteStateAwsS3ConfigGenerator> configTask = project.tasks.register(
            configTaskName,
            RemoteStateAwsS3ConfigGenerator
        )
        configTask.configure(defaultRemoteStateName(name))
        project.tasks.named(taskName(name, TERRAFORM_INIT)).configure(new Action<TerraformInit>() {
            @Override
            void execute(TerraformInit init) {
                terraformInit(configTask.get()).execute(init)
            }
        })
        lazyAddVariablesToSourceSet(configTask).execute(tsds)
    }

    @SuppressWarnings('ClosureAsLastMethodParameter')
    private static Action<TerraformSourceDirectorySet> lazyAddVariablesToSourceSet(
        Provider<RemoteStateAwsS3ConfigGenerator> configTask
    ) {
        new Action<TerraformSourceDirectorySet>() {
            @Override
            void execute(TerraformSourceDirectorySet tsds) {
                addVariables(
                    tsds,
                    { -> configTask.get().remoteStateName },
                    { -> configTask.get().awsRegion },
                    { -> configTask.get().s3BucketName }
                )
            }
        }
    }

    private static String newTaskName(String sourceSetName) {
        "create${taskName(sourceSetName, 's3BackendConfiguration').capitalize()}"
    }

    private static Action<RemoteStateAwsS3ConfigGenerator> defaultRemoteStateName(
        String sourceSetName
    ) {
        new Action<RemoteStateAwsS3ConfigGenerator>() {
            @Override
            void execute(RemoteStateAwsS3ConfigGenerator task) {
                RemoteStateS3 s3 = RemoteStateS3.findExtension(task.project, sourceSetName)
                String folderName = sourceSetName == DEFAULT_SOURCESET_NAME ?
                    'tfS3BackendConfiguration' :
                    "tf${sourceSetName.capitalize()}S3BackendConfiguration"

                task.remoteStateName = { Project p ->
                    TerraformRemoteStateExtension remote = findExtension(p, sourceSetName)
                    sourceSetName == DEFAULT_SOURCESET_NAME ? remote.prefix.get() :
                        "${remote.prefix.get()}-${sourceSetName}"
                }.curry(task.project)
                task.awsRegion = s3.region
                task.s3BucketName = s3.bucket
                task.destinationDir = { Project p ->
                    new File(p.buildDir, "tfRemoteState/${folderName}")
                }.curry(task.project)
            }
        }
    }

    private static Action<TerraformInit> terraformInit(RemoteStateAwsS3ConfigGenerator configTask) {
        new Action<TerraformInit>() {
            @Override
            void execute(TerraformInit terraformInit) {
                terraformInit.dependsOn(configTask)
                terraformInit.backendConfigFile = configTask.backendConfigFile
            }
        }
    }

    static private void addVariables(TerraformSourceDirectorySet tdds, Object name, Object region, Object bucket) {
        tdds.variables.map('remote_state', name: name, aws_region: region, 'aws_bucket': bucket)
    }
}
