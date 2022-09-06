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
package org.ysb33r.gradle.terraform.internal.remotestate

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet
import org.ysb33r.gradle.terraform.internal.TerraformConvention
import org.ysb33r.gradle.terraform.tasks.RemoteStateAwsS3ConfigGenerator

import static org.ysb33r.gradle.terraform.internal.TerraformConvention.TERRAFORM_INIT
import static org.ysb33r.gradle.terraform.internal.TerraformConvention.taskName

/** Conventions for when {@link org.ysb33r.gradle.terraform.plugins.TerraformRemoteStateAwsS3Plugin} is applied.
 *
 * @author Schalk W. Cronjé
 *
 * @deprecated
 *
 * @since 0.8.0
 */
@CompileStatic
@Deprecated
class S3Conventions {
    static void legacyTaskCreator(
        Project project,
        TerraformSourceDirectorySet tsds
    ) {
        String name = tsds.name
        if (!project.tasks.findByName(taskName(name, TERRAFORM_INIT, null))) {
            TerraformConvention.createTasksByConvention(project, tsds)
        }

        project.tasks.register(
            newTaskName(name),
            RemoteStateAwsS3ConfigGenerator,
            project.tasks.named(TerraformConvention.backendTaskName(name))
        )
    }

    @Deprecated
    private static String newTaskName(String sourceSetName) {
        "create${taskName(sourceSetName, 's3BackendConfiguration', null).capitalize()}"
    }
}
