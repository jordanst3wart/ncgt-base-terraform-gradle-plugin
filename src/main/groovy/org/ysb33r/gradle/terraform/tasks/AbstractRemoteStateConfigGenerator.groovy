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
package org.ysb33r.gradle.terraform.tasks

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.Transformer
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.ysb33r.gradle.terraform.internal.remotestate.Templates

import java.util.concurrent.Callable

/**
 * @author Schalk W. Cronjé
 *
 * @since 0.8.0
 */
@CompileStatic
abstract class AbstractRemoteStateConfigGenerator extends DefaultTask {

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
     * @return Configuration file
     */
    @OutputFile
    Provider<File> getBackendConfigFile() {
        this.outputFile
    }

    /** Applies a custom template file for backend configuration.
     *
     * @param file Location of template file
     */
    void setTemplateFile(Object file) {
        this.templateFile.set(project.provider({ ->
            project.file(file)
        } as Callable<File>))
    }

    /** Returns location of template file.
     *
     * @return Location of template file if set.
     */
    @Optional
    @InputFile
    Provider<File> getTemplateFile() {
        this.templateFile
    }

    /** Sets new delimiters for tokens.
     *
     * Only useful when a custom template is used.
     *
     * @param begin Start delimiter for tokens
     * @param end End delimiter for tokens
     */
    void delimiterTokenPair(String begin, String end) {
        this.start = begin
        this.end = end
    }

    /** Starting delimiter for tokens.
     *
     * Only useful when a custom template is used.
     *
     * @return Delimiter
     */
    @Input
    String getBeginToken() {
        this.start
    }

    /** Terminating delimiter for tokens
     *
     * Only useful when a custom template is used.
     *
     * @return Delimiter
     */
    @Input
    String getEndToken() {
        this.end
    }

    /** Replace all tokens with a new set.
     *
     * Only useful when a custom template is used.
     *
     * @param newTokens New replacement set
     */
    void setTokens(Map<String, Object> newTokens) {
        this.tokens.clear()
        this.tokens.putAll(newTokens)
    }

    /** Adds more tokens.
     *
     * Only useful when a custom template is used.
     *
     * @param moreTokens Additional tokens for replacement.
     */
    void tokens(Map<String, Object> moreTokens) {
        this.tokens.putAll(moreTokens)
    }

    /** Returns the current set of tokens
     *
     * @return Tokens used for replacements.
     */
    @Input
    Map<String, Object> getTokens() {
        this.tokens
    }

    @TaskAction
    void exec() {
        Templates.generateFromTemplate(
            name,
            project,
            templateResourcePath,
            templateFile,
            backendConfigFile,
            start,
            end,
            tokens
        )
    }

    protected AbstractRemoteStateConfigGenerator() {
        this.destDir = project.objects.property(File)
        this.outputFile = project.objects.property(File)
        this.templateFile = project.objects.property(File)

        this.outputFile.set(destDir.map(new Transformer<File, File>() {
            @Override
            File transform(File file) {
                new File(file, configFileName)
            }
        }))
    }

    /** Backend configuration file name
     *
     * @return Name of file.
     */
    @Internal
    abstract protected String getConfigFileName()

    /** Returns the name of the default template resource path.
     *
     * @return Resource path as meant for {@link java.lang.Class#getResourceAsStream}.
     */
    @Internal
    abstract protected String getTemplateResourcePath()

    private final Property<File> destDir
    private final Property<File> outputFile
    private String start = '@@'
    private String end = start
    private final Property<File> templateFile
    private final Map<String, Object> tokens = [:] as TreeMap<String, Object>

}
