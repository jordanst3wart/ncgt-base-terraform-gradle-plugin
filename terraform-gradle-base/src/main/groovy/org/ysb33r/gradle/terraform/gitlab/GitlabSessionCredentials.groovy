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
package org.ysb33r.gradle.terraform.gitlab

import groovy.transform.CompileStatic
import org.gradle.api.provider.Provider
import org.ysb33r.gradle.terraform.credentials.SessionCredentials

/**
 * Maintains sessions credentials for Gitlab
 *
 * @author Schalk W. Cronjé
 *
 * @since 1.1
 */
@CompileStatic
class GitlabSessionCredentials implements SessionCredentials {

    public static final String GITLAB_TOKEN = 'GITLAB_TOKEN'

    GitlabSessionCredentials(Provider<String> token) {
        this.token = token
    }

    /**
     * Get the AWS credentials environment to pass to Terraform.
     *
     * @return Map of resolved environment variables that could be understood by a Terraform provider.
     *   Can be empty, but never {@code null}
     */
    @Override
    Map<String, String> getEnvironment() {
        [(GITLAB_TOKEN) : token.get()]
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
     * @return New credentials. Never {@code null}.
     */
    @Override
    SessionCredentials refresh() {
        this
    }

    private final Provider<String> token
}
