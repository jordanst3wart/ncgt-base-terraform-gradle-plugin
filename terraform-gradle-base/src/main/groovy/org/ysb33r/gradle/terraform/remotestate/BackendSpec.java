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
package org.ysb33r.gradle.terraform.remotestate;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.util.Map;

/**
 * Describes the configuration for a backend.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.12
 */
public interface BackendSpec extends Named, BackendAttributesSpec {

    /**
     * Clear all tokens.
     */
    void clear();

    /**
     * Configures the backend according to a specification
     *
     * @param configurator Configurating action
     */
    void configure(Action<? extends BackendSpec> configurator);

    /**
     * Configures the backend according to a specification
     *
     * @param configurator Configurating closure
     */
    void configure(Closure configurator);

    /**
     * Applies a custom template file for backend configuration.
     *
     * @param file Location of template file
     */
    void setTemplateFile(Object file);

    /**
     * Use a string as a template.
     *
     * @param text Template that can be processed by Ant's {@code ReplaceTokens}.
     */
    void setTextTemplate(Object text);

    /**
     * The template is simply a dump of all tokens that were set.
     */
    void allTokenTemplate();

    /**
     * Sets new delimiters for tokens.
     * <p>
     * Only useful when a custom template is used.
     *
     * @param begin Start delimiter for tokens
     * @param end   End delimiter for tokens
     */
    void delimiterTokenPair(String begin, String end);

    /**
     * Replace all tokens with a new set.
     * <p>
     * Only useful when a custom template is used.
     *
     * @param newTokens New replacement set
     */
    void setTokens(Map<String, Object> newTokens);

    /**
     * Adds more tokens.
     * <p>
     * Only useful when a custom template is used.
     *
     * @param moreTokens Additional tokens for replacement.
     */
    void tokens(Map<String, Object> moreTokens);

    /**
     * Set a single token value.
     *
     * @param key   Token name
     * @param value Lazy-evaluted value. Anything that can resolve to a string.
     */
    void token(String key, Object value);

    /**
     * Adds a provider of tokens.
     * <p>
     * These providers are processed before any of the customisations on the class.
     *
     * @param tokenProvider Addition provider of tokens
     * @since 1.0
     */
    void addTokenProvider(Provider<Map<String, Object>> tokenProvider);
}
