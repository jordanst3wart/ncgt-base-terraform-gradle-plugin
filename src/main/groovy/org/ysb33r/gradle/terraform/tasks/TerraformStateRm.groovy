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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.TerraformExecSpec

import javax.inject.Inject

/** The {@code terraform state rm} command.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.5.0
 */
@CompileStatic
class TerraformStateRm extends AbstractTerraformStateTask {

    @Inject
    TerraformStateRm(String workspaceName) {
        super('rm', workspaceName)
    }

    /**
     * The resource path
     *
     * @return The resource path that was set.
     *
     * @since 0.10
     */
    @Input
    String getResourcePath() {
        this.path ?: "${type}.${resourceName}"
    }

    @Option(option = 'type', description = 'Type of resource to remove')
    void setResourceType(String id) {
        logger.warn '--type / setResourceType is deprecated. Use --path / setResourcePath instead.'
        this.type = id
    }

    @Option(option = 'name', description = 'Name of resource to remove')
    void setResourceName(String id) {
        logger.warn '--name / setResourceName is deprecated. Use --path / setResourcePath instead.'
        this.resourceName = id
    }

    @Override
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
        super.addCommandSpecificsToExecSpec(execSpec)
        execSpec.cmdArgs(resourcePath)
        execSpec
    }

    private String path

    @Deprecated
    private String type

    @Deprecated
    private String resourceName
}
