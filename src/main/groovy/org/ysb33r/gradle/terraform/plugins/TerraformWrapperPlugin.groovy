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
package org.ysb33r.gradle.terraform.plugins

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.ysb33r.gradle.terraform.tasks.TerraformCacheBinary
import org.ysb33r.gradle.terraform.tasks.TerraformWrapper

@CompileStatic
class TerraformWrapperPlugin implements Plugin<Project> {
    public static final String WRAPPER_TASK_NAME = 'terraformWrapper'
    public static final String CACHE_BINARY_TASK_NAME = 'cacheTerraformBinary'

    @Override
    void apply(Project project) {
        project.apply plugin: TerraformBasePlugin

        TerraformCacheBinary terraformCacheBinary = project.tasks.create(CACHE_BINARY_TASK_NAME, TerraformCacheBinary)
        project.tasks.create(WRAPPER_TASK_NAME, TerraformWrapper, terraformCacheBinary)
    }
}
