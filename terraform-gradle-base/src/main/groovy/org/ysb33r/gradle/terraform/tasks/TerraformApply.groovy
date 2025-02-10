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
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.gradle.terraform.config.ResourceFilter
import org.ysb33r.gradle.terraform.config.StateOptionsFull

import javax.inject.Inject
import java.util.concurrent.Callable

/** Equivalent of {@code terraform apply}.
 *
 * A {@code TerraformApply} task will be bound to {@link TerraformPlan} task
 * in order to retrieve most of its configuration.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformApply extends AbstractTerraformTask {

    @InputFile
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private Provider<File> planFile
    private boolean json = false

    @Inject
    @SuppressWarnings('DuplicateStringLiteral')
    TerraformApply() {
        super('apply', [Lock, StateOptionsFull, ResourceFilter])
        supportsAutoApprove()
        supportsInputs()
        supportsColor()
        planFile = getPlanFile()
        planFile = project.provider({ ->
            new File(dataDir.get(), "${sourceSet.name}.tf.plan")
        } as Callable<File>)
        inputs.files(taskProvider('plan'))
        mustRunAfter(taskProvider('plan'))
        addCommandLineProvider(sourceSetVariables())
    }

    /** Select specific resources.
     *
     * @param resourceNames List of resources to target.
     *
     * @since 0.10.0
     */
    @Option(option = 'target', description = 'List of resources to target')
    void setTargets(List<String> resourceNames) {
        extensions.getByType(ResourceFilter).target(resourceNames)
    }

    /** Mark resources to be replaced.
     *
     * @param resourceNames List of resources to target.
     *
     * @since 0.10.0
     */
    @Option(option = 'replace', description = 'List of resources to replace')
    void setReplacements(List<String> resourceNames) {
        extensions.getByType(ResourceFilter).replace(resourceNames)
    }

    /**
     * Output progress in json as per https://www.terraform.io/docs/internals/machine-readable-ui.html
     *
     * @param state Set to {@code true} to output in JSON.
     */
    @Option(option = 'json', description = 'Output progress in JSON')
    void setJson(boolean state) {
        this.json = state
    }

    Provider<File> getPlanFile() {
        project.provider({ ->
            new File(dataDir.get(), "${sourceSet.name}.tf.plan")
        } as Callable<File>)
    }

    @Override
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
        super.addCommandSpecificsToExecSpec(execSpec)

        if (json) {
            execSpec.cmdArgs(JSON_FORMAT)
        }

        execSpec.cmdArgs(planFile.get().absolutePath)
        execSpec
    }
}
