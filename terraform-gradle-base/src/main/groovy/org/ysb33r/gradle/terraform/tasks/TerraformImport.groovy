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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.gradle.terraform.config.StateOptionsConcurrency

import javax.inject.Inject
import java.util.concurrent.Callable

import static org.ysb33r.gradle.terraform.config.multilevel.TerraformExtensionConfigTypes.VARIABLES

/** Equivalent of {@code terraform import}.
 *
 * Should be used with command-line arguments {@code --type}, {@code --name} and {@code --id}.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformImport extends AbstractTerraformTask {

    @Inject
    TerraformImport(String workspaceName) {
        super(
            'import',
            [Lock, StateOptionsConcurrency],
            [VARIABLES],
            workspaceName
        )
        supportsInputs()
        supportsColor()
        alwaysOutOfDate()
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

    @Input
    String getResourceIdentifier() {
        this.id
    }

    @Option(option = 'type', description = 'Type of resource to import')
    @Deprecated
    void setResourceType(String id) {
        logger.warn '--type / setResourceType is deprecated. Use --path / setResourcePath instead.'
        this.type = id
    }

    @Option(option = 'name', description = 'Name of resource to import (Deprecated)')
    @Deprecated
    void setResourceName(String id) {
        logger.warn '--name / setResourceName is deprecated. Use --path / setResourcePath instead.'
        this.resourceName = id
    }

    @Option(option = 'id', description = 'Identifier of resource to import (Deprecated)')
    void setResourceIdentifier(String id) {
        this.id = id
    }

    @Option(option = 'path', description = 'Resource path to import')
    void setResourcePath(String id) {
        this.path = id
    }

    /** This is the location of an variables file used to keep anything provided via the build script.
     *
     * @return Location of variables file.
     *
     * @since 0.13.1
     */
    @Internal
    Provider<File> getVariablesFile() {
        project.provider({ ->
            new File(dataDir.get(), "__.${workspaceName}.tfvars")
        } as Callable<File>)
    }

    @Override
    void exec() {
        createVarsFile()
        super.exec()
    }

    @Override
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
        super.addCommandSpecificsToExecSpec(execSpec)
        execSpec.cmdArgs("-var-file=${variablesFile.get().absolutePath}")
        execSpec.cmdArgs(resourcePath, resourceIdentifier)
        execSpec
    }

    private void createVarsFile() {
        variablesFile.get().withWriter { writer ->
            tfVarProviders*.get().flatten().each { writer.println(it) }
        }
    }

    @Deprecated
    private String type

    @Deprecated
    private String resourceName

    private String path
    private String id
}
