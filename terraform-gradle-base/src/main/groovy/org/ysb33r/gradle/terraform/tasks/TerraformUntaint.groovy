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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.config.Lock

import javax.inject.Inject

/** The {@code terraform untaint} command.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.10.0
 */
@CompileStatic
class TerraformUntaint extends AbstractTerraformTask {

    @Inject
    TerraformUntaint() {
        super('untaint', [Lock], [])
        supportsColor()
    }

    @Input
    String getResourcePath() {
        this.resourcePath
    }

    @Option(option = 'path', description = 'Resource to untaint')
    void setResourcePath(String id) {
        this.resourcePath = id
    }

    @Option(option = 'allow-missing', description = 'Allow task to succeed even if the resource is missing')
    void setAllowMissing(boolean flag) {
        this.allowMissing = flag
    }

    @Option(option = 'ignore-remote-version',
        description = 'Continue if remote and local Terraform versions differ from Terraform Cloud')
    void setIgnoreRemote(boolean flag) {
        this.ignoreRemoteVersion = flag
    }

    @Override
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
        super.addCommandSpecificsToExecSpec(execSpec)

        if (allowMissing) {
            execSpec.cmdArgs '-allow-missing'
        }

        if (ignoreRemoteVersion) {
            execSpec.cmdArgs '-ignore-remote-version'
        }

        execSpec.cmdArgs resourcePath
        execSpec
    }

    private String resourcePath
    private boolean allowMissing = false
    private boolean ignoreRemoteVersion = false
}
