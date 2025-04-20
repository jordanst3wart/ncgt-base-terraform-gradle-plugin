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
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.ysb33r.grolifant.api.core.ProjectOperations

import java.util.concurrent.Callable

import static org.ysb33r.gradle.terraform.plugins.TerraformBasePlugin.TERRAFORM_TASK_GROUP

/**
 * Generates a remote state file containing partial configuration for backend.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.12 (Refactored from {@code AbstractRemoteStateConfigGenerator}).
 */
@CompileStatic
class RemoteStateTask extends DefaultTask {

    RemoteStateTask() {
        group = TERRAFORM_TASK_GROUP
        description = 'Generates configuration for backend state provider'
        this.destinationDir = project.objects.property(File)
        this.outputFile = project.objects.property(File)
        this.projectOperations = ProjectOperations.find(project)
        this.backendText = project.objects.property(String)
        this.outputFile.set(
            project.providers.provider(new Callable<File>() {
                @Override
                File call() throws Exception {
                    new File(destinationDir.get(), 'terraform-backend-config.tf')
                }
            })
        )
    }

    /**
     * Returns whether a backend partial configuration file should be generated.
     *
     * @return {@code true} is one is required.
     */
    @Internal
    Provider<Boolean> getBackendFileRequired() {
        if (!backendText.present) {
            return project.providers.provider { false }
        }
        backendText.map {
            !it.blank
        } as Provider<Boolean>
    }

    void setBackendText(Provider<String> backendText) {
        this.backendText = backendText
    }

    @Input
    Provider<String> getBackendText() {
        this.backendText
    }

    /** Override the output directory.
     *
     * @param dir Anything convertible to a file path.
     */
    void setDestinationDir(File destDir) {
        this.destinationDir.set(destDir)
    }

    @Internal
    Property<File> getDestinationDir() {
        this.destinationDir
    }

    /** The location of the backend configuration file.
     *
     * @return Configuration file.
     */
    @OutputFile
    Property<File> getBackendConfigFile() {
        this.outputFile
    }

    @TaskAction
    void exec() {
        String backendFileContent = backendText.get()
        if (backendFileContent != null ) {
            File outputFileActual = outputFile.get()
            outputFileActual.parentFile.mkdirs()
            outputFileActual.with {
                parentFile.mkdirs()
                withWriter { writer ->
                    writer.println(backendFileContent)
                }
            }
        } else {
            throw new IllegalStateException('backend text is null')
        }
    }

    private final ProjectOperations projectOperations
    private final Property<File> destinationDir
    private final Property<File> outputFile
    private Provider<String> backendText
}
