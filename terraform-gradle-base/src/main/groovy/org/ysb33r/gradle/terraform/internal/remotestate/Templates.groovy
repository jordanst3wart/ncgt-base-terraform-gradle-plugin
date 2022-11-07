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
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Action
import org.gradle.api.file.CopySpec
import org.gradle.api.provider.Provider
import org.ysb33r.gradle.terraform.errors.TerraformConfigurationException
import org.ysb33r.gradle.terraform.remotestate.BackendAttributesSpec
import org.ysb33r.gradle.terraform.remotestate.BackendTextTemplate
import org.ysb33r.grolifant.api.core.ProjectOperations

@CompileStatic
class Templates {

    /** Generates a configuration file
     *
     * @param taskName Name of task that is request the generation
     * @param projectOperations Project context
     * @param templateFile A template file if present
     * @param textTemplate A text template if available.
     * @param outputFile Final output file
     * @param beginToken Starting delimiter for tokens
     * @param endToken Terminating delimiter for tokens
     * @param tokens List of replacement tokens
     * @return Location of generated file
     */
    @SuppressWarnings('ParameterCount')
    static File generateFromTemplate(
        String taskName,
        ProjectOperations projectOperations,
        BackendAttributesSpec backendAttributes,
        Provider<File> templateFile,
        Provider<BackendTextTemplate> textTemplate,
        Provider<File> outputFile,
        String beginToken,
        String endToken,
        Map<String, Object> tokens
    ) {

        if (!templateFile.present && !textTemplate.present) {
            throw new TerraformConfigurationException('Either a text template or a file template has to be provided')
        }

        File template = templateFile.present ? templateFile.get() :
            useStringTemplate(projectOperations, taskName, textTemplate.get(), backendAttributes)

        File backendConfigFile = outputFile.get()
        def configGenerator = new Action<CopySpec>() {
            @Override
            void execute(CopySpec copySpec) {
                copySpec.with {
                    from template
                    into backendConfigFile.parentFile
                    rename '(.+)', backendConfigFile.name
                    filter ReplaceTokens,
                        beginToken: beginToken, endToken: endToken,
                        tokens: projectOperations.stringTools.stringizeValues(tokens)
                }
            }
        }

        backendConfigFile.parentFile.mkdirs()
        projectOperations.copy(configGenerator)
        backendConfigFile
    }

    private static File useStringTemplate(
        ProjectOperations projectOperations,
        String filenamePrefix,
        BackendTextTemplate textTemplate,
        BackendAttributesSpec attributes
    ) {
        final safeName = projectOperations.fsOperations.toSafeFileName(filenamePrefix)
        File target = projectOperations
            .buildDirDescendant("tmp/${safeName}.template.tf")
            .get()
        target.parentFile.mkdirs()
        target.text = textTemplate.template(attributes)
        target
    }
}
