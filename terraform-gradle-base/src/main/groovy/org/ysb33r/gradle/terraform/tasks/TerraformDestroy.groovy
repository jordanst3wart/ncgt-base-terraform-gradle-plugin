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
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.gradle.terraform.config.ResourceFilter
import org.ysb33r.gradle.terraform.config.StateOptionsFull

import javax.inject.Inject
import java.util.concurrent.Callable

/** Equivalent of {@code terraform destroy}.
 *
 * A {@code TerraformApply} task will be bound to {@link TerraformPlan} task
 * in order to retrieve most of its configuration.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformDestroy extends AbstractTerraformTask {

    @Inject
    TerraformDestroy(TerraformPlanProvider plan, String workspaceName) {
        super('destroy', [Lock, StateOptionsFull], [], workspaceName)
        supportsInputs()
        supportsColor()

        addCommandLineProvider(
            projectOperations.provider({ ->
                plan.get().extensions.getByType(TerraformExtension).allVariables.commandLineArgs +
                    plan.get().extensions.getByType(Lock).commandLineArgs +
                    plan.get().extensions.getByType(StateOptionsFull).commandLineArgs
            } as Callable<List<String>>)
        )

        tfVarProviders = projectOperations.provider({ ->
            plan.get().extensions.getByType(TerraformExtension).allVariables.tfVars.flatten()
        } as Callable<List<String>>)

        doLast {
            plan.get().internalTrackerFile.get().delete()
        }

        inputs.files(taskProvider('init'))

        variablesFile = projectOperations.providerTools.map(plan) {
            new File(it.variablesFile.get().parentFile, "_d_.${workspaceName}.tfVars")
        }
    }

    /** Set auto-approve mode.
     *
     * Once set it cannot be unset for the duration of the Gradle task graph execution.
     *
     * @param state {@code true} will auto-approve destruction.
     */
    @Option(option = 'approve', description = 'Auto-approve destruction of resources')
    void setAutoApprove(Boolean state) {
        if (state) {
            supportsAutoApprove()
        }
    }

    @Option(option = 'target', description = 'List of resources to target')
    void setTargets(List<String> resourceNames) {
        extensions.getByType(ResourceFilter).targets = resourceNames
    }

    @Override
    void exec() {
        createVarsFile()
        super.exec()
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
        execSpec.identity {
            cmdArgs "-var-file=${variablesFile.get().absolutePath}"
        }
        execSpec
    }

    private void createVarsFile() {
        def lines = tfVarProviders.get()
        variablesFile.get().withWriter { writer ->
            lines.each { writer.println(it) }
        }
    }

    private final Provider<List<String>> tfVarProviders
    private final Provider<File> variablesFile
}
