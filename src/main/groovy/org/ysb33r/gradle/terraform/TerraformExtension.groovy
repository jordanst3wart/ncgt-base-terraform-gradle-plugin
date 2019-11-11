/*
 * Copyright 2017-2019 the original author or authors.
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
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer
import org.ysb33r.gradle.terraform.config.VariablesSpec
import org.ysb33r.gradle.terraform.config.multilevel.TerraformExtensionConfigTypes
import org.ysb33r.gradle.terraform.config.multilevel.TerraformExtensionEmbeddable
import org.ysb33r.gradle.terraform.config.multilevel.TerraformSourceSetEmbeddable
import org.ysb33r.gradle.terraform.config.multilevel.Variables
import org.ysb33r.gradle.terraform.config.multilevel.VarsFilesPair
import org.ysb33r.gradle.terraform.config.multilevel.ignore.IgnoreGlobal
import org.ysb33r.gradle.terraform.config.multilevel.ignore.IgnoreSourceSet
import org.ysb33r.gradle.terraform.errors.TerraformConfigurationException
import org.ysb33r.gradle.terraform.internal.Downloader
import org.ysb33r.gradle.terraform.internal.TerraformUtils
import org.ysb33r.gradle.terraform.tasks.AbstractTerraformTask
import org.ysb33r.grolifant.api.MapUtils
import org.ysb33r.grolifant.api.exec.AbstractToolExtension
import org.ysb33r.grolifant.api.exec.ResolveExecutableByVersion

import java.util.concurrent.Callable

import static org.ysb33r.gradle.terraform.config.multilevel.TerraformExtensionConfigTypes.VARIABLES

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
class TerraformExtension extends AbstractToolExtension {

    /** The standard extension name.
     *
     */
    public static final String NAME = 'terraform'

    /** The default version of Terraform that will be used on
     * a supported platform if nothing else is configured.
     */
    public static final String TERRAFORM_DEFAULT = '0.12.13'

//    /** Function to return all Terraform variables from an extension.
//     *
//     */
//    public static final Function<TerraformExtension, TerraformTaskConfigExtension> ALL_VARIABLES =
//        { TerraformExtension te -> te.allVariables } as Function<TerraformExtension, TerraformTaskConfigExtension>
//
    /** Constructs a new extension which is attached to the provided project.
     *
     * @param project Project this extension is associated with.
     */
    TerraformExtension(Project project) {
        super(project)
        if (Downloader.downloadSupported) {
            addVersionResolver(project)
            executable([version: TERRAFORM_DEFAULT])
        } else {
            executable searchPath()
        }
        this.warnOnNewVersion = false
        this.env = [:]
        addVariablesExtension()
    }

    /** Constructs a new extension which is attached to the provided task.
     *
     * @param project Project this extension is associated with.
     * @param configExtensions Configuration extensions that have to be added. Has to implement
     * {@link TerraformExtensionEmbeddable} interface.
     */
    TerraformExtension(AbstractTerraformTask task, List<TerraformExtensionConfigTypes> configExtensions) {
        super(task, NAME)
        configExtensions.each { TerraformExtensionConfigTypes config ->
            switch (config.type) {
                case Variables:
                    addVariablesExtension(task)
                    break
                default:
                    throw new TerraformConfigurationException(
                        "${config.type.canonicalName} is not a supported extension for ${this.class.canonicalName}"
                    )
            }
        }
    }

    /** Use this to configure a system path search for {@code Terraform}.
     *
     * @return Returns a special option to be used in {@link #executable}
     */
    static Map<String, Object> searchPath() {
        TerraformExtension.SEARCH_PATH
    }

    /** Print a warning message if a new version of {@code terraform} is available.
     *
     */
    boolean getWarnOnNewVersion() {
        (this.warnOnNewVersion == null && task != null) ? globalExtension.getWarnOnNewVersion() : this.warnOnNewVersion
    }

    /** Turn checkpoint warning on or off
     *
     * @param value {@code true} to warn on new {@code terraform} versions.
     */
    void setWarnOnNewVersion(boolean value) {
        this.warnOnNewVersion = value
    }

    /** Turn checkpoint warning on or off
     *
     * @param value {@code true} to warn on new {@code terraform} versions.
     */
    void warnOnNewVersion(boolean value) {
        this.warnOnNewVersion = value
    }

    /** Returns all terraform variables and descriptions of variables within files within the specific context
     *
     * If this is a project extension only return the global varibale definitions are returned.
     *
     * If this is a task extension and globals are not ignored, then return a combination of the global variables,
     * the source set variables and the local task extension variables.
     *
     * If this is a task extension and globals ignored, then return a combination of source set variables and the
     * local task extension variables.
     *
     * Task extension variables overrides source set variables which in turn overrides global variables.
     *
     * Files ocntaining variables are simply added to a list in order of global, source set, local.
     *
     * @return Terraform variables and files containing variables.
     */
    Variables getAllVariables() {
        if (task) {
            AbstractTerraformTask terraformTask = (AbstractTerraformTask) task
            Variables vars = (Variables) extContainer.getByType(VariablesSpec)
            VarsFilesPair varsFilesPair = new VarsFilesPair()

            if (!secondLevelExtension(vars, IgnoreGlobal).ignore) {
                VarsFilesPair global = globalExtension.allVariables.allVars
                varsFilesPair.files.addAll(global.files)
                varsFilesPair.vars.putAll(global.vars)
            }

            if (!secondLevelExtension(vars, IgnoreSourceSet).ignore) {
                VarsFilesPair ssvfp = ((Variables) (terraformTask.sourceSet.variables)).allVars
                varsFilesPair.files.addAll(ssvfp.files)
                varsFilesPair.vars.putAll(ssvfp.vars)
            }

            varsFilesPair.files.addAll(vars.allVars.files)
            varsFilesPair.vars.putAll(vars.allVars.vars)

            new Variables(
                varsFilesPair,
                project.provider({ -> terraformTask.sourceDir.get() } as Callable<File>)
            )
        } else {
            (Variables) extContainer.getByType(VariablesSpec)
        }
    }

    /** Replace current environment with new one.
     * If this is called on the task extension, no project extension environment will
     * be used.
     *
     * @param args New environment key-value map of properties.
     */
    void setEnvironment(Map<String, ?> args) {
        if (task) {
            ((AbstractTerraformTask) task).environment = args
        } else {
            this.env.clear()
            this.env.putAll((Map<String, Object>) args)
        }
    }

    /** Environment for running the exe
     *
     * <p> Calling this will resolve all lazy-values in the variable map.
     *
     * @return Map of environmental variables that will be passed.
     */
    Map<String, String> getEnvironment() {
        task ? ((AbstractTerraformTask) task).environment : MapUtils.stringizeValues(this.env)
    }

    /** Add environmental variables to be passed to the exe.
     *
     * @param args Environmental variable key-value map.
     */
    void environment(Map<String, ?> args) {
        if (task) {
            ((AbstractTerraformTask) task).environment(args)
        } else {
            this.env.putAll((Map<String, Object>) args)
        }
    }

    /** Converts a file path to a format suitable for interpretation by Terraform on the appropriate
     * platform.
     *
     * @param file Object that can be converted using {@code project.file}.
     * @return String version adapted on a per-platform basis
     */
    String terraformPath(Object file) {
        TerraformUtils.terraformPath(project, file)
    }

    private TerraformExtension getGlobalExtension() {
        (TerraformExtension) projectExtension
    }

    private void addVersionResolver(Project project) {
        ResolveExecutableByVersion.DownloaderFactory downloaderFactory = {
            Map<String, Object> options, String version, Project p ->
                new Downloader(version, p)
        } as ResolveExecutableByVersion.DownloaderFactory

        ResolveExecutableByVersion.DownloadedExecutable resolver = { Downloader installer ->
            installer.terraformExecutablePath
        } as ResolveExecutableByVersion.DownloadedExecutable

        resolverFactoryRegistry.registerExecutableKeyActions(
            new ResolveExecutableByVersion(project, downloaderFactory, resolver)
        )
    }

    private void addVariablesExtension(AbstractTerraformTask task = null) {
        if (task) {
            addEmbeddableConfiguration(task, VARIABLES, project.provider { -> task.sourceDir.get() })
        } else {
            addEmbeddableConfiguration(null, VARIABLES, project.provider { -> null })
        }
    }

    private <T> void addEmbeddableConfiguration(
        AbstractTerraformTask task,
        TerraformExtensionConfigTypes configType,
        Object... args
    ) {
        T embedded = extContainer.create(configType.publicType, configType.name, configType.type, args)

        if (task) {
            secondLevelExtContainer(embedded).create(IgnoreGlobal.NAME, IgnoreGlobal)

            if (embedded instanceof TerraformSourceSetEmbeddable) {
                secondLevelExtContainer(embedded).create(IgnoreSourceSet.NAME, IgnoreSourceSet)
            }
        }
    }

    /** Get the terraform extension's own extension container
     *
     * @return
     */
    private ExtensionContainer getExtContainer() {
        ((ExtensionAware) this).extensions
    }

    /** Get the extension container of an extension that has been added to the terraform extension.
     *
     * @param embeddable
     * @return Extension container
     */
    private ExtensionContainer secondLevelExtContainer(Object embedded) {
        ((ExtensionAware) embedded).extensions
    }

    /** Get the extension of an extension that has been added to the terraform extension.
     *
     * @param embeddedType Type that was embedded in the terraform extension
     * @param secondLevelEmbeddedType Type that was embedded in the {@code embeddedType} as an extension.
     * @return Instance of the extension.
     */
    private <T> T secondLevelExtension(TerraformExtensionEmbeddable embedded, Class<T> secondLevelEmbeddedType) {
        secondLevelExtContainer(embedded).getByType(secondLevelEmbeddedType)
    }

    @SuppressWarnings('UnnecessaryCast')
    private static final Map<String, Object> SEARCH_PATH = [search: NAME] as Map<String, Object>

    private Boolean warnOnNewVersion
    private final Map<String, Object> env

}

//    String getWorkspace() {
//        (this.workspace == null && task != null) ? globalExtension.getWorkspace() : this.workspace
//    }
//
//    void setWorkspace(final String ws) {
//        this.workspace = ws
//    }
//
//    void workspace(final String ws) {
//        this.workspace = ws
//    }
//
//    private String workspace

