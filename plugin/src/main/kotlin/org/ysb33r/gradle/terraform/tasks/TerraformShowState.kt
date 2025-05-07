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

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.TerraformExecSpec

import javax.inject.Inject
import java.io.File
import java.net.URI

/** Equivalent of {@code terraform show /path/to/terraform.tfstate}.
 */
abstract class TerraformShowState : TerraformTask {
    @OutputFile
    val statusReportOutputFile: Property<File> = project.objects.property(File::class.java)

    @Inject
    constructor() : super("show", emptyList()) {
        statusReportOutputFile.set(
            project.provider {
                File(sourceSet.get().reportsDir.get(),
                    "${sourceSet.get().name}.status.${if (json) "tf.json" else "tf"}")
            })

        supportsColor(false)
        captureStdOutTo(statusReportOutputFile)
        inputs.files(taskProvider("init"))
        alwaysOutOfDate()
    }

    /** Whether output should be in JSON
     *
     * This option can be set from the command-line with {@code --json}.
     */
    @Option(option = "json", description = "Force output to be in JSON format")
    @Internal
    var json: Boolean = false

    override fun exec() {
        super.exec()
        val fileLocation: URI = statusReportOutputFile.get().toURI()
        logger.lifecycle(
            "The textual representation of the state file has been generated into $fileLocation"
        )
    }

    /** Add specific command-line options for the command.
     *
     * @param execSpec
     * @return execSpec
     */
    override fun addCommandSpecificsToExecSpec(execSpec: TerraformExecSpec): TerraformExecSpec {
        super.addCommandSpecificsToExecSpec(execSpec)

        if (json) {
            execSpec.cmdArgs(JSON_FORMAT)
        }

        return execSpec
    }
}