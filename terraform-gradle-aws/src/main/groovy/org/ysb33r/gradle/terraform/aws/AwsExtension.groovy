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
package org.ysb33r.gradle.terraform.aws

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Transformer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.ysb33r.gradle.terraform.aws.internal.DefaultAwsCredentialsSpec
import org.ysb33r.gradle.terraform.aws.internal.TerraformAwsAssumeRoleCredentials
import org.ysb33r.gradle.terraform.aws.internal.TerraformAwsFixedCredentials
import org.ysb33r.gradle.terraform.credentials.SessionCredentials
import org.ysb33r.gradle.terraform.credentials.SessionCredentialsProvider
import org.ysb33r.grolifant.api.core.ProjectOperations
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider

import java.util.concurrent.Callable

import static org.ysb33r.grolifant.api.v4.MapUtils.stringizeValues

/**
 * An extension that is added to terraform source sets.
 *
 * It aids in the provision of credentials when using {@code Terraform} with {@code AWS}.
 *
 * By default, no credentials are passed. However, in this case if
 * {@link org.ysb33r.gradle.terraform.TerraformExtension#useAwsEnvironment} was set, then that will be applied.
 * Calling any authentication setting in this extension will turn that off.
 *
 * Authentication can be applied to all workspaces or customised to specific workspaces. In addition workspaces
 * can use assumed role authentication instead. Certain calls will eliminate other previous calls. For instance, if
 * {@link #usePropertiesForAws} was called for a specific workspace, and {@link #usePropertiesForAssumeRole} is then
 * called for the same workspace then the latter wins. However if {@link #usePropertiesForAws} was called for a
 * specific workspace and {@link #useAwsCredentialsFromEnvironment} is then called, all other workspaces will use
 * the standard credentials, but the specific workspace will still use the assumed role.
 *
 * This extension is unique to every source set. If you have multiple source sets you will need to provide the
 * credentials for each of them.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.11
 */
@CompileStatic
class AwsExtension implements SessionCredentialsProvider {

    public static final String NAME = 'aws'

    AwsExtension(Project tempProjectReference) {
        this.projectOperations = ProjectOperations.find(tempProjectReference)
        this.objectFactory = tempProjectReference.objects
        this.defaultCreds = new Config()
        this.envTransformer = new Transformer<SessionCredentials, String>() {
            @Override
            TerraformAwsSessionCredentials transform(String s) {
                Config config = credentialsMap.getOrDefault(s, defaultCreds)
                if (config.assumedRoleSpec) {
                    new TerraformAwsAssumeRoleCredentials(
                        config.awsCredentialsProviderChain,
                        config.assumedRoleSpec
                    )
                } else {
                    new TerraformAwsFixedCredentials(stringizeValues(config.env).findAll { it.value })
                }
            }
        }
        this.sessionNameFactory = new Callable<Provider<String>>() {
            @Override
            Provider<String> call() throws Exception {
                projectOperations.providerTools
                    .map(projectOperations.systemProperty('user.name')) { String it ->
                        final String head = it ? "tf-${it}" : 'terraform'
                        final String name = "${head}-${UUID.randomUUID()}"
                        name.size() > 64 ? name[0..63] : name
                    }
            }
        }
    }

    /**
     * Removes any credential customisations and reset to a no-credential state.
     */
    void clearAllCredentials() {
        this.credentialsMap.clear()
        this.defaultCreds = new Config()
    }

    /**
     * Whether credentials were configured.
     *
     * @return {@code true} if credentials were configured.
     */
    boolean hasCredentials() {
        !this.credentialsMap.empty || !defaultCreds.env.empty || defaultCreds.assumedRoleSpec
    }

    /**
     * Sets the default to be to use AWS credentials from the environment and pass as-is to {@code Terraform}.
     *
     */
    void useAwsCredentialsFromEnvironment() {
        defaultCreds.env.clear()
        defaultCreds.env.putAll(AWS_ENV.collectEntries {
            [it, projectOperations.environmentVariable(it)]
        })
    }

    /**
     * Sets the credentials for a specific workspace
     * to use AWS credentials from the environment and pass as-is to {@code Terraform}.
     *
     * THis will replace any assumeRole configuration for the specific workspace.
     *
     * @param workspace Workspace to use.
     */
    void useAwsCredentialsFromEnvironment(String workspace) {
        this.credentialsMap.put(workspace, new Config(AWS_ENV.collectEntries {
            [it, projectOperations.environmentVariable(it)]
        }))
    }

    /**
     * Pass these property values to {@code Terraform} to use as authentication for a specific workspace.
     *
     * Use the values that are supplied by the following property names.
     * Properties are searched in order of Gradle properties, then system properties and finally
     *   environmental variables. For the latter the convention of converting dots to underscores and uppercasing
     *   the name is used.
     *
     * Calling this will remove any external influence available via {@link #useAwsCredentialsFromEnvironment}.
     *
     * @param accessKeyIdPropertyName Property name for AWS access key.
     * @param secretPropertyName Property name for AWS secret.
     *
     * @deprecated Use {@link #usePropertiesForAws(Action < AwsCredentialsSpec > creds)} instead.
     */
    @Deprecated
    void usePropertiesForAws(
        String accessKeyIdPropertyName,
        String secretPropertyName
    ) {
        usePropertiesForAws {
            it.accessKey = accessKeyIdPropertyName
            it.secretKey = secretPropertyName
        }
    }

    /**
     * Pass these property values to {@code Terraform} to use as authentication for a specific workspace.
     *
     * Use the values that are supplied by the following property names.
     * Properties are searched in order of Gradle properties, then system properties and finally
     *   environmental variables. For the latter the convention of converting dots to underscores and uppercasing
     *   the name is used.
     *
     * @param workspace Workspace to apply this to.
     * @param accessKeyIdPropertyName Property name for AWS access key.
     * @param secretPropertyName Property name for AWS secret.
     *
     * @deprecated Use {@link #usePropertiesForAws(String workspace, Action < AwsCredentialsSpec > creds)} instead.
     */
    @Deprecated
    void usePropertiesForAws(
        String workspace,
        String accessKeyIdPropertyName,
        String secretPropertyName
    ) {
        usePropertiesForAws(workspace) {
            it.accessKey = accessKeyIdPropertyName
            it.secretKey = secretPropertyName
        }
    }

    /**
     * Pass these provider values to {@code Terraform} to use as authentication for all workspaces.
     *
     * Use the values that are supplied by the following providers.
     *
     * Calling this will remove any external influence available via
     * {@link #useAwsCredentialsFromEnvironmentForAssumeRole}.
     *
     * @param accessKeyId Provider for AWS access key.
     * @param secret Provider for AWS secret.
     *
     * @deprecated Use {@link #usePropertiesForAws(Action < AwsCredentialsSpec > creds)} instead.
     */
    @SuppressWarnings('UnnecessaryCast')
    @Deprecated
    void usePropertiesForAws(
        Provider<String> accessKeyId,
        Provider<String> secret
    ) {
        usePropertiesForAws {
            it.accessKey = accessKeyId
            it.secretKey = secret
        }
    }

    /**
     * Pass these provider values to {@code Terraform} to use as authentication for a specific workspace.
     *
     * Use the values that are supplied by the following providers.
     *
     * Calling this will remove any external influence available via
     * {@link #useAwsCredentialsFromEnvironmentForAssumeRole}.
     *
     * @param workspace Workspace to apply this to.
     * @param accessKeyId Provider for AWS access key.
     * @param secret Provider for AWS secret.
     *
     * @deprecated Use {@link #usePropertiesForAws(String workspace, Action < AwsCredentialsSpec > creds)} instead.
     */
    @Deprecated
    @SuppressWarnings('UnnecessaryCast')
    void usePropertiesForAws(
        String workspace,
        Provider<String> accessKeyId,
        Provider<String> secret
    ) {
        usePropertiesForAws(workspace) {
            it.accessKey = accessKeyId
            it.secretKey = secret
        }
    }

    /**
     * Pass these provider value to {@code Terraform} to use as authentication for all workspaces.
     *
     * Use the values that are supplied by the following providers.
     *
     * Calling this will remove any external influence available via
     * {@link #useAwsCredentialsFromEnvironmentForAssumeRole}.
     *
     * @param creds Credentials specification.
     *   If any of these resolve to null or empty, they will be ignored when providers are resolved.
     *
     * @since 0.15
     */
    void usePropertiesForAws(
        Action<AwsCredentialsSpec> creds
    ) {
        def spec = new DefaultAwsCredentialsSpec(projectOperations)
        creds.execute(spec)
        this.defaultCreds = new Config(spec.asMap)
    }

    /**
     * Pass these provider value to {@code Terraform} to use as authentication for a specific workspace.
     *
     * Use the values that are supplied by the following providers.
     *
     * Calling this will remove any external influence available via
     * {@link #useAwsCredentialsFromEnvironmentForAssumeRole}.
     *
     * @param workspace Workspace to apply this to.
     * @param creds Credentials specification.
     *   If any of these resolve to null or empty, they will be ignored when providers are resolved.
     *
     * @since 0.15
     */
    void usePropertiesForAws(
        String workspace,
        Action<AwsCredentialsSpec> creds
    ) {
        def spec = new DefaultAwsCredentialsSpec(projectOperations)
        creds.execute(spec)
        credentialsMap.put(workspace, new Config(spec.asMap))
    }

    /**
     * Obtain a session key for an assumed role.
     *
     * Use AWS credentials that are available in the environment
     *
     * This is applied to all workspaces.
     *
     * Calling this will remove any customisation done via {@link #usePropertiesForAssumeRole}.
     *
     * @param assumedRoleSpec Configure the assumed role details.
     *
     * @deprecated It is better to allow Terraform to assume roles.
     */
    @Deprecated
    void useAwsCredentialsFromEnvironmentForAssumeRole(
        Action<AssumedRoleSpec> assumedRoleSpec
    ) {
        def spec = createAssumedRoleSpec()
        assumedRoleSpec.execute(spec)
        this.defaultCreds = new Config(
            spec,
            AwsCredentialsProviderChain.of(EnvironmentVariableCredentialsProvider.create())
        )
    }

    /**
     *
     * Obtain a session key for an assumed role.
     *
     * Use AWS credentials that are available in the environment
     * or as system properties or in a credentials file for the mentioned workspace
     *
     * @param workspace Workspace to use credentials
     * @param assumedRoleSpec Configure the assumed role details.
     *
     * @deprecated It is better to allow Terraform to assume roles.
     */
    @Deprecated
    void useAwsCredentialsFromEnvironmentForAssumeRole(
        String workspace,
        Action<AssumedRoleSpec> assumedRoleSpec
    ) {
        def spec = createAssumedRoleSpec()
        assumedRoleSpec.execute(spec)
        credentialsMap.put(workspace, new Config(
            spec,
            AwsCredentialsProviderChain.of(EnvironmentVariableCredentialsProvider.create())
        ))
    }

    /**
     * Obtain a session key for an assume role on all workspaces.
     *
     * Use the values that are supplied by the following property names.
     * Properties are searched in order of Gradle properties, then system properties and finally
     *   environmental variables. For the latter the convention of converting dots to underscores and uppercasing
     *   the name is used.
     *
     * Calling this will remove any external influence available via
     * {@link #useAwsCredentialsFromEnvironmentForAssumeRole}.
     *
     * @param accessKeyIdPropertyName Property name for AWS access key.
     * @param secretPropertyName Property name for AWS secret.
     * @param assumedRoleSpec Configure the assumed role details.
     *
     * @deprecated It is better to allow Terraform to assume roles.
     */
    @Deprecated
    void usePropertiesForAssumeRole(
        String accessKeyIdPropertyName,
        String secretPropertyName,
        Action<AssumedRoleSpec> spec
    ) {
        usePropertiesForAssumeRole(
            projectOperations.resolveProperty(accessKeyIdPropertyName),
            projectOperations.resolveProperty(secretPropertyName),
            spec
        )
    }

    /**
     * Obtain a session key for an assumed role on a specific workspace.
     *
     * Use the values that are supplied by the following property names.
     * Properties are searched in order of Gradle properties, then system properties and finally
     *   environmental variables. For the latter the convention of converting dots to underscores and uppercasing
     *   the name is used.
     *
     * @param workspace Workspace to apply this to.
     * @param accessKeyIdPropertyName Property name for AWS access key.
     * @param secretPropertyName Property name for AWS secret.
     * @param assumedRoleSpec Configure the assumed role details.
     *
     * @deprecated It is better to allow Terraform to assume roles.
     */
    @Deprecated
    void usePropertiesForAssumeRole(
        String workspace,
        String accessKeyIdPropertyName,
        String secretPropertyName,
        Action<AssumedRoleSpec> spec
    ) {
        usePropertiesForAssumeRole(
            workspace,
            projectOperations.resolveProperty(accessKeyIdPropertyName),
            projectOperations.resolveProperty(secretPropertyName),
            spec
        )
    }

    /**
     * Obtain a session key for an assume role on all workspaces.
     *
     * Use the values that are supplied by the following providers.
     *
     * Calling this will remove any external influence available via
     * {@link #useAwsCredentialsFromEnvironmentForAssumeRole}.
     *
     * @param accessKeyId Provider for AWS access key.
     * @param secret Provider for AWS secret.
     * @param assumedRoleSpec Configure the assumed role details.
     *
     * @deprecated It is better to allow Terraform to assume roles.
     */
    @Deprecated
    void usePropertiesForAssumeRole(
        Provider<String> accessKeyId,
        Provider<String> secret,
        Action<AssumedRoleSpec> assumedRoleSpec
    ) {
        def spec = createAssumedRoleSpec()
        assumedRoleSpec.execute(spec)
        this.defaultCreds = new Config(
            spec,
            AwsCredentialsProviderChain.of(
                new TerraformAwsCredentialsProvider(accessKeyId, secret, this.projectOperations)
            )
        )
    }

    /**
     * Obtain a session key for an assume role on a specific workspace.
     *
     * Use the values that are supplied by the following providers.
     *
     * Calling this will remove any external influence available via
     * {@link #useAwsCredentialsFromEnvironmentForAssumeRole}.
     *
     * @param workspace Workspace to apply this to.
     * @param accessKeyId Provider for AWS access key.
     * @param secret Provider for AWS secret.
     * @param assumedRoleSpec Configure the assumed role details.
     *
     * @deprecated It is better to allow Terraform to assume roles.
     */
    @Deprecated
    void usePropertiesForAssumeRole(
        String workspace,
        Provider<String> accessKeyId,
        Provider<String> secret,
        Action<AssumedRoleSpec> assumedRoleSpec
    ) {
        def spec = createAssumedRoleSpec()
        assumedRoleSpec.execute(spec)
        credentialsMap.put(workspace, new Config(
            spec,
            AwsCredentialsProviderChain.of(
                new TerraformAwsCredentialsProvider(accessKeyId, secret, this.projectOperations)
            )
        ))
    }

    Provider<SessionCredentials> getCredentialsEnvForWorkspace(String name = 'default') {
        projectOperations.provider { -> name }.map(this.envTransformer)
    }

    /** Set a factory for creating session names
     *
     * @param factory A factory the can create session names.
     */
    void setSessionNameFactory(Callable<Provider<String>> factory) {
        this.sessionNameFactory = factory
    }

    private final ProjectOperations projectOperations
    private final ObjectFactory objectFactory
    private final TreeMap<String, Config> credentialsMap = new TreeMap<String, Config>()
    private final Transformer<SessionCredentials, String> envTransformer
    private Callable<Provider<String>> sessionNameFactory
    private Config defaultCreds

    private AssumedRoleSpec createAssumedRoleSpec() {
        def spec = new AssumedRoleSpec()
        spec.sessionName = sessionNameFactory.call()
        spec
    }

    private static final Set<String> AWS_ENV = System.getenv().keySet()
        .findAll { k -> k.startsWith('AWS_') }.toSet()

    private static class Config {
        AssumedRoleSpec assumedRoleSpec
        Map<String, Object> env
        AwsCredentialsProviderChain awsCredentialsProviderChain

        Config() {
            this.assumedRoleSpec = null
            this.awsCredentialsProviderChain = null
            this.env = [:]
        }

        Config(Map<String, Object> env) {
            this.assumedRoleSpec = null
            this.awsCredentialsProviderChain = null
            this.env = env
        }

        Config(AssumedRoleSpec spec, AwsCredentialsProviderChain chain) {
            this.assumedRoleSpec = spec
            this.awsCredentialsProviderChain = chain
            this.env = null
        }
    }
}
