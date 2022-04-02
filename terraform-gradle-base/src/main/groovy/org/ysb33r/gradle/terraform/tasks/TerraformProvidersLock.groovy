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
import org.gradle.api.tasks.Internal
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.TerraformMajorVersion

/** Equivalent of {@code terraform providers lock}.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.14.0
 */
@CompileStatic
class TerraformProvidersLock extends AbstractTerraformProviderTask {
    TerraformProvidersLock() {
        super('lock')
        enabled = !projectOperations.offline
        this.providerLockFile = sourceDir.map { new File(it, '.terraform.lock.hcl') }
    }

    /** Configuration for Terraform backend.
     *
     * See {@link https://www.terraform.io/docs/backends/config.html#partial-configuration}
     *
     * @return Location of configuration file. Can be {@code null} if none is required.
     *
     * @since 0.4.0
     */
    @Internal
    Provider<File> getProviderLockFile() {
        this.providerLockFile
    }

    @Override
    void exec() {
        if (terraformMajorVersion < TerraformMajorVersion.VERSION_14) {
            logger.error('Cannot run this task with Terraform < 0.14')
        } else {
            super.exec()
        }
    }

    /** Add specific command-line options for the command.
     * If {@code --refresh-dependencies} was specified on the command-line the {@code -upgrade} will be passed
     * to {@code terraform init}.
     *
     * @param execSpec
     * @return execSpec
     */
    @Override
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
        super.addCommandSpecificsToExecSpec(execSpec)
        for (String platform : (requiredPlatforms)) {
            execSpec.cmdArgs "-platform=${platform}"
        }

        File fsMirror = terraformExtension.localMirrorDirectory.getOrNull()
        if (fsMirror) {
            execSpec.cmdArgs("-fs-mirror=${fsMirror.absolutePath}")
        }

        String netMirror = terraformExtension.netMirror.getOrNull()
        if (netMirror) {
            execSpec.cmdArgs("-net-mirror=${netMirror}")
        }

        execSpec
    }

    private final Provider<File> providerLockFile
}
