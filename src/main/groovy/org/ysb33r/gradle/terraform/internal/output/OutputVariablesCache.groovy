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
package org.ysb33r.gradle.terraform.internal.output

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.transform.Synchronized
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.process.ExecSpec
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.tasks.TerraformOutput

import static org.ysb33r.gradle.terraform.internal.TerraformUtils.terraformEnvironment
import static org.ysb33r.grolifant.api.v4.FileUtils.toSafeFileName

/** An internal cache of output variables
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.9.0
 */
@CompileStatic
class OutputVariablesCache {

    OutputVariablesCache(
        Project project,
        Provider<TerraformOutput> outputTask
    ) {
        this.project = project
        this.outputTaskProvider = outputTask
    }

    @Synchronized
    Map<String, ?> getMap() {
        if (outputs.isEmpty()) {
            populateMap()
        }

        this.outputs
    }

    private void populateMap() {
        String sourceSetName = outputTask.sourceSet.name
        TerraformExecSpec execSpec = buildExecSpec()
        Action<ExecSpec> runner = new Action<ExecSpec>() {
            @Override
            void execute(ExecSpec spec) {
                execSpec.copyToExecSpec(spec)
            }
        }

        project.logger.debug "Loading output variables from terraform sourceset ${outputTask.sourceSet.name}"
        File tmpFile = new File(
            "${project.buildDir}/tmp/tf-output-var-cache",
            "${toSafeFileName(sourceSetName)}.tmp.---.json"
        )
        tmpFile.parentFile.mkdirs()
        try {
            tmpFile.withOutputStream { strm ->
                execSpec.standardOutput(strm)
                project.exec(runner).assertNormalExitValue()
            }
            outputs.putAll(new JsonSlurper().parse(tmpFile) as Map<String, ?>)
            project.logger.debug "Loaded sourceset ${outputTask.sourceSet.name} output variables with ${outputs}"
        } finally {
            tmpFile.delete()
        }
    }

    private TerraformExecSpec buildExecSpec() {
        Map<String, String> tfEnv = terraformEnvironment(
            project,
            "${outputTask.sourceSet.name}-output-cache",
            outputTask.dataDir,
            outputTask.logDir,
            null
        )

        TerraformExecSpec execSpec = new TerraformExecSpec(project, terraformExt.resolver)

        execSpec.identity {
            executable terraformExt.resolvableExecutable.executable.absolutePath
            command 'output'
            cmdArgs '-json'
            workingDir outputTask.sourceSet.srcDir
            environment tfEnv
            environment outputTask.environment
        }

        execSpec
    }

    private TerraformOutput getOutputTask() {
        outputTaskProvider.get()
    }

    private TerraformExtension getTerraformExt() {
        outputTask.extensions.getByType(TerraformExtension)
    }

    private final Map<String, ?> outputs = [:]
    private final Provider<TerraformOutput> outputTaskProvider
    private final Project project
}
