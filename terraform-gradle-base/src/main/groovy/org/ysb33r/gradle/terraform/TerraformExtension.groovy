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
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.process.ExecSpec
import org.ysb33r.gradle.terraform.config.VariablesSpec
import org.ysb33r.gradle.terraform.config.multilevel.TerraformExtensionConfigTypes
import org.ysb33r.gradle.terraform.config.multilevel.TerraformExtensionEmbeddable
import org.ysb33r.gradle.terraform.config.multilevel.TerraformSourceSetEmbeddable
import org.ysb33r.gradle.terraform.config.multilevel.Variables
import org.ysb33r.gradle.terraform.config.multilevel.VarsFilesPair
import org.ysb33r.gradle.terraform.config.multilevel.ignore.IgnoreGlobal
import org.ysb33r.gradle.terraform.config.multilevel.ignore.IgnoreSourceSet
import org.ysb33r.gradle.terraform.credentials.SessionCredentials
import org.ysb33r.gradle.terraform.errors.TerraformConfigurationException
import org.ysb33r.gradle.terraform.internal.CredentialsCache
import org.ysb33r.gradle.terraform.internal.Downloader
import org.ysb33r.gradle.terraform.internal.TerraformUtils
import org.ysb33r.gradle.terraform.tasks.AbstractTerraformBaseTask
import org.ysb33r.gradle.terraform.tasks.AbstractTerraformTask
import org.ysb33r.grolifant.api.core.ProjectOperations
import org.ysb33r.grolifant.api.v4.exec.AbstractToolExtension
import org.ysb33r.grolifant.api.v4.exec.DownloadedExecutable
import org.ysb33r.grolifant.api.v4.exec.DownloaderFactory
import org.ysb33r.grolifant.api.v4.exec.ResolveExecutableByVersion

import static org.ysb33r.gradle.terraform.config.multilevel.TerraformExtensionConfigTypes.VARIABLES
import static org.ysb33r.gradle.terraform.internal.TerraformUtils.awsEnvironment
import static org.ysb33r.gradle.terraform.tasks.AbstractTerraformBaseTask.defaultEnvironment

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
    public static final String TERRAFORM_DEFAULT = '1.8.0'

    /** Constructs a new extension which is attached to the provided project.
     *
     * @param project Project this extension is associated with.
     */
    TerraformExtension(Project project) {
        super(project)
        executableDetails = [:]
        if (Downloader.downloadSupported) {
            addVersionResolver(projectOperations)
            executable([version: TERRAFORM_DEFAULT])
        } else {
            executable searchPath()
        }
        this.warnOnNewVersion = false
        this.env = [:]
        this.credentialsCache = new CredentialsCache(projectOperations)
        this.fsMirror = project.objects.property(File)
        this.netMirror = project.objects.property(String)
        addVariablesExtension()
    }

    /** Constructs a new extension which is attached to the provided task.
     *
     * @param project Project this extension is associated with.
     * @param configExtensions Configuration extensions that have to be added. Has to implement
     * {@link TerraformExtensionEmbeddable} interface.
     */
    TerraformExtension(AbstractTerraformBaseTask task, List<TerraformExtensionConfigTypes> configExtensions) {
        super(task, NAME)
        executableDetails = [:]
        this.fsMirror = task.project.objects.property(File)
        this.netMirror = task.project.objects.property(String)
        projectOperations.fsOperations.updateFileProperty(
            this.fsMirror,
            ((TerraformExtension) projectExtension).localMirrorDirectory
        )
        projectOperations.stringTools.updateStringProperty(
            this.netMirror,
            ((TerraformExtension) projectExtension).netMirror
        )
        if (task instanceof AbstractTerraformTask) {
            configExtensions.each { TerraformExtensionConfigTypes config ->
                switch (config.type) {
                    case Variables:
                        addVariablesExtension((AbstractTerraformTask) task)
                        break
                    default:
                        throw new TerraformConfigurationException(
                            "${config.type.canonicalName} is not a supported extension for ${this.class.canonicalName}"
                        )
                }
            }
        }
    }

    /** Standard set of platforms.
     *
     * @return The set of provider platforms supported at the time the plugin was released.
     *
     * @since 0.14.0
     */
    Set<String> getAllPlatforms() {
        PLATFORMS.asImmutable()
    }

    @Override
    void executable(Map<String, ?> opts) {
        super.executable(opts)
        executableDetails.clear()
        executableDetails.putAll(opts)
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
     * If this is a project extension only the global variable definitions are returned.
     *
     * If this is a task extension and globals are not ignored, then return a combination of the global variables,
     * the source set variables and the local task extension variables.
     *
     * If this is a task extension and globals ignored, then return a combination of source set variables and the
     * local task extension variables.
     *
     * Task extension variables overrides source set variables which in turn overrides global variables.
     *
     * Files containing variables are simply added to a list in order of global, source set, local.
     *
     * @return Terraform variables and files containing variables.
     */
    Variables getAllVariables() {
        if (task) {
            AbstractTerraformTask terraformTask = (AbstractTerraformTask) task
            Variables variablesOnTask = (Variables) extContainer.getByType(VariablesSpec) // task.terraform.variables
            VarsFilesPair varsFilesPair = new VarsFilesPair()

            if (!secondLevelExtension(variablesOnTask, IgnoreGlobal).ignore) {
                globalExtension.allVariables.allVars.copyTo(varsFilesPair)
            }

            if (!secondLevelExtension(variablesOnTask, IgnoreSourceSet).ignore) {
                ((Variables) (terraformTask.sourceSet.variables)).allVars.copyTo(varsFilesPair)
            }

            variablesOnTask.allVars.copyTo(varsFilesPair)

            new Variables(varsFilesPair, terraformTask.sourceDir)
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
        task ? ((AbstractTerraformTask) task).environment : projectOperations.stringTools.stringizeValues(this.env)
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

    /**
     * Add one or more platforms to support for providers.
     *
     * Use {@link #getAllPlatforms} to add all platforms supported bu this plugin.
     *
     * @param reqPlatforms Platforms to add.
     *
     * @since 0.14.0
     */
    void platforms(Iterable<String> reqPlatforms) {
        this.requiredPlatforms.addAll(reqPlatforms)
    }

    /**
     * Add one or more platforms to support for providers.
     *
     * Use {@link #getAllPlatforms} to add all platforms supported bu this plugin.
     *
     * @param reqPlatforms Platforms to add.
     *
     * @since 0.14.0
     */
    void platforms(String... reqPlatforms) {
        this.requiredPlatforms.addAll(reqPlatforms as List)
    }

    /**
     * Provide the list of platforms that need to be supported.
     *
     * If empty, only the current platform will be supported.
     *
     * @return List of supported platforms.
     *
     * @since 0.14.0
     */
    Set<String> getPlatforms() {
        if (task) {
            if (this.requiredPlatforms.empty) {
                ((TerraformExtension) projectExtension).platforms
            } else {
                this.requiredPlatforms
            }
        } else {
            this.requiredPlatforms
        }
    }

    /**
     * Set an alternative local directory to look for provider packages rather than in upstream registries.
     *
     * @param path Local path.
     *
     * @since 0.14.0
     */
    void setLocalMirrorDirectory(Object path) {
        projectOperations.fsOperations.updateFileProperty(this.fsMirror, path)
    }

    /**
     * An alternative local directory to look for provider packages rather than in upstream registries.
     *
     * @return Provider to a location. Can be empty.
     *
     * @since 0.14.0
     */
    Provider<File> getLocalMirrorDirectory() {
        this.fsMirror
    }

    /**
     * Set an alternative network mirror service for provide packages.
     *
     * @param uri Mirror location.
     *
     * @since 0.14.0
     */
    void setNetMirror(Object uri) {
        projectOperations.stringTools.updateStringProperty(this.netMirror, uri)
    }

    /**
     * An alternative network mirror service for provider packages.
     *
     * @return Provider to a URL. Can be empty.
     *
     * @since 0.14.0
     */
    Provider<String> getNetMirror() {
        this.netMirror
    }

    /** Adds AWS environmental variables to Terraform runtime environment.
     *
     * @since 0.6.0
     *
     * @deprecated Use the {@code org.ysb33r.terraform.aws} plugin instead and fine control AWS authentication in the
     *   source sets.
     */
    @Deprecated
    void useAwsEnvironment() {
        environment awsEnvironment()
    }

    /** Converts a file path to a format suitable for interpretation by Terraform on the appropriate
     * platform.
     *
     * @param file Object that can be converted using {@code project.file}.
     * @return String version adapted on a per-platform basis
     */
    String terraformPath(Object file) {
        TerraformUtils.terraformPath(projectOperations, file)
    }

    /**
     * If the version is set via a version string return that, otherwise run terraform and parse the output
     *
     * @return The Terraform primary version or {@link TerraformMajorVersion#UNKNOWN} if version could be matched to
     *   something known to this system
     */
    TerraformMajorVersion resolveTerraformVersion() {
        String ver
        if (executableDetails[VERSION_KEY]) {
            ver = projectOperations.stringTools.stringize(executableDetails[VERSION_KEY])
        } else {
            TerraformExecSpec tes = new TerraformExecSpec(projectOperations, resolver)
            def strm = new ByteArrayOutputStream()
            tes.standardOutput(strm)
            tes.executable(resolvableExecutable)
            tes.command(VERSION_CMD_ARGS)
            tes.environment(defaultEnvironment)
            Action<ExecSpec> runner = new Action<ExecSpec>() {
                @Override
                void execute(ExecSpec spec) {
                    tes.copyToExecSpec(spec)
                }
            }

            projectOperations.exec(runner).assertNormalExitValue()
            ver = strm.toString().readLines()[0].replaceFirst('Terraform v', '')
        }

        TerraformMajorVersion.version(ver)
    }

    /**
     * Returns a map of credentials for for a source set.
     *
     * This can be called multiple times during task execution as credentials may time out between tasks.
     *
     * @param sourceSetName Name of source set.
     * @param workspaceName Name of workspace
     * @param creds Session credential providers.
     * @return Credentials for the specific sourceset-workspace combination
     *
     * @since 0.11
     */
    Map<String, String> credentialsCacheFor(
        String sourceSetName,
        String workspaceName,
        Provider<Set<SessionCredentials>> creds
    ) {
        task ? globalExtension.credentialsCacheFor(sourceSetName, workspaceName, creds) :
            this.credentialsCache.get(sourceSetName, workspaceName, creds)
    }

    private TerraformExtension getGlobalExtension() {
        (TerraformExtension) projectExtension
    }

    @Deprecated
    private void addVersionResolver(ProjectOperations projectOperations) {
        DownloaderFactory downloaderFactory = {
            Map<String, Object> options, String version, ProjectOperations p ->
                new Downloader(version, p)
        }

        DownloadedExecutable resolver = { Downloader installer ->
            installer.terraformExecutablePath
        }

        resolverFactoryRegistry.registerExecutableKeyActions(
            new ResolveExecutableByVersion(projectOperations, downloaderFactory, resolver)
        )
    }

    private void addVariablesExtension(AbstractTerraformTask task = null) {
        if (task) {
            addEmbeddableConfiguration(task, VARIABLES, task.sourceDir)
        } else {
            addEmbeddableConfiguration(null, VARIABLES, projectOperations.provider { -> null })
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
    private static final String VERSION_KEY = 'version'
    private static final String VERSION_CMD_ARGS = VERSION_KEY
    private static final Set<String> PLATFORMS = [
        'darwin_amd64', 'darwin_arm64',
        'windows_amd64', 'windows_386',
        'linux_386', 'linux_amd64', 'linux_arm', 'linux_arm64',
        'freebsd_386', 'freebsd_amd64', 'freebsd_arm'
    ].toSet()

    private Boolean warnOnNewVersion
    private final Property<File> fsMirror
    private final Property<String> netMirror
    private final Map<String, Object> env
    private final Map<String, Object> executableDetails
    private final CredentialsCache credentialsCache
    private final Set<String> requiredPlatforms = []
}

