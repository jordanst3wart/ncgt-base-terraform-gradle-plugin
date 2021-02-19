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
package org.ysb33r.gradle.terraform.tasks

import groovy.transform.CompileStatic
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.internal.Transform
import org.ysb33r.grolifant.api.core.ProjectOperations
import org.ysb33r.grolifant.api.v4.wrapper.script.AbstractScriptWrapperTask

import javax.inject.Inject

import static org.ysb33r.gradle.terraform.plugins.TerraformRCPlugin.TERRAFORM_RC_TASK

@CompileStatic
class TerraformWrapper extends AbstractScriptWrapperTask {

    @Inject
    TerraformWrapper(TerraformCacheBinary cacheTask) {
        super()
        this.terraformExtension = project.extensions.getByType(TerraformExtension)
        this.cacheTask = cacheTask
        this.rootRelativePath = project.relativePath(project.rootDir)
        this.projectOperations = ProjectOperations.find(project)
        outputs.files(Transform.toList(TEMPLATE_MAPPING.values()) {
            new File(project.projectDir, it)
        })
        inputs.file(cacheTask.locationPropertiesFile)
        dependsOn(cacheTask)
        deleteTemplatesAfterUsage = true
        useWrapperTemplatesInResources(TEMPLATE_RESOURCE_PATH, TEMPLATE_MAPPING)
    }

    @Override
    protected String getBeginToken() {
        TEMPLATE_TOKEN_DELIMITER
    }

    @Override
    protected String getEndToken() {
        TEMPLATE_TOKEN_DELIMITER
    }

    @Override
    protected Map<String, String> getTokenValuesAsMap() {
        File cacheTaskParent = cacheTask.locationPropertiesFile.get().parentFile
        [
            APP_BASE_NAME               : 'terraform',
            GRADLE_WRAPPER_RELATIVE_PATH: rootRelativePath,
            DOT_GRADLE_RELATIVE_PATH    : projectOperations.relativePath(cacheTaskParent),
            APP_LOCATION_FILE           : cacheTask.locationPropertiesFile.get().name,
            CACHE_TASK_NAME             : cacheTask.name,
            TERRAFORMRC_TASK_NAME       : TERRAFORM_RC_TASK
        ]
    }

    private final TerraformExtension terraformExtension
    private final TerraformCacheBinary cacheTask
    private final String rootRelativePath
    private final ProjectOperations projectOperations
    private static final String TEMPLATE_TOKEN_DELIMITER = '~~'
    private static final String TEMPLATE_RESOURCE_PATH = '/terraform-wrapper'
    private static final Map<String, String> TEMPLATE_MAPPING = [
        'wrapper-template.sh' : 'terraformw',
        'wrapper-template.bat': 'terraformw.bat',
//        'wrapper-template.ps': 'terraformw.ps',
    ]
}
