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
package org.ysb33r.gradle.terraform.internal.output

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.transform.Synchronized
import groovy.util.logging.Slf4j
import org.gradle.api.Action
import org.gradle.api.provider.Provider
import org.gradle.process.ExecSpec
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.TerraformRCExtension
import org.ysb33r.gradle.terraform.tasks.TerraformOutput
import org.ysb33r.grolifant.api.core.ProjectOperations

import static org.ysb33r.gradle.terraform.internal.TerraformConvention.DEFAULT_WORKSPACE
import static org.ysb33r.gradle.terraform.internal.TerraformUtils.terraformEnvironment
import static org.ysb33r.grolifant.api.v4.FileUtils.toSafeFileName

/** An internal cache of output variables
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.9.0
 */
@CompileStatic
@Slf4j
class OutputVariablesCache {

    OutputVariablesCache(
        ProjectOperations projectOperations,
        TerraformRCExtension terraformrc,
        Provider<TerraformOutput> outputTask
    ) {
        this.projectOperations = projectOperations
        this.outputTaskProvider = outputTask
        this.terraformrc = terraformrc
        this.tmpDirProvider = outputTask.map {
            def sourceSetName = it.sourceSet.name
            projectOperations.buildDirDescendant(
                "tmp/tf-output-var-cache/${toSafeFileName(sourceSetName)}.${it.workspaceName ?: ''}.tmp.---.json"
            ).get()
        }
    }

    @Synchronized
    Map<String, ?> getMap() {
        if (outputs.isEmpty()) {
            populateMap()
        }

        this.outputs
    }

    @SuppressWarnings('LineLength')
    private void populateMap() {
        TerraformExecSpec execSpec = buildExecSpec()
        Action<ExecSpec> runner = new Action<ExecSpec>() {
            @Override
            void execute(ExecSpec spec) {
                execSpec.copyToExecSpec(spec)
            }
        }

        log.debug "Loading output variables from terraform sourceset ${outputTask.sourceSet.name}/${outputTask.workspaceName}"
        File tmpFile = tmpDirProvider.get()
        tmpFile.parentFile.mkdirs()
        try {
            tmpFile.withOutputStream { strm ->
                execSpec.standardOutput(strm)
                projectOperations.exec(runner).assertNormalExitValue()
            }
            outputs.putAll(new JsonSlurper().parse(tmpFile) as Map<String, ?>)
            log.debug "Loaded sourceset ${outputTask.sourceSet.name}/${outputTask.workspaceName} output variables with ${outputs}"
        } finally {
            tmpFile.delete()
        }
    }

    private TerraformExecSpec buildExecSpec() {
        Map<String, String> tfEnv = terraformEnvironment(
            terraformrc,
            "${outputTask.sourceSet.name}-output-cache",
            outputTask.dataDir,
            outputTask.logDir,
            null
        )

        TerraformExecSpec execSpec = new TerraformExecSpec(projectOperations, terraformExt.resolver)
        final String workspaceName = outputTask.workspaceName ?: DEFAULT_WORKSPACE

        execSpec.tap {
            executable terraformExt.resolvableExecutable.executable.absolutePath
            command 'output'
            cmdArgs '-json'
            workingDir outputTask.sourceSet.srcDir
            environment tfEnv
            environment outputTask.environment

            environment(terraformExt.credentialsCacheFor(
                outputTask.sourceSet.name,
                workspaceName,
                outputTask.sourceSet.getCredentialProviders(workspaceName)
            ))
        }
    }

    private TerraformOutput getOutputTask() {
        outputTaskProvider.get()
    }

    private TerraformExtension getTerraformExt() {
        outputTask.extensions.getByType(TerraformExtension)
    }

    private final Map<String, ?> outputs = [:]
    private final Provider<TerraformOutput> outputTaskProvider
    private final ProjectOperations projectOperations
    private final Provider<File> tmpDirProvider
    private final TerraformRCExtension terraformrc
}
