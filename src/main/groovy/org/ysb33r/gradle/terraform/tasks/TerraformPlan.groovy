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
package org.ysb33r.gradle.terraform.tasks

import groovy.transform.CompileStatic
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.gradle.terraform.config.ResourceFilter
import org.ysb33r.gradle.terraform.config.StateOptionsFull
import org.ysb33r.gradle.terraform.config.Variables

/** Equivalent of {@code terraform plan}.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformPlan extends AbstractTerraformTask {

    TerraformPlan() {
        super()
        terraformCommand = 'plan'
        supportsInputs()
        supportsColor()
        withConfigExtensions(Lock, Variables, StateOptionsFull, ResourceFilter)
    }

    /** Set to {@code true} if a plan to destroy all resources must be produced.
     *
     */
    @Input
    boolean destructionPlan = false

    /** Where the plan file will be written to.
     *
     * @return Location of plan file.
     */
    @OutputFile
    Provider<File> getPlanOutputFile() {
        reportsDir.map { File reportDir ->
            new File(reportDir, "${sourceSet.name}.tf.plan")
        }
    }

    @Option(option = 'targets', description = 'List of resources to target')
    void setTargets(List<String> resourceNames) {
        extensions.getByType(ResourceFilter).targets = resourceNames
    }

    @Override
    void exec() {
        super.exec()
        File out = planOutputFile.get()
        logger.lifecycle("The ${destructionPlan ? 'destruction' : ''} plan file has been generated into ${out}")
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
            cmdArgs "-out=${planOutputFile.get()}"
        }
        if (destructionPlan) {
            execSpec.cmdArgs '-destroy'
        }
        execSpec
    }
}
