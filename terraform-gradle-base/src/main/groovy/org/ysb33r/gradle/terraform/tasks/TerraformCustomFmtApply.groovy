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
import org.gradle.api.tasks.TaskProvider
import org.gradle.process.ExecResult
import org.ysb33r.gradle.terraform.TerraformExecSpec

import javax.inject.Inject

/**
 * Checks the format of Terraform source in an arbitrary collection of directories.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.10
 */
@CompileStatic
class TerraformCustomFmtApply extends AbstractTerraformCustomFmt {

    @Inject
    TerraformCustomFmtApply(TaskProvider<TerraformCustomFmtCheck> checkTaskProvider) {
        super()
        this.checkTaskProvider = checkTaskProvider

        this.sourceDirectoriesProvider = projectOperations.providerTools.flatMap(checkTaskProvider) {
            it.sourceDirectories
        }
        projectOperations.tasks.ignoreEmptyDirectories(inputs, sourceDirectories)
    }

    @Override
    final Provider<Set<File>> getSourceDirectories() {
        this.sourceDirectoriesProvider
    }

    protected void addCommandSpecificsForFmt(TerraformExecSpec execSpec) {
        execSpec.cmdArgs '-write=true'

        if (checkTaskProvider.get().recursive) {
            execSpec.cmdArgs '-recursive'
        }

        if (logger.infoEnabled) {
            execSpec.cmdArgs '-list=true'
        } else {
            execSpec.cmdArgs '-list=false'
        }
    }

    @Override
    protected void handleExecResult(ExecResult result) {
        result.assertNormalExitValue()
    }

    private final Provider<Set<File>> sourceDirectoriesProvider
    private final TaskProvider<TerraformCustomFmtCheck> checkTaskProvider
}
