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
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.TerraformExecSpec

import javax.inject.Inject

/** Equivalent of {@code terraform validate}.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformValidate extends AbstractTerraformTask {

    @Inject
    TerraformValidate(String workspaceName) {
        super('validate', [], [], workspaceName)
        supportsColor()
        inputs.files(taskProvider('init'))
    }

    /** Whether output should be in JSON
     *
     * This option can be set from the command-line with {@code --upgrade=true}.
     */
    @Option(option = 'json', description = 'Force validate output to be in JSON format')
    @Internal
    boolean json = false

    /** Add specific command-line options for the command.
     *
     * @param execSpec
     * @return execSpec
     */
    @Override
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
        super.addCommandSpecificsToExecSpec(execSpec)

        if (json) {
            execSpec.cmdArgs JSON_FORMAT
        }
        execSpec
    }
}
