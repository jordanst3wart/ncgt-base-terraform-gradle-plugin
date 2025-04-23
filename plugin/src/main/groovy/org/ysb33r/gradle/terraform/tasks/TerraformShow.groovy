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
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.TerraformExecSpec

import javax.inject.Inject
import java.util.concurrent.Callable

/** Equivalent of {@code terraform show /path/to/terraform.plan}.
 */
@CompileStatic
abstract class TerraformShow extends TerraformTask {

    @Inject
    TerraformShow() {
        super('show', [])
        outputFile = project.objects.property(File)
        outputFile.set(
            project.provider({ ->
                new File(sourceSet.get().reportsDir.get(),
                    "${sourceSet.get().name}.${json ? 'tf.json' : 'tf'}")
            } as Callable<File>))

        supportsColor(false)
        captureStdOutTo(reportOutputFile)
        inputs.files(taskProvider('init'))
        alwaysOutOfDate()
    }

    /** Whether output should be in JSON
     *
     * This option can be set from the command-line with {@code --json}.
     */
    // only task where json is true
    @Option(option = 'json', description = 'Force output to be in JSON format')
    @Internal
    boolean json = true

    /** Get the location where the report file needs to be generated.
     *
     * @return File provider
     */
    @OutputFile
    Provider<File> getReportOutputFile() {
        this.outputFile
    }

    @Override
    void exec() {
        super.exec()
        URI fileLocation = reportOutputFile.get().toURI()
        logger.lifecycle(
            "The textual representation of the state file has been generated into ${fileLocation}"
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
            execSpec.cmdArgs(JSON_FORMAT)
        }

        execSpec.cmdArgs(this.planFile.get().absolutePath)
        execSpec
    }

    private final Property<File> outputFile
}
