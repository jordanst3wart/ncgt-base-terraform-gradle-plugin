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
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem
import org.ysb33r.gradle.terraform.config.Json
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.gradle.terraform.config.Parallel
import org.ysb33r.gradle.terraform.internal.DownloaderTerraform
import org.ysb33r.gradle.terraform.internal.DownloaderBinary
import org.ysb33r.gradle.terraform.internal.DownloaderOpenTofu
import org.ysb33r.grolifant.api.core.ProjectOperations
import org.ysb33r.grolifant.api.v4.exec.DownloadedExecutable
import org.ysb33r.grolifant.api.v4.exec.DownloaderFactory
import org.ysb33r.grolifant.api.v4.exec.ExternalExecutable
import org.ysb33r.grolifant.api.v4.exec.ResolvableExecutable
import org.ysb33r.grolifant.api.v4.exec.ResolveExecutableByVersion
import org.ysb33r.grolifant.api.v4.exec.ResolverFactoryRegistry

import static org.ysb33r.gradle.terraform.internal.Utils.awsEnvironment
import static org.ysb33r.gradle.terraform.internal.Utils.googleEnvironment

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
        if (!DownloaderTerraform.downloadSupported) {
            throw new GradleException(
                "Terraform distribution not supported on ${OperatingSystem.current().name}"
            )
        }
        addVersionResolver(projectOperations)
        executable([version: TERRAFORM_DEFAULT])
    }

    ExternalExecutable getResolver() {
        this.registry
    }

    void executable(Map<String, ?> opts) {
        this.resolvableExecutable = this.registry.getResolvableExecutable(opts)
    }

    /** Replace current environment with new one.
     * If this is called on the task extension, no project extension environment will
     * be used.
     *
     * @param args New environment key-value map of properties.
     */
    void setEnvironment(Map<String, ?> args) {
        this.env.clear() // TODO might not need to clear
        this.env.putAll((Map<String, Object>) args)
    }

    /** Environment for running the exe
     *
     * <p> Calling this will resolve all lazy-values in the variable map.
     *
     * @return Map of environmental variables that will be passed.
     */
    Map<String, String> getEnvironment() {
        projectOperations.stringTools.stringizeValues(this.env)
    }

    /** Add environmental variables to be passed to the exe.
     *
     * @param args Environmental variable key-value map.
     */
    void environment(Map<String, ?> args) {
        this.env.putAll((Map<String, Object>) args)
    }

    void useAwsEnvironment() {
        environment(awsEnvironment())
    }

    void useGoogleEnvironment() {
        environment(googleEnvironment())
    }

    void setLockTimeout(int timeout) {
        this.lock.timeout = timeout
        this.lock.enabled = true
    }

    Lock getLock() {
        this.lock
    }

    void setParallel(int parallel) {
        this.parallel.maxParallel = parallel
    }

    Parallel getParallel() {
        this.parallel
    }

    void setJson(boolean enabled) {
        this.json.enabled = enabled
    }

    Json getJson() {
        this.json
    }

    @CompileDynamic
    private void addVersionResolver(ProjectOperations projectOperations) {
        def tofu = project.rootProject.properties.getOrDefault('opentofu', false)
        DownloaderFactory downloaderFactory = {
            Map<String, Object> options, String version, ProjectOperations p ->
                if (tofu) {
                    new DownloaderOpenTofu(version, p)
                } else {
                    new DownloaderTerraform(version, p)
                }
        }

        DownloadedExecutable resolver = { DownloaderBinary installer -> installer.terraformExecutablePath() }

        this.registry.registerExecutableKeyActions(
            new ResolveExecutableByVersion(projectOperations, downloaderFactory, resolver)
        )
    }

    private final Map<String, Object> env
    private final ResolverFactoryRegistry registry
    private final ProjectOperations projectOperations
    private final Project project
    private final Lock lock = new Lock()
    private final Parallel parallel = new Parallel()
    private final Json json = new Json()
}

