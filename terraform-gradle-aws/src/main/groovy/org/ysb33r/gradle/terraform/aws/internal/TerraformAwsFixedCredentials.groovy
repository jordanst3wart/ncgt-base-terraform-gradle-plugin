/*
 * Copyright 2017-2021 the original author or authors.
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
package org.ysb33r.gradle.terraform.aws.internal

import groovy.transform.CompileStatic
import org.ysb33r.gradle.terraform.aws.TerraformAwsSessionCredentials

/**
 * Fixed credentials that are supplied to Terraform.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.11*
 */
@CompileStatic
class TerraformAwsFixedCredentials implements TerraformAwsSessionCredentials {
    TerraformAwsFixedCredentials(final Map<String, String> env) {
        this.env = env
    }

    /**
     * Get the AWS credentials environment to pass to Terraform.
     *
     * @return Map of resolved {@code AWS_*} variables.
     */
    @Override
    Map<String, String> getEnvironment() {
        this.env
    }

    /**
     * Whether the credentials expired.
     * @return {@code true} if a refresh is required.
     */
    @Override
    boolean isExpired() {
        false
    }

    /**
     * Create a new set of credentials.
     *
     * @return New credentials.
     */
    @Override
    TerraformAwsSessionCredentials refresh() {
        this
    }

    private final Map<String, String> env
}
