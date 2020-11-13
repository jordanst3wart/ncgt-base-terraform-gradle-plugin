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
package org.ysb33r.gradle.terraform.tasks

import groovy.transform.CompileStatic
import org.gradle.api.tasks.options.Option

import javax.inject.Inject

/** Equivalent of {@code terraform destroy}.
 *
 * A {@code TerraformApply} task will be bound to {@link TerraformPlan} task
 * in order to retrieve most of its configuration.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformDestroy extends AbstractTerraformApplyTask {

    @Inject
    TerraformDestroy(TerraformPlanProvider plan) {
        super(plan, 'destroy')

        doLast {
            plan.get().internalTrackerFile.get().delete()
        }

        inputs.files(taskProvider('init'))
        inputs.files(taskProvider('apply'))
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
}
