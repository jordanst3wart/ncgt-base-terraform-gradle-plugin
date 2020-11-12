/*
 * Copyright 2017-2020 the original author or authors.
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
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.TerraformExecSpec

import java.util.concurrent.Callable

/** Equivalent of {@code terraform show /path/to/terraform.tfstate}.
 *
 * @since 0.3
 */
@CompileStatic
class TerraformShowState extends AbstractTerraformTask {

    TerraformShowState() {
        super('show', [], [])

        outputFile = project.objects.property(File)
        outputFile.set(
            project.provider({ ->
                new File(reportsDir.get(), "${sourceSet.name}.status.${json ? 'tf.json' : 'tf'}")
            } as Callable<File>))

        supportsColor(false)
        captureStdOutTo(statusReportOutputFile)
        inputs.files(taskProvider('init'))
        inputs.files(taskProvider('plan'))
        inputs.files(taskProvider('apply'))
    }

    /** Whether output should be in JSON
     *
     * This option can be set from the command-line with {@code --json}.
     */
    @Option(option = 'json', description = 'Force output to be in JSON format')
    @Internal
    boolean json = false

    /** Get the location where the report file needs to be generated.
     *
     * @return File provider
     */
    @OutputFile
    Provider<File> getStatusReportOutputFile() {
        this.outputFile
    }

    @Override
    void exec() {
        super.exec()
        logger.lifecycle(
            "The textual representation of the plan file has been generated into ${statusReportOutputFile.get()}"
        )
    }

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

    private final Property<File> outputFile
}
