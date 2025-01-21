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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.TerraformExecSpec

import javax.inject.Inject
import java.util.concurrent.Callable

/** Equivalent of {@code terraform output}.
 *
 * Could be used with command-line arguments {@code --name}.
 *
 * @since 0.9.0
 */
@CompileStatic
class TerraformOutput extends AbstractTerraformTask {

    @Inject
    TerraformOutput() {
        super('output', [], [])
        outputFile = project.objects.property(File)
        outputFile.set(
            project.provider({ ->
                new File(reportsDir.get(), "${sourceSet.name}.outputs.${json ? 'tf.json' : 'tf'}")
            } as Callable<File>)
        )

        supportsColor(false)
        captureStdOutTo(statusReportOutputFile)
        inputs.files(taskProvider('init'))
        mustRunAfter(taskProvider('plan'), taskProvider('apply'))
    }

    /** Whether output should be in JSON
     *
     * This option can be set from the command-line with {@code --json}.
     */
    @Option(option = 'json', description = 'Force output to be in JSON format')
    @Internal
    boolean json = false

    /** List of names to retrieve output for.
     *
     * @return List of names. Can be {@code null} to retrieve all names.
     */
    @Input
    @Optional
    String getOutputName() {
        this.outputName
    }

    /** Set the list of output names to retrieve.
     *
     * This option can be set on the command-line with {@code --name}
     *
     * @param outputName Output name to use.
     */
    @Option(option = 'name', description = 'Name of output variable')
    void setName(String outputName) {
        this.outputName = outputName
    }

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
        URI fileLocation = statusReportOutputFile.get().toURI()
        logger.lifecycle(
            "The textual representation of the plan file has been generated into ${fileLocation}"
        )
    }

    @Override
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
        super.addCommandSpecificsToExecSpec(execSpec)
        if (json) {
            execSpec.cmdArgs JSON_FORMAT
        }
        if (outputName) {
            execSpec.cmdArgs(outputName)
        }
        execSpec
    }

    private String outputName
    private final Property<File> outputFile
}
