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
package org.ysb33r.gradle.terraform.remotestate

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet
import org.ysb33r.gradle.terraform.errors.TerraformUnknownBackendException
import org.ysb33r.grolifant.api.core.ProjectOperations

import java.util.concurrent.Callable

import static org.ysb33r.gradle.terraform.internal.TerraformUtils.stringizeOrNull
import static org.ysb33r.gradle.terraform.remotestate.AbstractBackendSpec.DEFAULT_TOKEN_DELIMITER

/** Extension that is added to the project {@link TerraformExtension} and to
 * instances of {@link org.ysb33r.gradle.terraform.TerraformSourceDirectorySet}
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.8.0
 */
@CompileStatic
@Slf4j
class TerraformRemoteStateExtension implements BackendAttributesSpec {

    public static final String NAME = 'remote'

    /** A utility to locate the extension.
     *
     * @param project Project context
     * @return {@link TerraformRemoteStateExtension} if it has been added.
     */
    static TerraformRemoteStateExtension findExtension(Project project) {
        ((ExtensionAware) project.extensions.getByType(TerraformExtension))
            .extensions.getByType(TerraformRemoteStateExtension)
    }

    /** A utility to find the global configuration for a specific backend
     *
     * @param project Project to interrogate
     * @param backend Backend to find
     * @return GLobal backend configuration
     *
     * @throw TerraformUnknownBackendException if backend not registered
     *
     * @since 0.12
     */
    static <T extends BackendSpec> T findBackend(Project project, Class<T> backend) {
        try {
            ((ExtensionAware) findExtension(project)).extensions.getByType(backend)
        } catch (UnknownDomainObjectException e) {
            throw new TerraformUnknownBackendException("${backend.canonicalName} is not a registered backend.")
        }
    }

    /**
     * Utility to find this extension on a terraform source set.
     *
     * @param project Project context
     * @param sourceSetName Name of source set.
     * @return Extension after it has been attached.
     *
     * @since 0.10.0
     */
    static TerraformRemoteStateExtension findExtension(Project project, String sourceSetName) {
        def sourceSet = project.extensions.getByType(NamedDomainObjectContainer<TerraformSourceDirectorySet>)
            .getByName(sourceSetName)
        ((ExtensionAware) sourceSet).extensions.getByType(TerraformRemoteStateExtension)
    }

    TerraformRemoteStateExtension(Project project) {
        final ExtensionContainer myExtensions = ((ExtensionAware) this).extensions

        this.projectOperations = ProjectOperations.find(project)
        this.prefix = project.objects.property(String)
        this.objectFactory = project.objects
        this.remoteStateVarProvider = project.objects.property(Boolean)
        this.remoteStateVarProvider.set(false)
        this.backendProperty = project.objects.property(BackendSpec)
        this.backendProperty.set(project.provider { -> myExtensions.getByType(LocalBackendSpec) })

        this.beginToken = project.provider(new Callable<String>() {
            @Override
            String call() throws Exception {
                BackendSpec current = backendProvider.get()
                BackendSpec following = getBackendOnFollowedExtension(current.name)
                if (following) {
                    current.beginTokenProvider.getOrElse(
                        following?.beginTokenProvider?.getOrElse(DEFAULT_TOKEN_DELIMITER) ?: DEFAULT_TOKEN_DELIMITER
                    )
                } else {
                    current.beginTokenProvider.getOrElse(DEFAULT_TOKEN_DELIMITER)
                }
            }
        })

        this.endToken = project.provider(new Callable<String>() {
            @Override
            String call() throws Exception {
                BackendSpec current = backendProvider.get()
                BackendSpec following = getBackendOnFollowedExtension(current.name)
                if (following) {
                    current.endTokenProvider.getOrElse(
                        following?.endTokenProvider?.getOrElse(DEFAULT_TOKEN_DELIMITER) ?: DEFAULT_TOKEN_DELIMITER
                    )
                } else {
                    current.endTokenProvider.getOrElse(DEFAULT_TOKEN_DELIMITER)
                }
            }
        })

        this.tokenProvider = project.provider(new Callable<Map<String, ?>>() {
            @Override
            Map<String, ?> call() throws Exception {
                BackendSpec current = backendProvider.get()
                BackendSpec following = getBackendOnFollowedExtension(current.name)
                if (following) {
                    Map<String, ?> map = [:]
                    map.putAll(following.tokenProvider.get())
                    map.putAll(current.tokenProvider.get())
                    map
                } else {
                    current.tokenProvider.get()
                }
            }
        })

        this.templateFileProvider = project.provider(new Callable<File>() {
            @Override
            File call() throws Exception {
                BackendSpec current = backendProvider.get()
                BackendSpec following = getBackendOnFollowedExtension(current.name)
                if (following) {
                    current.templateFile.getOrElse(
                        following?.templateFile?.getOrNull()
                    )
                } else {
                    current.templateFile.getOrNull()
                }
            }
        })

        this.textTemplateProvider = project.provider(new Callable<BackendTextTemplate>() {
            @Override
            BackendTextTemplate call() throws Exception {
                BackendSpec current = backendProvider.get()
                BackendSpec following = getBackendOnFollowedExtension(current.name)
                if (following) {
                    current.textTemplate.getOrElse(
                        following?.textTemplate?.getOrNull()
                    )
                } else {
                    current.textTemplate.getOrNull()
                }
            }
        })

        setPrefix(projectOperations.fullProjectPath.replaceAll(':', '-'))
    }

    /**
     * The default template used for the partial configuration of the backend.
     *
     * @return Default template as a string.
     */
    @Override
    String getDefaultTextTemplate() {
        backendProvider.getOrNull()?.defaultTextTemplate
    }

    /**
     * Returns location of template file.
     *
     * @return Location of template file if set.
     */
    @Override
    Provider<File> getTemplateFile() {
        this.templateFileProvider
    }

    /**
     * Returns text template.
     *
     * @return text of template if set.
     */
    @Override
    Provider<BackendTextTemplate> getTextTemplate() {
        this.textTemplateProvider
    }

    /**
     * Starting delimiter for tokens.
     * <p>
     * Only useful when a custom template is used.
     *
     * @return Provider for start delimiter
     *
     * @since 0.12
     */
    @Override
    Provider<String> getBeginTokenProvider() {
        this.beginToken
    }

    /**
     * Terminating delimiter for tokens
     * <p>
     * Only useful when a custom template is used.
     *
     * @return Provider for end delimiter
     *
     * @since 0.12
     */
    @Override
    Provider<String> getEndTokenProvider() {
        this.endToken
    }

    /**
     * Returns the current set of tokens
     *
     * @return Tokens used for replacements.
     */
    @Override
    Map<String, Object> getTokens() {
        tokenProvider.get()
    }

    /**
     * The name of the currently configured backend.
     *
     * @return Name of backend. Can be {@code null} is no backend is configured.
     *
     * @since 0.12
     */
    String getBackendName() {
        backendProperty.getOrNull()?.name
    }

    /** Assign the prefix.
     *
     * @param p Object that can be converted to a string. Can be a {@code Provider} as well.
     */
    void setPrefix(Object p) {
        projectOperations.stringTools.updateStringProperty(this.prefix, p)
    }

    /** A prefix that is added to remote state names.
     *
     */
    Provider<String> getPrefix() {
        this.prefix
    }

    /**
     * Follows the settings of another remote state extension.
     *
     * If the other remote has a backend provider configured, set this remote state to be the same.
     * This overrides any previous configured backend.
     *
     * If you want a different backend or fix the usage of a remote state variable map, you'll need to set it
     * after the {@code follow} call.
     *
     * Tokens will be read from the followed extension first and then overwritten by and local setting.
     *
     * @param other Instance of {@link TerraformRemoteStateExtension} to follow.
     */
    @SuppressWarnings('LineLength')
    void follow(TerraformRemoteStateExtension other) {
        setPrefix(other.prefix)
        this.followed = other
        this.remoteStateVarProvider.set(other.remoteStateVarProvider)
        try {
            backend = other.backendName
        } catch (TerraformUnknownBackendException e) {
            log.debug("${e.message}. It may be registered already, but not on this extension. Sticking with '${backendName}'")
        }
    }

    /**
     * Do not follow any other remote state.
     *
     * This will set the following
     * <ul>
     *   <li>{@link #setPrefix} to the snapshot of the previously followed value or an empty string.</li>
     *   <li>{@link #setRemoteStateVar} to the snapshot of the previously followed value or {@code false}.</li>
     * </ul>
     *
     * @since 0.12
     */
    void noFollow() {
        setPrefix(stringizeOrNull(this.prefix) ?: '')
        this.remoteStateVarProvider.set(this.remoteStateVarProvider?.getOrElse(false))
//        this.followedBackendProvider = null
        this.followed = null
    }

    /** Replaces the existing backend with another registered one.
     *
     * @param backendType Name of backend.
     * @throw TerraformUnknownBackendException if backend not registered.
     *
     * @since 0.12
     */
    void setBackend(String backendType) {
        backend(backendType) { it -> }
    }

    /** Replaces the existing backend with another registered one.
     *
     * @param backendType Type of backend.
     * @throw TerraformUnknownBackendException if backend not registered.
     *
     * @since 0.12
     */
    void setBackend(Class<? extends BackendSpec> backendType) {
        backend(backendType) { it -> }
    }

    /**
     * Replaces the existing backend with another registered one and configures it.
     *
     * @param backendType Name of backend
     * @param configurator Configurator
     * @throw TerraformUnknownBackendException if backend not registered.
     *
     * @since 0.12
     */
    void backend(String backendType, Action<? extends BackendSpec> configurator) {
        def useThisBackend = getBackendByName(backendType)
        useThisBackend.configure(configurator)
        backendProperty.set(useThisBackend)
    }

    /**
     * Replaces the existing backend with another registered one and configures it.
     *
     * @param backendType Type of backend
     * @param configurator Configurator
     * @throw TerraformUnknownBackendException if backend not registered.
     *
     * @since 0.12
     */
    void backend(Class<? extends BackendSpec> backendType, Action<? extends BackendSpec> configurator) {
        def useThisBackend = getBackendByType(backendType)
        useThisBackend.configure(configurator)
        backendProperty.set(useThisBackend)
    }

    /**
     * A provider to the configured backend.
     *
     * @return Backend provider. Can be empty if no backend has been configured.
     *
     * @since 0.12
     */
    Provider<BackendSpec> getBackendProvider() {
        this.backendProperty
    }

    /**
     * A provider of all of the tokens that were configured in the active backend.
     *
     * @return Provider of a map of lazy-evaluated tokens.
     *
     * @since 0.12
     */
    Provider<Map<String, ?>> getTokenProvider() {
        this.tokenProvider
    }

    /**
     * Whether tokens should be bundled in a map variable called {@code remote_state}.
     *
     * @param asVar
     *
     * @since 0.12
     */
    void setRemoteStateVar(boolean asVar) {
        this.remoteStateVarProvider.set(asVar)
    }

    /**
     * A provider to indicate whether remote state tokens should be added as a terraform map variable.
     *
     * @return Decision provider.
     *
     * @since 0.12
     */
    Provider<Boolean> getRemoteStateVarProvider() {
        this.remoteStateVarProvider
    }

    private BackendSpec getBackendByName(String name) {
        try {
            (BackendSpec) ((ExtensionAware) this).extensions.getByName(name)
        } catch (UnknownDomainObjectException e) {
            throw new TerraformUnknownBackendException("'${name}' is not registered as a backend.", e)
        }
    }

    private BackendSpec getBackendByType(Class<? extends BackendSpec> type) {
        try {
            ((ExtensionAware) this).extensions.getByType(type)
        } catch (UnknownDomainObjectException e) {
            throw new TerraformUnknownBackendException("'${type.canonicalName}' is not registered as a backend.", e)
        }
    }

    private BackendSpec getBackendOnFollowedExtension(String targetBackendName) {
        if (followed) {
            try {
                (BackendSpec) ((ExtensionAware) followed).extensions.getByName(targetBackendName)
            } catch (UnknownDomainObjectException e) {
                log.debug("'${targetBackendName}' was not found on followed ${this.class.name}. Ignoring.", e)
                null
            }
        } else {
            null
        }
    }

    private TerraformRemoteStateExtension followed
    private final Property<BackendSpec> backendProperty
    private final Provider<String> beginToken
    private final Provider<String> endToken
    private final ObjectFactory objectFactory
    private final ProjectOperations projectOperations
    private final Property<String> prefix
    private final Property<Boolean> remoteStateVarProvider
    private final Provider<Map<String, ?>> tokenProvider
    private final Provider<File> templateFileProvider
    private final Provider<BackendTextTemplate> textTemplateProvider
}
