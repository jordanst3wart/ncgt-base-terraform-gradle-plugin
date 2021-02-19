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
package org.ysb33r.gradle.terraform

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.ysb33r.grolifant.api.core.OperatingSystem
import org.ysb33r.grolifant.api.core.ProjectOperations

import java.util.concurrent.Callable

import static org.ysb33r.gradle.terraform.plugins.TerraformRCPlugin.TERRAFORM_RC_TASK
import static org.ysb33r.grashicorp.HashicorpUtils.escapedFilePath

/** Extension that describes a {@code terraformrc} file.
 *
 * @author Schalk W. Cronjé
 */
@CompileStatic
class TerraformRCExtension {

    /** Disable checkpoint.
     *
     * When set to true, disables upgrade and security bulletin checks that require reaching out to
     * HashiCorp-provided network services.
     *
     * Default is {@code true}.
     */
    boolean disableCheckPoint = true

    /** Disable checkpoint signature.
     *
     * When set to true, allows the upgrade and security bulletin checks described above but disables the use of an
     * anonymous id used to de-duplicate warning messages.
     *
     * Default is {@code false}.
     */
    boolean disableCheckPointSignature = false

    /** Select source of Terraform configuration.
     *
     * When set to {@code true} use global Terraform configuration rather than a project configuration.
     *
     * Default is {@code false}.
     */
    boolean useGlobalConfig = false

    TerraformRCExtension(Project project) {
        this.projectOperations = ProjectOperations.find(project)

        this.terraformRC = project.providers.provider({ File defaultLoc ->
            defaultLoc
        }.curry(new File(projectOperations.projectCacheDir, '.terraformrc')) as Callable<File>)

        this.pluginCacheDir = project.objects.property(File)
        projectOperations.updateFileProperty(
            this.pluginCacheDir,
            projectOperations.gradleUserHomeDir.map { new File(it, 'caches/terraform.d') }
        )

        this.terraformRCTask = project.provider({ TaskContainer t ->
            t.getByName(TERRAFORM_RC_TASK)
        }.curry(project.tasks) as Callable<Task>)
    }

    /** Sets the location of the Terraform plugin cache directory
     *
     * @param dir Anything that is convertible using {@code project.file}.
     */
    void setPluginCacheDir(Object dir) {
        projectOperations.updateFileProperty(
            this.pluginCacheDir,
            projectOperations.provider({ ->
                projectOperations.file(dir)
            } as Callable<File>)
        )
    }

    /** Location of Terraform plugin cache directory
     *
     * @return Location of cache directory as a file provider.
     */
    Provider<File> getPluginCacheDir() {
        this.pluginCacheDir
    }

    /** Location of the {@code terraformrc} file if it shoudl be written by the project.
     *
     * @return Location of {@code terraformrc} file. Never {@code null}.
     */
    Provider<File> getTerraformRC() {
        this.terraformRC
    }

    /**
     * Task that creates the {@code .terraformrc} file.
     *
     * @return Provider*
     * @since 0.17.0
     */
    Provider<Task> getTerraformRCTask() {
        this.terraformRCTask
    }

    /** Adds a credential set to the configuration file.
     *
     * @param key Remote Terraform system name
     * @param token Token for remote Terraform system.
     */
    void credentials(String key, String token) {
        credentials.put(key, token)
    }

    /** Writes to the content of the Terraform configuration to an HCL writer.
     *
     * @param writer Writer instance to send output to.
     * @return The writer
     */
    Writer toHCL(Writer writer) {
        writer.println "disable_checkpoint = ${this.disableCheckPoint}"
        writer.println "disable_checkpoint_signature = ${this.disableCheckPointSignature}"
        writer.println "plugin_cache_dir = \"${escapedFilePath(OperatingSystem.current(), pluginCacheDir.get())}\""
        this.credentials.each { String key, String token ->
            writer.println "credentials \"${key}\" {"
            writer.println "  token = \"${token}\""
            writer.println '}'
        }
        writer
    }

    private final Property<File> pluginCacheDir
    private final Provider<File> terraformRC
    private final Map<String, String> credentials = [:]
    private final ProjectOperations projectOperations
    private final Provider<Task> terraformRCTask

}
