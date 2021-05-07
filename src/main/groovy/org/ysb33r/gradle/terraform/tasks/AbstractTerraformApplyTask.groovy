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
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.gradle.terraform.config.ResourceFilter
import org.ysb33r.gradle.terraform.config.StateOptionsFull

import java.util.concurrent.Callable

/** Base class for the {@code terraform} {@code apply} and {@code destroy} commands.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.2
 */
@CompileStatic
class AbstractTerraformApplyTask extends AbstractTerraformTask {

    /** Binds a {@link TerraformPlan} task so that resources can be applied or destroyed,
     *
     * @param plan {@link TerraformPlan} task that this is bound to.
     * @param cmd Command that is implemented
     */
    protected AbstractTerraformApplyTask(TerraformPlanProvider planProvider, String cmd) {
        super(cmd, [ResourceFilter], [])
        supportsInputs()
        supportsColor()

        TerraformPlan plan = planProvider.get()
        addCommandLineProvider(
            projectOperations.provider({ ->
                plan.extensions.getByType(TerraformExtension).allVariables.commandLineArgs +
                    plan.extensions.getByType(Lock).commandLineArgs +
                    plan.extensions.getByType(StateOptionsFull).commandLineArgs
            } as Callable<List<String>>)
        )
    }

    @Option(option = 'target', description = 'List of resources to target')
    void setTargets(List<String> resourceNames) {
        extensions.getByType(ResourceFilter).targets = resourceNames
    }
}
