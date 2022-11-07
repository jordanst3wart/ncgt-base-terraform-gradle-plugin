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
import org.gradle.api.Project
import org.gradle.api.Transformer
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.ysb33r.gradle.terraform.credentials.SessionCredentials
import org.ysb33r.gradle.terraform.credentials.SessionCredentialsProvider
import org.ysb33r.grolifant.api.core.ProjectOperations

/**
 * Provides Gitlab token authentication to Terraform.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.11
 */
@CompileStatic
class GitlabExtension implements SessionCredentialsProvider {

    public static final String NAME = 'gitlab'

    GitlabExtension(Project tempProjectReference) {
        this.projectOperations = ProjectOperations.find(tempProjectReference)
        this.defaultCreds = tempProjectReference.objects.property(String)
        this.transformer = new Transformer<SessionCredentials, String>() {
            @Override
            GitlabSessionCredentials transform(String s) {
                new GitlabSessionCredentials(credentialsMap.getOrDefault(s, defaultCreds))
            }
        }
    }

    /**
     * Removes any credential customisations and reset to a no-credential state.
     */
    void clearAllCredentials() {
        this.credentialsMap.clear()
        this.defaultCreds.set((String) null)
    }

    /**
     * Provide the credentials for a specific workspace.
     *
     * @param name Workspace name
     * @return Credentials provider.
     */
    @Override
    Provider<SessionCredentials> getCredentialsEnvForWorkspace(String name) {
        projectOperations.provider { -> name }.map(this.transformer)
    }

    /**
     * Whether any credentials has been configured for this provider.
     *
     * @return {@code true} if credentials have been configured.
     */
    @Override
    boolean hasCredentials() {
        !credentialsMap.empty || defaultCreds.present
    }

    /**
     * Sets the default to be to use {@code GITLAB_TOKEN} from the environment and pass as-is to {@code Terraform}.
     *
     */
    void useGitlabTokenFromEnvironment() {
        projectOperations.stringTools.updateStringProperty(
            this.defaultCreds,
            projectOperations.environmentVariable(GitlabSessionCredentials.GITLAB_TOKEN)
        )
    }

    /**
     * Sets the credentials for a specific workspace
     * to use Gitlab credentials from the environment and pass as-is to {@code Terraform}.
     *
     * This will replace any assumeRole configuration for the specific workspace.
     *
     * @param workspace Workspace to use.
     */
    void useGitlabTokenFromEnvironment(String workspace) {
        this.credentialsMap.put(
            workspace,
            projectOperations.environmentVariable(GitlabSessionCredentials.GITLAB_TOKEN)
        )
    }

    /**
     * Pass these property values to {@code Terraform} to use as authentication for a specific workspace.
     *
     * Use the values that are supplied by the following property names.
     * Properties are searched in order of Gradle properties, then system properties and finally
     *   environmental variables. For the latter the convention of converting dots to underscores and uppercasing
     *   the name is used.
     *
     * Calling this will remove any external influence available via {@link #useGitlabTokenFromEnvironment}.
     *
     * @param gitlabTokenPropertyName Property name for the Gitlab token.
     */
    void useProperty(
        String gitlabTokenPropertyName
    ) {
        useProperty(projectOperations.resolveProperty(gitlabTokenPropertyName))
    }

    /**
     * Pass these property values to {@code Terraform} to use as authentication for a specific workspace.
     *
     * Use the values that are supplied by the following property names.
     * Properties are searched in order of Gradle properties, then system properties and finally
     *   environmental variables. For the latter the convention of converting dots to underscores and uppercasing
     *   the name is used.
     *
     * Calling this will remove any external influence available via {@link #useGitlabTokenFromEnvironment}.
     *
     * @param workspace Workspace name
     * @param gitlabTokenPropertyName Property name for the Gitlab token.
     */
    void useProperty(
        String workspace,
        String gitlabTokenPropertyName
    ) {
        useProperty(workspace, projectOperations.resolveProperty(gitlabTokenPropertyName))
    }

    /**
     * Use a provider for the Gitlab token.
     *
     * Calling this will remove any external influence available via {@link #useGitlabTokenFromEnvironment}.
     *
     * @param gitlabTokenProvider Provider for the Gitlab token.
     */
    void useProperty(
        Provider<String> gitlabTokenProvider
    ) {
        projectOperations.stringTools.updateStringProperty(this.defaultCreds, gitlabTokenProvider)
    }

    /**
     * Use a provider for the Gitlab token for a specific workspace.
     *
     * Calling this will remove any external influence available via {@link #useGitlabTokenFromEnvironment}.
     *
     * @param gitlabTokenProvider Provider for the Gitlab token.
     */
    void useProperty(
        String workspace,
        Provider<String> gitlabTokenProvider
    ) {
        credentialsMap.put(workspace, gitlabTokenProvider)
    }

    private final ProjectOperations projectOperations
    private final Map<String, Provider<String>> credentialsMap = [:]
    private final Property<String> defaultCreds
    private final Transformer<SessionCredentials, String> transformer
}
