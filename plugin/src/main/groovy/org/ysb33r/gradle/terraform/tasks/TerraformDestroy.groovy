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
import org.gradle.api.tasks.Internal
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.config.Json
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.gradle.terraform.config.Parallel
import org.ysb33r.gradle.terraform.config.Refresh

import javax.inject.Inject
import java.util.concurrent.Callable

/** Equivalent of {@code terraform destroy}.
 *  Note: DOES NOT USE A PLAN FILE
 *
 * @since 0.1
 */
@CompileStatic
abstract class TerraformDestroy extends TerraformTask {
    @Internal
    Provider<File> getVariablesFile() {
        project.provider({ ->
            new File(sourceSet.get().dataDir.get(), '__.tfvars')
        } as Callable<File>)
    }

    @Inject
    @SuppressWarnings('DuplicateStringLiteral')
    TerraformDestroy() {
        super('destroy', [Lock, Refresh, Parallel, Json])
        supportsAutoApprove()
        supportsInputs()
        supportsColor()
        inputs.files(taskProvider('destroyPlan'))
        mustRunAfter(taskProvider('destroyPlan'))
        addCommandLineProvider(sourceSetVariables())
    }

    @Override
    void exec() {
        createVarsFile()
        super.exec()
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
        execSpec.cmdArgs("-var-file=${variablesFile.get().absolutePath}")
        execSpec
    }

    private void createVarsFile() {
        variablesFile.get().withWriter { writer ->
            tfVarProviders*.get().flatten().each { writer.println(it) }
        }
    }
}
