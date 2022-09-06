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
package org.ysb33r.gradle.terraform.credentials;

import org.gradle.api.provider.Provider;

/**
 * A system that can provide session credentials of a per-workspace basis.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.11
 */
public interface SessionCredentialsProvider {
    /**
     * Provide the credentials for the default workspace.
     *
     * @return Credentials provider.
     */
    default Provider<SessionCredentials> getCredentialsEnvForWorkspace() {
        return getCredentialsEnvForWorkspace("default");
    }

    /**
     * Provide the credentials for a specific workspace.
     *
     * @param name Workspace name
     * @return Credentials provider.
     */
    Provider<SessionCredentials> getCredentialsEnvForWorkspace(String name);

    /**
     * Whether any credentials has been configured for this provider.
     *
     * @return {@code true} if credentials have been configured.
     */
    boolean hasCredentials();
}
