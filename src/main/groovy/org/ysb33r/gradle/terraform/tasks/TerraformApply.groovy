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
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.gradle.terraform.config.ResourceFilter
import org.ysb33r.gradle.terraform.config.StateOptionsFull

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
class TerraformApply extends AbstractTerraformTask {

    /*
-lock=false - Disables Terraform's default behavior of attempting to take a read/write lock on the state for the duration of the operation.

-lock-timeout=DURATION - Unless locking is disabled with -lock=false, instructs Terraform to retry acquiring a lock for a period of time before returning an error. The duration syntax is a number followed by a time unit letter, such as "3s" for three seconds.

-parallelism=n
     */

    @Inject
    TerraformApply(TerraformPlanProvider plan) {
        super('apply', [Lock, StateOptionsFull], [])
        supportsAutoApprove()
        supportsInputs()
        supportsColor()
        planProvider = plan

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
    private final TerraformPlanProvider planProvider
}
