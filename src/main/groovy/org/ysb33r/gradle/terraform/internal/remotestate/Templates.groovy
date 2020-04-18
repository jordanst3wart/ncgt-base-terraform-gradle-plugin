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

import groovy.transform.CompileStatic
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.provider.Provider
import org.ysb33r.grolifant.api.FileUtils
import org.ysb33r.grolifant.api.MapUtils

@CompileStatic
class Templates {

    /** Generates a configuration file
     *
     * @param taskName Name of task that is request the generation
     * @param project Project context
     * @param templateResourcePath Resource path for default template
     * @param templateFile ALternative template file if present
     * @param outputFile Final output file
     * @param beginToken Starting delimiter for tokens
     * @param endToken Terminating delimiter for tokens
     * @param tokens LIst of replacement tokens
     * @return Location of generated file
     */
    @SuppressWarnings('ParameterCount')
    static File generateFromTemplate(
        String taskName,
        Project project,
        String templateResourcePath,
        Provider<File> templateFile,
        Provider<File> outputFile,
        String beginToken,
        String endToken,
        Map<String, Object> tokens
    ) {
        File template = templateFile.present ? templateFile.get() :
            useDefaultTemplate(project, taskName, templateResourcePath)

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
                        tokens: MapUtils.stringizeValues(tokens)
                }
            }
        }

        backendConfigFile.parentFile.mkdirs()
        project.copy configGenerator
        backendConfigFile
    }

    private static File useDefaultTemplate(Project project, String filenamePrefix, String templateResourcePath) {
        File target = new File(project.buildDir, "tmp/${FileUtils.toSafeFileName(filenamePrefix)}.template.tf")
        target.parentFile.mkdirs()
        Templates.getResourceAsStream(templateResourcePath).withCloseable { strm ->
            target.withOutputStream { output ->
                output << strm
            }
        }
        target
    }
}
