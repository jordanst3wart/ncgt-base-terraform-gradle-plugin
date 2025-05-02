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
package org.ysb33r.gradle.terraform

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.internal.os.OperatingSystem
import org.ysb33r.grolifant.api.core.ProjectOperations

import java.util.concurrent.Callable
import static org.ysb33r.grashicorp.HashicorpUtils.escapedFilePath

/** Extension that describes a {@code terraformrc} file.
 *
 * @author Schalk W. Cronjé
 */
@CompileStatic
class TerraformRCExtension {
    public static final String TERRAFORM_RC_TASK = 'generateTerraformConfig'

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
     * Default is {@code true}.
     */
    boolean useGlobalConfig = false

    /** Plugin cache may break dependency lock file.
     *
     * Setting this option gives Terraform CLI permission to create an incomplete dependency
     * lock file entry for a provider if that would allow Terraform to use the cache to install that provider.
     *
     * In that situation the dependency lock file will be valid for use on the current system but may not be
     * valid for use on another computer with a different operating system or CPU architecture, because it
     * will include only a checksum of the package in the global cache.
     *
     * Default is {@code false}.
     */
    boolean pluginCacheMayBreakDependencyLockFile = false

    TerraformRCExtension(Project project) {
        this.projectOperations = ProjectOperations.find(project)

        this.terraformRC = project.providers.provider({ File defaultLoc ->
            defaultLoc
        }.curry(new File(projectOperations.projectCacheDir, '.terraformrc')) as Callable<File>)

        this.pluginCacheDir = project.objects.property(File)
        projectOperations.fsOperations.updateFileProperty(
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
        projectOperations.fsOperations.updateFileProperty(
            this.pluginCacheDir,
            projectOperations.provider({ ->
                projectOperations.fsOperations.file(dir)
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
        writer.println "plugin_cache_may_break_dependency_lock_file = ${this.pluginCacheMayBreakDependencyLockFile}"
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
