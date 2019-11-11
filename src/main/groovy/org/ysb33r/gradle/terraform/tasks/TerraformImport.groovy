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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.gradle.terraform.config.StateOptionsConcurrency

import static org.ysb33r.gradle.terraform.config.multilevel.TerraformExtensionConfigTypes.VARIABLES

/** Equivalent of {@code terraform import}.
 *
 * Should be used with command-line arguments {@code --type}, {@code --name} and {@code --id}.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformImport extends AbstractTerraformTask {

    TerraformImport() {
        super(
            'import',
            [Lock, StateOptionsConcurrency],
            [VARIABLES]
        )
        supportsInputs()
        supportsColor()
    }

    @Input
    String getResourceType() {
        this.type
    }

    @Input
    String getResourceName() {
        this.resourceName
    }

    @Input
    String getResourceIdentifier() {
        this.id
    }

    @Option(option = 'type', description = 'Type of resource to import')
    void setResourceType(String id) {
        this.type = id
    }

    @Option(option = 'name', description = 'Name of resource to import')
    void setResourceName(String id) {
        this.resourceName = id
    }

    @Option(option = 'id', description = 'Identifier of resource to import')
    void setResourceIdentifier(String id) {
        this.id = id
    }

    @Override
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
        super.addCommandSpecificsToExecSpec(execSpec)
        execSpec.cmdArgs "${resourceType}.${resourceName}", resourceIdentifier
        execSpec
    }

    private String type
    private String resourceName
    private String id

}
