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
package org.ysb33r.gradle.terraform

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.ysb33r.gradle.terraform.tasks.RemoteStateTask
import org.ysb33r.gradle.terraform.tasks.TerraformApply
import org.ysb33r.gradle.terraform.tasks.TerraformInit
import org.ysb33r.gradle.terraform.tasks.TerraformPlan
import spock.lang.Specification

class TerraformPluginSpec extends Specification {

    Project project = ProjectBuilder.builder().build()

    void setup() {
        project.apply plugin: 'org.ysb33r.terraform'
        project.allprojects {
            terraformSourceSets {
                main {
                    srcDir = file('foo/bar')
                    backendText("hi")
                }
                release
            }
        }
    }

    void 'Create additional source sets'() {
        expect:
        def tss = project.terraformSourceSets
        tss.getByName('main').srcDir.get() == project.file('foo/bar')
        tss.getByName( 'release').srcDir.get() == project.file('src/release/tf')
        project.tasks.named('initRelease').get() instanceof TerraformInit
        project.tasks.named('planRelease').get() instanceof TerraformPlan
        project.tasks.named('applyRelease').get() instanceof TerraformApply
    }

    /*void 'Plugin is applied'() {
        expect: 'Backend tasks are not created if no backend text set'
        def backendTask = project.tasks.named('createTfFooBackendConfiguration').get() as RemoteStateTask
        backendTask.name == 'createTfFooBackendConfiguration'
        backendTask.backendFileRequired.get() == true
        backendTask.backendConfigFile.get() == new File(project.buildDir, "tfRemoteState/tfFooBackendConfiguration/terraform-backend-config.tf")

        def task = project.tasks.named('initFoo').get() as TerraformInit
        task.backendConfigFile.get() == new File(project.buildDir, "tfRemoteState/tfFooBackendConfiguration/terraform-backend-config.tf")
        task.useBackendFile.get() == true // should be true
        try {
            project.tasks.named('createTfReleaseBackendConfiguration').get()
            false == true
        } catch (org.gradle.api.UnknownTaskException e) {
        }
    }*/

    /*void 'The default destination directory is based upon the source set name'() {
        expect:
        def remoteStateTask = project.tasks.withType(RemoteStateTask)
        remoteStateTask.size() == 2
        File main = project.tasks.createTfMainBackendConfiguration.backendConfigFile.get()
        main.name == 'terraform-backend-config.tf'
        main.parentFile.name == 'tfMainBackendConfiguration'
        main.parentFile.parentFile == new File(project.buildDir, 'tfRemoteState')
        project.tasks.createTfMainBackendConfiguration.getDestinationDir().get() == main.parentFile
    }

    void 'terraformInit has configuration file correctly setup'() {
        expect:
        project.tasks.initMain.backendConfigFile.get() == project.tasks.createTfMainBackendConfiguration.backendConfigFile.get()

        try {
            project.tasks.initRelease.backendConfigFile.get() == project.tasks.createTfReleaseBackendConfiguration.backendConfigFile.get()
            false == true
        } catch (groovy.lang.MissingPropertyException e) {
        }
    }*/
}