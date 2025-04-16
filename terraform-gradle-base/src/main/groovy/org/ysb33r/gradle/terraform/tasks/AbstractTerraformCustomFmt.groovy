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
import org.gradle.api.Action
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.internal.TerraformConfigUtils

/**
 * Base class for custom formatting tasks.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.10
 */
// TODO this task should be removed
@CompileStatic
abstract class AbstractTerraformCustomFmt extends AbstractTerraformBaseTask {

    protected AbstractTerraformCustomFmt() {
        super('fmt', [])
        this.dataDir = projectOperations.buildDirDescendant("tf/${name}")
    }

    @Override
    void exec() {
        workingDirForCommand.get().mkdirs()

        for (File target : sourceDirectories.get()) {
            TerraformExecSpec execSpec = createExecSpec()
            addExecutableToExecSpec(execSpec)
            Map<String, String> tfEnv = terraformEnvironment
            execSpec.identity {
                command terraformCommand
                workingDir workingDirForCommand
                environment tfEnv
                cmdArgs defaultCommandParameters
            }
            execSpec.environment(environment)
            addCommandSpecificsToExecSpec(execSpec)
            execSpec.cmdArgs target.absolutePath
            execSpec.ignoreExitValue(true)

            Action<ExecSpec> runner = new Action<ExecSpec>() {
                @Override
                void execute(ExecSpec spec) {
                    execSpec.copyToExecSpec(spec)
                }
            }
            handleExecResult(projectOperations.exec(runner))
        }
    }

    /**
     * The list of toplevel directories to check
     *
     * @return List of directories
     */
    @Internal
    abstract Provider<Set<File>> getSourceDirectories()

    abstract protected void addCommandSpecificsForFmt(TerraformExecSpec execSpec)

    abstract protected void handleExecResult(ExecResult result)

    @Override
    protected Provider<File> getWorkingDirForCommand() {
        dataDir
    }

    @Override
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
        addCommandSpecificsForFmt(execSpec)
        execSpec
    }

    // this logic is duplicated in AbstractTerraformTask
    @Override
    protected Map<String, String> getTerraformEnvironment() {
        [
            TF_DATA_DIR       : dataDir.get().absolutePath,
            TF_CLI_CONFIG_FILE: TerraformConfigUtils.locateTerraformConfigFile(terraformrc).absolutePath
        ]
    }

    protected final Provider<File> dataDir
}
