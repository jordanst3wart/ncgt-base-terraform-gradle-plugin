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
package org.ysb33r.gradle.terraform.tasks

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecSpec
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.gradle.terraform.config.Parallel
import org.ysb33r.gradle.terraform.config.Refresh

import javax.inject.Inject
import java.util.concurrent.Callable

/** Equivalent of {@code terraform plan}.
 *
 */
@CompileStatic
class TerraformPlan extends AbstractTerraformTask {

    @Inject
    TerraformPlan() {
        super(
            'plan',
            [Lock, Refresh, Parallel]
        )
        supportsInputs()
        supportsColor()
        inputs.files(taskProvider('init'))
        addCommandLineProvider(sourceSetVariables())
    }

    /** Where the plan file will be written to.
     *
     * @return Location of plan file.
     */
    @OutputFile
    File getPlanOutputFile() {
        new File(sourceSet.get().dataDir.get(), "${sourceSet.get().name}.tf.plan")
    }

    /** This is the location of an variables file used to keep anything provided via the build script.
     *
     * @return Location of variables file.
     *
     * @since 0.13.0
     */
    @Internal
    Provider<File> getVariablesFile() {
        project.provider({ ->
            new File(sourceSet.get().dataDir.get(), '__.tfvars')
        } as Callable<File>)
    }

    /** Where to write the report in human-readable or JSON format.
     *
     * @param state Set to {@code true} to output in JSON.
     */
    @Option(option = 'json', description = 'Output readable plan in JSON')
    void setJson(boolean state) {
        this.jsonPlan = state
    }

    @Override
    void exec() {
        createVarsFile()
        super.exec()

        File planOut = planOutputFile

        logger.lifecycle(
            "The plan file has been generated into ${planOut.toURI()}"
        )
    }

    /** Add specific command-line options for the command.
     * If {@code --refresh-dependencies} was specified on the command-line the {@code -upgrade} will be passed
     * to {@code terraform init}.
     *
     * @param execSpec
     * @return execSpec
     */
    @Override
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
        if (project.hasProperty('tf.plan.refresh')) {
            logger.lifecycle('tf.plan.refresh property found setting refresh to false')
            extensions.getByType(Refresh).refresh = false
        }
        super.addCommandSpecificsToExecSpec(execSpec)
        if (jsonPlan) {
            execSpec.cmdArgs(JSON_FORMAT)
        }
        execSpec.identity {
            cmdArgs "-out=${planOutputFile}"
            cmdArgs "-var-file=${variablesFile.get().absolutePath}"
        }
        execSpec
    }

    private void createVarsFile() {
        variablesFile.get().withWriter { writer ->
            tfVarProviders*.get().flatten().each { writer.println(it) }
        }
    }

    protected boolean jsonPlan = false
}
