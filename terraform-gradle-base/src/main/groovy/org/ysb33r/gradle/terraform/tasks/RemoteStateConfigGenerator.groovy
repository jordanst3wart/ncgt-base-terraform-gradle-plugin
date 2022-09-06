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
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.ysb33r.gradle.terraform.internal.remotestate.Templates
import org.ysb33r.gradle.terraform.internal.remotestate.TextTemplates
import org.ysb33r.gradle.terraform.remotestate.BackendSpec
import org.ysb33r.gradle.terraform.remotestate.BackendTextTemplate
import org.ysb33r.gradle.terraform.remotestate.TerraformRemoteStateExtension
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
class RemoteStateConfigGenerator extends DefaultTask {

    RemoteStateConfigGenerator() {
        group = TERRAFORM_TASK_GROUP
        description = 'Generates configuration for backend state provider'

        this.destDir = project.objects.property(File)
        this.outputFile = project.objects.property(File)
        this.projectOperations = ProjectOperations.find(project)

        this.templateFileProvider = project.objects.property(File)
        this.textTemplateProvider = project.objects.property(BackendTextTemplate)
        this.backendProvider = project.objects.property(BackendSpec)

        this.outputFile.set(
            project.provider(new Callable<File>() {
                @Override
                File call() throws Exception {
                    backendFileRequired ? new File(destDir.get(), 'terraform-backend-config.tf') : null
                }
            })
        )

        onlyIf {
            backendFileRequired
        }

        inputs.property('textTemplate') { textTemplate.getOrNull()?.template(remoteState) }.optional(true)
    }

    @Deprecated
    void configure(Action<? extends BackendSpec> configurator) {
        if (backendProvider.present) {
            backendProvider.get().configure(configurator)
        } else {
            logger.warn "Configurator ignored for task '${name}' as no backend associated with task."
        }
    }

    /**
     * The default text template that will be used if nothing else is configured.
     *
     * @return Default template or empty string if not backend is associated with the task.
     */
    @Internal
    String getDefaultTextTemplate() {
        remoteState.defaultTextTemplate ?: ''
    }

    /**
     * Set remote state.
     *
     * This removes any previous settings of {@link #setTextTemplate} and {@link #setTemplateFile}.
     *
     * @param source Remote state extension associated with this configuration generator.
     *
     */
    void setRemoteState(TerraformRemoteStateExtension source) {
        this.remoteState = source
        this.backendProvider.set(source.backendProvider)
        projectOperations.updateFileProperty(
            this.templateFileProvider,
            source.templateFile
        )

        this.textTemplateProvider.set(source.textTemplate)
    }

    /**
     * Returns whether a backend partial configuration file should be generated.
     *
     * @return {@code true} is one is required.
     */
    @Internal
    boolean getBackendFileRequired() {
        templateFileProvider.getOrNull() || textTemplateProvider.getOrNull()?.template(remoteState)?.size()
    }

    /** Override the output directory.
     *
     * @param dir Anything convertible to a file path.
     */
    void setDestinationDir(Object dir) {
        this.destDir.set(project.provider({ ->
            project.file(dir)
        } as Callable<File>))
    }

    /** The output directory for the configuration file
     *
     * @return
     */
    @Internal
    Provider<File> getDestinationDir() {
        this.destDir
    }

    /** The location of the backend configuration file.
     *
     * @return Configuration file.
     */
    @OutputFile
    Provider<File> getBackendConfigFile() {
        this.outputFile
    }

    /** Applies a custom template file for backend configuration.
     *
     * The default is to use the template file from the remote extension on the source set.
     *
     * @param file Location of template file
     */
    void setTemplateFile(Object file) {
        projectOperations.updateFileProperty(this.templateFileProvider, file)
        this.textTemplateProvider.set((BackendTextTemplate) null)
    }

    /**
     * Use a string as a template.
     *
     * The default is to use the template text from the remote extension on the source set.
     *
     * @param text Template that can be processed by Ant's {@code ReplaceTokens}.
     *
     * @since 0.11
     */
    void setTextTemplate(Object text) {
        this.textTemplateProvider.set(new TextTemplates.ReplaceTokens(text))
        this.templateFileProvider.set((File) null)
    }

    /** Returns location of template file.
     *
     * @return Location of template file if set.
     */
    @Optional
    @InputFile
    Provider<File> getTemplateFile() {
        this.templateFileProvider
    }

    /** Returns text template.
     *
     * @return text of template if set.
     */
    @Internal
    Provider<BackendTextTemplate> getTextTemplate() {
        this.textTemplateProvider
    }

    /** Starting delimiter for tokens.
     *
     * Only useful when a custom template is used.
     *
     * @return Delimiter
     *
     * @since 0.12
     */
    @Input
    Provider<String> getBeginTokenProvider() {
        remoteState.beginTokenProvider
    }

    /** Terminating delimiter for tokens
     *
     * Only useful when a custom template is used.
     *
     * @return Delimiter
     *
     * @since 0.12
     */
    @Input
    Provider<String> getEndTokenProvider() {
        remoteState.endTokenProvider
    }

    /** Returns the current set of tokens
     *
     * @return Tokens used for replacements.
     */
    @Input
    Map<String, Object> getTokens() {
        Map<String, Object> compiledTokens = [:]
        for (Map<String, Object> map : tokenProviders*.get()) {
            compiledTokens.putAll(map)
        }

        compiledTokens.putAll(remoteState.tokenProvider.getOrElse([:]))
        compiledTokens
    }

    /**
     * Adds a provider of tokens.
     *
     * These providers are processed before any of the customisations on the backend provider.
     *
     * @param tokenProvider Addition provider of tokens
     *
     * @since 1.0
     */
    void addTokenProvider(Provider<Map<String, Object>> tokenProvider) {
        this.tokenProviders.add(tokenProvider)
    }

    /** Starting delimiter for tokens.
     *
     * Only useful when a custom template is used.
     *
     * @return Delimiter
     *
     * @deprecated
     */
    @Deprecated
    @Internal
    String getBeginToken() {
        beginTokenProvider.get()
    }

    /** Terminating delimiter for tokens
     *
     * Only useful when a custom template is used.
     *
     * @return Delimiter
     *
     * @deprecated
     */
    @Deprecated
    @Internal
    String getEndToken() {
        endTokenProvider.get()
    }

    @Internal
    @Deprecated
    Provider<BackendSpec> getBackendProvider() {
        this.backendProvider
    }

    @TaskAction
    void exec() {
        Templates.generateFromTemplate(
            name,
            projectOperations,
            remoteState,
            templateFileProvider,
            textTemplateProvider,
            backendConfigFile,
            beginTokenProvider.get(),
            endTokenProvider.get(),
            tokens
        )
    }

    private TerraformRemoteStateExtension remoteState
    private final ProjectOperations projectOperations
    private final Property<File> destDir
    private final Property<File> outputFile
    private final Property<File> templateFileProvider
    private final Property<BackendTextTemplate> textTemplateProvider
    private final List<Provider<Map<String, Object>>> tokenProviders = []

    @Deprecated
    private final Property<BackendSpec> backendProvider
}
