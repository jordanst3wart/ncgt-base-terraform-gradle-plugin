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
package org.ysb33r.gradle.terraform.internal.remotestate

import groovy.transform.CompileStatic
import org.ysb33r.gradle.terraform.internal.TerraformUtils
import org.ysb33r.gradle.terraform.remotestate.BackendAttributesSpec
import org.ysb33r.gradle.terraform.remotestate.BackendTextTemplate
import org.ysb33r.grashicorp.StringUtils

/**
 * A collection of text templates.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.12
 */
@CompileStatic
class TextTemplates {

    static class LegacyS3ReplaceTokens extends ReplaceTokens {
        private LegacyS3ReplaceTokens() {
            super('''bucket = "@@bucket@@"
key    = "@@key@@"
region = "@@region@@"
''')
        }

        public static final LegacyS3ReplaceTokens INSTANCE = new LegacyS3ReplaceTokens()
    }
    /**
     * A template to which ANT's {@code ReplaceTokens} can be applied to
     */
    static class ReplaceTokens implements BackendTextTemplate {

        /**
         * Provide the template.
         *
         * @param theTemplate The template
         */
        ReplaceTokens(final Object theTemplate) {
            this.theTemplate = theTemplate
        }

        @Override
        String template(BackendAttributesSpec backendAttributes) {
            StringUtils.stringize(theTemplate)
        }

        private final Object theTemplate
    }

    static class AllTokens implements BackendTextTemplate {

        public static final AllTokens INSTANCE = new AllTokens()

        /**
         * Returns a template based upon backend attributes.
         *
         * @param backendAttributes Attributes can be used to generate the template.
         *
         * @return Text template
         */
        @Override
        String template(BackendAttributesSpec backendAttributes) {
            def entries = TerraformUtils.escapeHclVars(backendAttributes.tokens, true).entrySet()
            Integer maxLength = entries*.key*.toString()*.size().max()
            StringWriter writer = new StringWriter()
            for (Map.Entry<String, String> entry : entries) {
                writer.println("${entry.key.padRight(maxLength)} = ${entry.value}")
            }
            writer.toString()
        }
    }
}
