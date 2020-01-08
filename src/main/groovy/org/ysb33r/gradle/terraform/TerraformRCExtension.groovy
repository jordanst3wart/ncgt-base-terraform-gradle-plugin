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
package org.ysb33r.gradle.terraform

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.ysb33r.grolifant.api.FileUtils
import org.ysb33r.grolifant.api.OperatingSystem

import java.util.concurrent.Callable

import static org.ysb33r.grashicorp.HashicorpUtils.escapedFilePath

/** Extension that describes a {@code terraformrc} file.
 *
 * @author Schalk W. Cronjé
 */
@CompileStatic
class TerraformRCExtension {

    /** Project this extension is attached to.
     *
     */
    final Project project

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
        this.project = project
        this.terraformRC = project.providers.provider({ File defaultLoc ->
            defaultLoc
        }.curry(new File(FileUtils.projectCacheDirFor(project.rootProject), '.terraformrc'))
            as Callable<File>
        )
        this.pluginCacheDir = project.provider({ File defaultLoc ->
            defaultLoc
        }.curry(new File(
            project.gradle.gradleUserHomeDir,
            'caches/terraform.d'
        )))
    }

    /** Sets the location of the Terraform plugin cache directory
     *
     * @param dir Anything that is convertible using {@code project.file}.
     */
    void setPluginCacheDir(Object dir) {
        this.pluginCacheDir = project.providers.provider({ ->
            project.file(dir)
        } as Callable<File>)
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
        this.credentials.each { String key, String token ->
            writer.println "credentials \"${key}\" {"
            writer.println "  token = \"${token}\""
            writer.println '}'
        }
        writer
    }

    private Provider<File> pluginCacheDir
    private final Provider<File> terraformRC
    private final Map<String, String> credentials = [:]
}
