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
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.config.Json
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.gradle.terraform.config.Parallel
import org.ysb33r.gradle.terraform.config.Refresh

import javax.inject.Inject

/** Equivalent of {@code terraform apply}.
 *
 * A {@code TerraformApply} task will be bound to {@link TerraformPlan} task
 * in order to retrieve most of its configuration.
 */
@CompileStatic
abstract class TerraformApply extends TerraformTask {

    @InputFile
    private final Provider<File> planFile

    @Inject
    @SuppressWarnings('DuplicateStringLiteral')
    TerraformApply() {
        super('apply', [Lock, Refresh, Parallel, Json])
        supportsAutoApprove()
        supportsInputs()
        supportsColor()
        planFile = this.getPlanFile()
        inputs.files(taskProvider('plan'))
        mustRunAfter(taskProvider('plan'))
    }

    @Override
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
        super.addCommandSpecificsToExecSpec(execSpec)
        execSpec.cmdArgs(planFile.get().absolutePath)
        execSpec
    }
}
