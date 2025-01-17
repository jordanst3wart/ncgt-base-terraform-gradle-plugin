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
import org.gradle.api.Transformer
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.TerraformExecSpec
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

    @Inject
    TerraformApply(TerraformPlanProvider plan, String workspaceName) {
        super('apply', [Lock, StateOptionsFull], [], workspaceName)
        supportsAutoApprove()
        supportsInputs()
        supportsColor()
        planProvider = plan
        planFile = planProvider.map(new Transformer<File, TerraformPlan>() {
            @Override
            File transform(TerraformPlan terraformPlan) {
                terraformPlan.planOutputFile.get()
            }
        })
        tracker = project.provider({ ->
            plan.get().internalTrackerFile.get()
        } as Callable<File>)

        doLast {
            tracker.get().text = LocalDateTime.now().toString()
        }

        outputs.file(tracker).optional()
        inputs.files(taskProvider('plan'))
    }

    /** Select specific resources.
     *
     * @param resourceNames List of resources to target.
     *
     * @since 0.10.0
     */
    @Option(option = 'target', description = 'List of resources to target')
    void setTargets(List<String> resourceNames) {
        planProvider.get().extensions.getByType(ResourceFilter).target(resourceNames)
    }

    /** Mark resources to be replaced.
     *
     * @param resourceNames List of resources to target.
     *
     * @since 0.10.0
     */
    @Option(option = 'replace', description = 'List of resources to replace')
    void setReplacements(List<String> resourceNames) {
        planProvider.get().extensions.getByType(ResourceFilter).replace(resourceNames)
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

    @Override
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
        super.addCommandSpecificsToExecSpec(execSpec)

        if (json) {
            execSpec.cmdArgs(JSON_FORMAT)
        }

        execSpec.cmdArgs(planFile.map(new Transformer<String, File>() {
            @Override
            String transform(File plan) {
                plan.absolutePath
            }
        }))
        execSpec
    }

    @InputFile
    protected Provider<File> getPlanFile() {
        this.planFile
    }

    private final Provider<File> planFile
    private final Provider<File> tracker
    private final TerraformPlanProvider planProvider
    private boolean json = false
}
