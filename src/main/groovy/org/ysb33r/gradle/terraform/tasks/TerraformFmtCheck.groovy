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
import org.ysb33r.gradle.terraform.TerraformExecSpec

/** The {@code terraform fmt -check} command.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.10.0
 */
@CompileStatic
class TerraformFmtCheck extends AbstractTerraformTask {

    TerraformFmtCheck() {
        super('fmt', [], [])
    }

    @Input
    boolean recursive = false

    @Override
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
        super.addCommandSpecificsToExecSpec(execSpec)

        execSpec.cmdArgs '-check'

        if (logger.infoEnabled) {
            execSpec.cmdArgs '-diff'
        }

        if (!logger.quietEnabled) {
            execSpec.cmdArgs '-list=true'
        }

        if (recursive) {
            execSpec.cmdArgs '-recursive'
        }

        execSpec
    }
}
