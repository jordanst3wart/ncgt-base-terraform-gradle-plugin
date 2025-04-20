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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.ysb33r.gradle.terraform.errors.TerraformExecutionException
import org.ysb33r.gradle.terraform.internal.Downloader
import org.ysb33r.gradle.terraform.internal.DownloaderBinary
import org.ysb33r.gradle.terraform.internal.DownloaderOpenTofu
import org.ysb33r.grolifant.api.core.ProjectOperations
import org.ysb33r.grolifant.api.v4.exec.DownloadedExecutable
import org.ysb33r.grolifant.api.v4.exec.DownloaderFactory
import org.ysb33r.grolifant.api.v4.exec.ExternalExecutable
import org.ysb33r.grolifant.api.v4.exec.ResolvableExecutable
import org.ysb33r.grolifant.api.v4.exec.ResolveExecutableByVersion
import org.ysb33r.grolifant.api.v4.exec.ResolverFactoryRegistry

import static org.ysb33r.gradle.terraform.internal.TerraformUtils.awsEnvironment
import static org.ysb33r.gradle.terraform.internal.TerraformUtils.googleEnvironment

/** Configure project defaults or task specifics for {@code Terraform}.
 *
 * This also allows the {@code terraform} executable to be set
 *
 * It can be passed by a single map option.
 *
 * <code>
 *   // By tag (Gradle will download and cache the correct distribution).
 *   executable tag : '0.10.1'
 *
 *   // By a physical path (
 *   executable path : '/path/to/terraform'
 *
 *   // By using searchPath (will attempt to locate in search path).
 *   executable searchPath()
 * </code>
 *
 * If the build runs on a platform that supports downloading of the {@code terraform} executable
 * the default will be to use the version as specified by {@link TerraformExtension#TERRAFORM_DEFAULT},
 * otherwise it will be in search mode.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.1
 */
@CompileStatic
class TerraformExtension {
    public static final String NAME = 'terraform'
    public static final String TERRAFORM_DEFAULT = '1.8.0'

    @SuppressWarnings('NonFinalPublicField')
    @SuppressWarnings('PublicInstanceField')
    public ResolvableExecutable resolvableExecutable

    /** Constructs a new extension which is attached to the provided project.
     *
     * @param project Project this extension is associated with.
     */
    TerraformExtension(Project project) {
        this.project = project
        this.env = [:]
        this.projectOperations = ProjectOperations.maybeCreateExtension(project)
        this.registry = new ResolverFactoryRegistry(project)
        if (!Downloader.downloadSupported) {
            throw new TerraformExecutionException(
                "Terraform distribution not supported on ${projectOperations.stringTools.stringize(Downloader.OS)}"
            )
        }
        addVersionResolver(projectOperations)
        executable([version: TERRAFORM_DEFAULT])
    }

    /** Standard set of platforms.
     *
     * @return The set of provider platforms supported at the time the plugin was released.
     *
     * @since 0.14.0
     */
    static Set<String> getAllPlatforms() {
        PLATFORMS.asImmutable()
    }

    ExternalExecutable getResolver() {
        this.registry
    }

    void executable(Map<String, ?> opts) {
        this.resolvableExecutable = this.registry.getResolvableExecutable(opts)
    }

    /** Environment for running the exe
     *
     * <p> Calling this will resolve all lazy-values in the variable map.
     *
     * @return Map of environmental variables that will be passed.
     */
    Map<String, String> getEnvironment() {
        this.env
    }

    /** Add environmental variables to be passed to the exe.
     *
     * @param args Environmental variable key-value map.
     */
    void environment(Map<String, String> args) {
        this.env.putAll(args)
    }

    /** Adds AWS environmental variables to Terraform runtime environment.
     */
    void useAwsEnvironment() {
        environment(awsEnvironment())
    }

    /** Adds Google environmental variables to Terraform runtime environment.
     */
    void useGoogleEnvironment() {
        environment(googleEnvironment())
    }

    @CompileDynamic
    private void addVersionResolver(ProjectOperations projectOperations) {
        def tofu = project.rootProject.properties.getOrDefault('opentofu', false)
        DownloaderFactory downloaderFactory = {
            Map<String, Object> options, String version, ProjectOperations p ->
                if (tofu) {
                    new DownloaderOpenTofu(version, p)
                } else {
                    new Downloader(version, p)
                }
        }

        DownloadedExecutable resolver = { DownloaderBinary installer -> installer.terraformExecutablePath() }

        this.registry.registerExecutableKeyActions(
            new ResolveExecutableByVersion(projectOperations, downloaderFactory, resolver)
        )
    }

    private static final Set<String> PLATFORMS = [
        'darwin_amd64', 'darwin_arm64',
        'windows_amd64', 'windows_386',
        'linux_386', 'linux_amd64', 'linux_arm', 'linux_arm64',
        'freebsd_386', 'freebsd_amd64', 'freebsd_arm'
    ].toSet()

    private final Map<String, String> env
    private final ResolverFactoryRegistry registry
    private final ProjectOperations projectOperations
    private final Project project
}

