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
import org.gradle.api.Action
import org.gradle.api.Transformer
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecSpec
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.gradle.terraform.config.ResourceFilter
import org.ysb33r.gradle.terraform.config.StateOptionsFull
import org.ysb33r.gradle.terraform.internal.TerraformConvention

import javax.inject.Inject
import java.util.concurrent.Callable

import static org.ysb33r.gradle.terraform.config.multilevel.TerraformExtensionConfigTypes.VARIABLES

/** Equivalent of {@code terraform plan}.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformPlan extends AbstractTerraformTask {

    @Inject
    TerraformPlan(String workspaceName) {
        super(
            'plan',
            [Lock, StateOptionsFull, ResourceFilter],
            [VARIABLES],
            workspaceName
        )
        supportsInputs()
        supportsColor()
        inputs.files(taskProvider('init'))
    }

    /** Where the plan file will be written to.
     *
     * @return Location of plan file.
     */
    @OutputFile
    Provider<File> getPlanOutputFile() {
        final String ws = workspaceName == TerraformConvention.DEFAULT_WORKSPACE ? '' : ".${workspaceName}"
        dataDir.map({ File reportDir ->
            new File(reportDir, "${sourceSet.name}${ws}.tf.plan")
        } as Transformer<File, File>)
    }

    /** Where the textual representation of the plan will be written to.
     *
     * @return Location of text file.
     */
    @OutputFile
    Provider<File> getPlanReportOutputFile() {
        final String ws = workspaceName == TerraformConvention.DEFAULT_WORKSPACE ? '' : ".${workspaceName}"
        reportsDir.map({ File reportDir ->
            new File(reportDir, "${sourceSet.name}${ws}.tf.plan.${jsonReport ? 'json' : 'txt'}")
        } as Transformer<File, File>)
    }

    /** This is the location of an internal tracker file used to keep state between apply & destroy cycles.
     *
     * @return Location of tracker file.
     */
    @Internal
    Provider<File> getInternalTrackerFile() {
        final String ws = workspaceName == TerraformConvention.DEFAULT_WORKSPACE ? '' : ".${workspaceName}"
        project.provider({ ->
            new File(dataDir.get(), "${ws}.tracker")
        } as Callable<File>)
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
            new File(dataDir.get(), "__.${workspaceName}.tfvars")
        } as Callable<File>)
    }

    /** Select specific resources.
     *
     * @param resourceNames List of resources to target.
     */
    @Option(option = 'target', description = 'List of resources to target')
    void setTargets(List<String> resourceNames) {
        extensions.getByType(ResourceFilter).target(resourceNames)
    }

    /** Mark resources to be replaces.
     *
     * @param resourceNames List of resources to target.
     */
    @Option(option = 'replace', description = 'List of resources to replace')
    void setReplacements(List<String> resourceNames) {
        extensions.getByType(ResourceFilter).replace(resourceNames)
    }

    /** Where to write the report in human-readable or JSON format.
     *
     * @param state Set to {@code true} to output in JSON.
     */
    @Option(option = 'json', description = 'Output readable plan in JSON')
    void setJson(boolean state) {
        this.jsonReport = state
    }

    @Override
    void exec() {
        createVarsFile()
        super.exec()

        File planOut = planOutputFile.get()
        File textOut = planReportOutputFile.get()

        textOut.withOutputStream { OutputStream report ->
            Action<ExecSpec> showExecSpec = configureShowCommand(planOut, report)
            projectOperations.exec(showExecSpec).assertNormalExitValue()
        }

        logger.lifecycle(
            "The plan file has been generated into ${planOut.toURI()}"
        )
        logger.lifecycle("The textual representation of the plan file has been generated into ${textOut.toURI()}")
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
        super.addCommandSpecificsToExecSpec(execSpec)
        if (jsonReport) {
            execSpec.cmdArgs(JSON_FORMAT)
        }
        execSpec.identity {
            cmdArgs "-out=${planOutputFile.get()}"
            cmdArgs "-var-file=${variablesFile.get().absolutePath}"
        }
        execSpec
    }

    private void createVarsFile() {
        variablesFile.get().withWriter { writer ->
            tfVarProviders*.get().flatten().each { writer.println(it) }
        }
    }

    private Action<ExecSpec> configureShowCommand(File planFile, OutputStream reportStream) {
        final List<String> cmdParams = [NO_COLOR]

        if (jsonReport) {
            cmdParams.add(JSON_FORMAT)
        }

        cmdParams.add(planFile.absolutePath)
        TerraformExecSpec execSpec = createExecSpec()
        execSpec.standardOutput(reportStream)
        addExecutableToExecSpec(execSpec)
        configureExecSpecForCmd(
            execSpec,
            'show',
            cmdParams
        )
        addSessionCredentialsIfAvailable(execSpec)

        new Action<ExecSpec>() {
            @Override
            void execute(ExecSpec spec) {
                execSpec.copyToExecSpec(spec)
            }
        }
    }

    protected boolean jsonReport = false
}
