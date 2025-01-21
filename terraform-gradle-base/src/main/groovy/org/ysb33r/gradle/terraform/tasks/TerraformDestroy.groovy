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
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.gradle.terraform.config.ResourceFilter
import org.ysb33r.gradle.terraform.config.StateOptionsFull

import javax.inject.Inject

/** Equivalent of {@code terraform destroy}.
 *  Note: DOES NOT USE A PLAN FILE
 *
 * @since 0.1
 */
@CompileStatic
class TerraformDestroy extends AbstractTerraformTask {

    private boolean json = false

    @Inject
    @SuppressWarnings('DuplicateStringLiteral')
    TerraformDestroy() {
        super('destroy', [Lock, StateOptionsFull, ResourceFilter], [])
        supportsAutoApprove()
        supportsInputs()
        supportsColor()
        inputs.files(taskProvider('destroyPlan'))
        mustRunAfter(taskProvider('destroyPlan'))
    }

    @Option(option = 'target', description = 'List of resources to target')
    void setTargets(List<String> resourceNames) {
        extensions.getByType(ResourceFilter).targets = resourceNames
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

    /** Add specific command-line options for the command.
     * If {@code --refresh-dependencies} was specified on the command-line the {@code -upgrade} will be passed
     * to {@code terraform init}.
     *
     * @param execSpec
     * @return execSpec
     */
    @Override
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
        if (json) {
            execSpec.cmdArgs(JSON_FORMAT)
        }

        super.addCommandSpecificsToExecSpec(execSpec)
        execSpec
    }
}
