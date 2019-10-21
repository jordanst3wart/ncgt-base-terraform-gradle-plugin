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
//
// (C) Copyright Schalk W. Cronje 2017-2019
//
// This software is licensed under the Apache License 2.0
// See http://www.apache.org/licenses/LICENSE-2.0 for license details
//
// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and limitations under the License.
//

package org.ysb33r.gradle.terraform.helpers

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.testfixtures.ProjectBuilder
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.plugins.TerraformBasePlugin

class IntegrationTestBaseSpecification extends DownloadTestSpecification {

    Project project = ProjectBuilder.builder().build()
    TerraformExtension terraformExtension
    Configuration terraformConfiguration

    void setup() {
        project.apply plugin : 'org.ysb33r.terraform.base'
        terraformExtension = project.extensions.getByName(TerraformExtension.NAME)
        terraformConfiguration = project.configurations.getByName(TerraformBasePlugin.TERRAFORM_CONFIGURATION)
        terraformExtension.warnOnNewVersion(false)
    }

//    void processTerraformSource() {
//        project.tasks.getByName('processTerraformSource').execute()
//    }
//
//    void copyTerraformFiles(final String sourceSubDir, final String destinationSubDir = 'src/terraform') {
//        final File sourceDir = new File(DownloadTestSpecification.RESOURCES_DIR,sourceSubDir).absoluteFile
//        final File destinationDir = project.file(destinationSubDir)
//        destinationDir.mkdirs()
//        project.copy {
//            from sourceDir, {
//                include '*.tf'
//                include '*.tfvars'
//            }
//            into destinationDir
//        }
//    }
}