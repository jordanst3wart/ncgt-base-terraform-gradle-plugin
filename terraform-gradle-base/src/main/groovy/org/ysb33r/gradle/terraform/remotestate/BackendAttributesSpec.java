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

import org.gradle.api.provider.Provider;

import java.io.File;
import java.util.Map;

/**
 * Attributes that describe a backend.
 *
 * @author Schalk W. Cronjé
 *
 * @since 1.2
 */
public interface BackendAttributesSpec {
    /**
     * The default template used for the partial configuration of the backend.
     *
     * @return Default template as a string.
     */
    String getDefaultTextTemplate();

    /**
     * Returns location of template file.
     *
     * @return Location of template file if set.
     */
    Provider<File> getTemplateFile();

    /**
     * Returns text template.
     *
     * @return text of template if set.
     */
    Provider<BackendTextTemplate> getTextTemplate();

    /**
     * A provider for all the tokens that were set.
     *
     * @return Provider to a map of tokens.
     */
    Provider<Map<String, ?>> getTokenProvider();

    /**
     * Starting delimiter for tokens.
     * <p>
     * Only useful when a custom template is used.
     *
     * @return Provider for start delimiter
     */
    Provider<String> getBeginTokenProvider();

    /**
     * Terminating delimiter for tokens
     * <p>
     * Only useful when a custom template is used.
     *
     * @return Provider for end delimiter
     */
    Provider<String> getEndTokenProvider();

    /**
     * Returns the current set of tokens
     *
     * @return Tokens used for replacements.
     */
    Map<String, Object> getTokens();
}
