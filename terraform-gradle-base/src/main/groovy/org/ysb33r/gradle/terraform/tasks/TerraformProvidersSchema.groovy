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
import org.gradle.api.tasks.OutputFile
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.TerraformMajorVersion

/** Equivalent of {@code terraform providers schema}.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.14.0
 */
@CompileStatic
class TerraformProvidersSchema extends AbstractTerraformProviderTask {
    TerraformProvidersSchema() {
        super('schema')
        outputFile = project.objects.property(File)
        projectOperations.updateFileProperty(
            outputFile,
            reportsDir.map { new File(it, "${sourceSet.name}.schema.json") }
        )
        captureStdOutTo(schemaOutputFile)

//        inputs.files(sourceDir.map { File f ->
//            projectOperations.fileTree(f).matching { PatternFilterable pat ->
//                pat.include('*.tf')
//            }
//        })
        inputs.files(taskProvider('init'))
    }

    /** Get the location where the report file needs to be generated.
     *
     * @return File provider
     */
    @OutputFile
    Provider<File> getSchemaOutputFile() {
        this.outputFile
    }

    @Override
    void exec() {
        if (terraformMajorVersion == TerraformMajorVersion.VERSION_11_OR_OLDER) {
            logger.error('Cannot run this task with Terraform < 0.12')
        } else {
            super.exec()
            URI fileLocation = schemaOutputFile.get().toURI()
            logger.lifecycle(
                "The textual representation of the plan file has been generated into ${fileLocation}"
            )
        }
    }

/** Add specific command-line options for the command.
 *
 * @param execSpec
 * @return execSpec
 */
    @Override
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
        super.addCommandSpecificsToExecSpec(execSpec)
        execSpec.cmdArgs('-json')
        execSpec
    }

    private final Property<File> outputFile
}
