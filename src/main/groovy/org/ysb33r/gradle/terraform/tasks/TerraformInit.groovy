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
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.config.Lock

/** Equivalent of {@code terraform init}.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformInit extends AbstractTerraformTask {

    TerraformInit() {
        super()
        terraformCommand = 'init'
        supportsInputs()
        supportsColor()
        withConfigExtensions(Lock)
    }

    // TODO: Implement -from-module=MODULE-SOURCE as Gradle @Option

    /** Whether modules should be upgraded
     *
     * This option can be set from the command-line with {@code --upgrade=true}.
     */
    @Option(option = 'upgrade', description = 'Force upgrade of modules and plugins when not offline')
    @Internal
    boolean upgrade = false

    /**
     * Skip initialisation of child modules.
     */
    @Internal
    boolean skipChildModules = false

    /** Whether backends should be configured.
     *
     * This option can be set from the command-line with {@code --configure-backends=true}
     */
    @Option(option = 'configure-backends', description = 'Whether backends should be configured')
    @Internal
    boolean configureBackend = true

    // TODO: Other backend settings
    // -force-copy
    // -reconfigure
    // -backend-config

    @Internal
    boolean verifyPlugins = true

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
        if (project.gradle.startParameter.offline) {
            logger.warn(
                'Gradle is running in offline mode. ' +
                    (upgrade ? 'Upgrade will not be attempted. ' : '') +
                    (skipChildModules ? '' : 'Modules will not be retrieved. ')
            )
            execSpec.cmdArgs '-get=false'
        } else {
            if (upgrade) {
                execSpec.cmdArgs('-upgrade')
            }
            execSpec.cmdArgs "-get=${!skipChildModules}"
        }

        execSpec.cmdArgs "-backend=${configureBackend}"
        execSpec.cmdArgs "-verify-plugins=${verifyPlugins}"
        execSpec
    }

//    private boolean checkVariables = true
}
