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
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.ysb33r.gradle.terraform.internal.Transform
import org.ysb33r.gradle.terraform.internal.remotestate.TextTemplates
import org.ysb33r.grolifant.api.core.ProjectOperations

import java.util.concurrent.Callable

/**
 * Base class for simplify implementation of backend specifications.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.12
 */
@CompileStatic
@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class AbstractBackendSpec implements BackendSpec {

    public static final String DEFAULT_TOKEN_DELIMITER = '@@'

    /** Utility method to find a backend extension on a project.
     *
     * @param project Project context.
     * @param clazz The type of the extension.
     * @return Extension after it has been attached.
     *
     * @since 0.12
     */
    static <T extends BackendSpec> T findExtension(Project project, Class<T> clazz) {
        ((ExtensionAware) TerraformRemoteStateExtension.findExtension(project)).extensions.getByType(clazz)
    }

    /**
     * Utility to find a backend extension on a terraform source set.
     *
     * @param project Project context
     * @param sourceSetName Name of source set.
     * @param clazz The type of the extension.
     * @return Extension after it has been attached.
     *
     * @since 0.12
     */
    static <T extends BackendSpec> T findExtension(Project project, String sourceSetName, Class<T> clazz) {
        def remote = TerraformRemoteStateExtension.findExtension(project, sourceSetName)
        ((ExtensionAware) remote).extensions.getByType(clazz)
    }

    /**
     * Configures the backend according to a specification
     *
     * @param configurator Configurating action
     */
    @Override
    void configure(Action<? extends BackendSpec> configurator) {
        configurator.execute(this)
    }

    /**
     * Configures the backend according to a specification
     *
     * @param configurator Configurating closure
     */
    @Override
    void configure(Closure configurator) {
        Closure cfg = (Closure) configurator.clone()
        this.identity(cfg)
    }

    /**
     * A provider for all the tokens that were set.
     *
     * @return Provider to a map of tokens.
     */
    Provider<Map<String, ?>> getTokenProvider() {
        this.tokenProvider
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
        this.actualTextTemplate
    }

    /**
     * Applies a custom template file for backend configuration.
     *
     * @param file Location of template file
     */
    @Override
    void setTemplateFile(Object file) {
        this.actualTemplateFile = file
        this.useTemplateFile = true
    }

    /**
     * Use a string as a template.
     *
     * @param text Template that can be processed by Ant's {@code ReplaceTokens}.
     */
    @Override
    void setTextTemplate(Object text) {
        if (text instanceof BackendTextTemplate) {
            this.actualTextTemplate.set((BackendTextTemplate) text)
        } else {
            this.actualTextTemplate.set(new TextTemplates.ReplaceTokens(text))
        }

        this.useTemplateFile = false
    }

    /**
     * The template is simply a dump of all tokens that were set.
     */
    @Override
    void allTokenTemplate() {
        actualTextTemplate.set(TextTemplates.AllTokens.INSTANCE)
        this.useTemplateFile = false
    }

    /**
     * Sets new delimiters for tokens.
     * <p>
     * Only useful when a custom template is used.
     *
     * @param begin Start delimiter for tokens
     * @param end End delimiter for tokens
     */
    @Override
    void delimiterTokenPair(String begin, String end) {
        this.beginTokenProperty.set(begin)
        this.endTokenProperty.set(end)
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
        this.beginTokenProperty
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
        this.endTokenProperty
    }

    /**
     * Replace all tokens with a new set.
     * <p>
     * Only useful when a custom template is used.
     *
     * @param newTokens New replacement set
     */
    @Override
    void setTokens(Map<String, Object> newTokens) {
        this.localTokens.clear()
        this.localTokens.putAll(newTokens)
    }

    /**
     * Adds more tokens.
     * <p>
     * Only useful when a custom template is used.
     *
     * @param moreTokens Additional tokens for replacement.
     */
    @Override
    void tokens(Map<String, Object> moreTokens) {
        this.localTokens.putAll(moreTokens)
    }

    /**
     * Set a single token value.
     *
     * @param key Token name
     * @param value Lazy-evaluted value. Anything that can resolve to a string.
     */
    @Override
    void token(String key, Object value) {
        this.localTokens.put(key, value)
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
     * Adds a provider of tokens.
     * <p>
     * These providers are processed before any of the customisations on the class.
     *
     * @param tokenProvider Addition provider of tokens
     * @since 1.0
     */
    @Override
    void addTokenProvider(Provider<Map<String, Object>> tokenProvider) {
        this.additionalTokenProviders.add(tokenProvider)
    }

    /**
     * Reset all tokens as well as token delimiters.
     */
    void clear() {
        this.localTokens.clear()
        this.delimiterTokenPair(null, null)
    }

    protected AbstractBackendSpec(ProjectOperations po, ObjectFactory objects) {
        this.localTokens = [:]
        this.additionalTokenProviders = []
        this.projectOperations = po
        this.beginTokenProperty = objects.property(String)
        this.endTokenProperty = objects.property(String)

        this.templateFileProvider = po.provider(new Callable<File>() {
            @Override
            File call() throws Exception {
                if (useTemplateFile && actualTemplateFile) {
                    if (actualTemplateFile instanceof Provider && !((Provider) actualTemplateFile).present) {
                        null
                    } else {
                        po.file(actualTemplateFile)
                    }
                } else {
                    null
                }
            }
        })

        this.actualTextTemplate = objects.property(BackendTextTemplate)

        this.tokenProvider = po.provider(new Callable<Map<String, Object>>() {
            @Override
            Map<String, ?> call() {
                Map<String, ?> mergedMap = [:]
                Transform.toList(additionalTokenProviders) { Provider<Map<String, ?>> it ->
                    mergedMap.putAll(it.getOrElse([:]))
                }
                mergedMap.putAll(localTokens)
                mergedMap
            }
        })
    }

    protected ProjectOperations getProjectOperations() {
        this.projectOperations
    }

    /** Adds a token value that will be evaluated to a file path.
     *
     * @param key Token name
     * @param p Object to be evaluated as a file path.
     */
    protected void tokenPath(String key, Object p) {
        token(key, projectOperations.provider { -> projectOperations.fsOperations.file(p).absolutePath })
    }

    private Object actualTemplateFile
    private boolean useTemplateFile = false

    private final Property<BackendTextTemplate> actualTextTemplate
    private final Property<String> beginTokenProperty
    private final Property<String> endTokenProperty
    private final ProjectOperations projectOperations
    private final Map<String, ?> localTokens
    private final Provider<Map<String, ?>> tokenProvider
    private final Provider<File> templateFileProvider
    private final List<Provider<Map<String, ?>>> additionalTokenProviders
}
