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
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.gradle.terraform.config.ResourceFilter
import org.ysb33r.gradle.terraform.config.StateOptionsFull

import static org.ysb33r.gradle.terraform.config.multilevel.TerraformExtensionConfigTypes.VARIABLES

/** Equivalent of {@code terraform apply}.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformApply extends AbstractTerraformTask {

    TerraformApply() {
        super(
            'apply',
            [Lock, StateOptionsFull, ResourceFilter],
            [VARIABLES]
        )
        supportsInputs()
        supportsColor()
        supportsAutoApprove()
    }

    @Option(option = 'targets', description = 'List of resources to target')
    void setTargets(List<String> resourceNames) {
        extensions.getByType(ResourceFilter).targets = resourceNames
    }
}
