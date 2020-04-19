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
package org.ysb33r.gradle.terraform.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.remotestate.TerraformRemoteStateExtension
import org.ysb33r.gradle.terraform.tasks.AbstractRemoteStateConfigGenerator

import static org.ysb33r.gradle.terraform.internal.TerraformConfigUtils.locateTerraformRCGenerator

class TerraformRemoteStateBasePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.apply plugin: TerraformBasePlugin

        ((ExtensionAware) project.extensions.getByType(TerraformExtension))
            .extensions.create(TerraformRemoteStateExtension.NAME, TerraformRemoteStateExtension, project)

        project.tasks.withType(AbstractRemoteStateConfigGenerator) { AbstractRemoteStateConfigGenerator t ->
            t.dependsOn(locateTerraformRCGenerator(t.project))
        }
    }
}
