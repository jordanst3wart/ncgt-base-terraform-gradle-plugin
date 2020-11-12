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
import org.gradle.api.provider.Provider

import javax.inject.Inject
import java.time.LocalDateTime
import java.util.concurrent.Callable

/** Equivalent of {@code terraform apply}.
 *
 * A {@code TerraformApply} task will be bound to {@link TerraformPlan} task
 * in order to retrieve most of its configuration.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformApply extends AbstractTerraformApplyTask {

    @Inject
    TerraformApply(TerraformPlanProvider plan) {
        super(plan, 'apply')
        supportsAutoApprove()

        tracker = project.provider({ ->
            plan.get().internalTrackerFile.get()
        } as Callable<File>)

        doLast {
            tracker.get().text = LocalDateTime.now().toString()
        }

        outputs.file(tracker).optional()
        inputs.files(taskProvider('init'))
        inputs.files(taskProvider('plan'))
    }

    private final Provider<File> tracker
}
