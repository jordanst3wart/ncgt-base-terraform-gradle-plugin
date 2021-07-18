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
package org.ysb33r.gradle.terraform.tasks

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskProvider
import org.ysb33r.gradle.terraform.errors.TerraformConfigurationException
import org.ysb33r.gradle.terraform.errors.TerraformUnknownBackendException
import org.ysb33r.gradle.terraform.remotestate.BackendSpec
import org.ysb33r.gradle.terraform.remotestate.RemoteStateS3Spec
import org.ysb33r.grolifant.api.core.ProjectOperations

import javax.inject.Inject

import static org.ysb33r.gradle.terraform.remotestate.RemoteStateS3Spec.TOKEN_BUCKET
import static org.ysb33r.gradle.terraform.remotestate.RemoteStateS3Spec.TOKEN_REGION
import static org.ysb33r.gradle.terraform.remotestate.RemoteStateS3Spec.TOKEN_REMOTE_STATE_NAME
import static org.ysb33r.grolifant.api.v4.StringUtils.stringize

/**
 * Legacy way of generating S3 backend config files.
 *
 * @deprecated {@link RemoteStateConfigGenerator} has replaced this.
 */
@CompileStatic
@Deprecated
class RemoteStateAwsS3ConfigGenerator extends DefaultTask {

    @SuppressWarnings('UnnecessaryCast')
    @Inject
    RemoteStateAwsS3ConfigGenerator(TaskProvider<RemoteStateConfigGenerator> linkedTask) {
        dependsOn(linkedTask)
        def pt = ProjectOperations.find(project).providerTools
        this.backend = pt.flatMap(linkedTask) { it.backendProvider }
        this.sourceSetName = name.replaceFirst(~/^create/, '')
            .replaceFirst(~/s3BackendConfiguration$/, '')
            .uncapitalize()
        this.backendConfigFile = pt.flatMap(linkedTask) { it.backendConfigFile }
        this.destinationDir = pt.flatMap(linkedTask) { it.destinationDir }
    }

    /**
     * Configures a {@link RemoteStateS3Spec}.
     *
     * @param spec Action to configure spec
     *
     */
    @Deprecated
    void s3Spec(Action<RemoteStateS3Spec> spec) {
        warnOnUsage('configure')
        linkedSpec.configure(spec)
    }

    /**
     * Configures a {@link RemoteStateS3Spec}.
     *
     * @param spec Closure to configure spec
     *
     */
    @Deprecated
    @SuppressWarnings('DuplicateStringLiteral')
    void s3Spec(@DelegatesTo(RemoteStateS3Spec) Closure spec) {
        warnOnUsage('configure')
        linkedSpec.configure(spec)
    }

    /**
     *  Sets the S3 bucket used for state storage.
     *
     *  Sets this as a token called {@code bucket_name}
     *
     * @param bucketName Bucket name
     *
     * @deprecated Configure via {@link #s3Spec} instead.
     */
    @Deprecated
    void setS3BucketName(Object bucketName) {
        warnOnUsage('setS3BucketName')
        linkedSpec.s3BucketName = bucketName
    }

    /** The S3 bucket used for state storage
     *
     * @return Bucket name provider
     *
     * @deprecated
     */
    @Internal
    @Deprecated
    Provider<String> getS3BucketName() {
        warnOnUsage('getS3BucketName')
        linkedSpecProvider.map {
            it.tokens[TOKEN_BUCKET] ? stringize(it.tokens[TOKEN_BUCKET]) : null
        }
    }

    /** Sets a new remote state name
     *
     * Sets this as a token called {@code remote_state_name}
     *
     * @param rsn Anything that can be lazy-evaluated to a string.
     *
     */
    @Deprecated
    void setRemoteStateName(Object rsn) {
        warnOnUsage('setRemoteStateName')
        linkedSpec.remoteStateName = rsn
    }

    /** The name that will be used to identify remote state.
     *
     * @return Remote state name
     *
     * @deprecated
     */
    @Internal
    @Deprecated
    Provider<String> getRemoteStateName() {
        warnOnUsage('getRemoteStateName')
        linkedSpecProvider.map {
            it.tokens[TOKEN_REMOTE_STATE_NAME] ? stringize(it.tokens[TOKEN_REMOTE_STATE_NAME]) : null
        }
    }

    /**
     * The AWS region used for remote state.
     *
     * Sets this as a token called {@code aws_region}.
     *
     * @param region Anything convertible to a string.
     *
     * @deprecated Configure via {@link #s3Spec} instead
     */
    @Deprecated
    void setAwsRegion(Object region) {
        warnOnUsage('setAwsRegion')
        linkedSpec.awsRegion = region
    }

    /** Get AWS region used for remote storage.
     *
     * @return Region* @deprecated
     */
    @Internal
    @Deprecated
    Provider<String> getAwsRegion() {
        warnOnUsage('getAwsRegion')
        linkedSpecProvider.map {
            it.tokens[TOKEN_REGION] ? stringize(it.tokens[TOKEN_REGION]) : null
        }
    }

    /** Returns the current set of tokens
     *
     * For legacy purposes the following tokens are added as aliases.
     *
     * <ul>
     *     <li>name as alias for remote_state_name.</li>
     *     <li>aws_bucket as alias for bucket.</li>
     *     <li>aws_region as alias for region.</li>
     * </ul>
     *
     * @return Tokens used for replacements.
     */
    @Deprecated
    @Internal
    Map<String, Object> getTokens() {
        warnOnUsage('getTokens')
        backend.get().tokens
    }

    @SuppressWarnings('UnusedMethodParameter')
    void setDestinationDir(Object dir) {
        notSupported('setDestinationDir')
    }

    @Internal
    Provider<File> getDestinationDir() {
        warnOnUsage('getDestinationDir')
        this.backendConfigFile
    }

    @Internal
    Provider<File> getBackendConfigFile() {
        warnOnUsage('getBackendConfigFile')
        this.backendConfigFile
    }

    void setTemplateFile(Object file) {
        warnOnUsage('setTemplateFile')
        linkedSpec.templateFile = file
    }

    void setTextTemplate(Object text) {
        warnOnUsage('setTextTemplate')
        linkedSpec.textTemplate = text
    }

    @Internal
    Provider<File> getTemplateFile() {
        warnOnUsage('getTemplateFile')
        linkedSpecProvider.map {
            it.templateFile.get()
        }
    }

    @Internal
    Provider<String> getTextTemplate() {
        warnOnUsage('getTextTemplate')
        linkedSpecProvider.map {
            it.textTemplate.get().template(it)
        }
    }

    void delimiterTokenPair(String begin, String end) {
        warnOnUsage('delimiterTokenPair')
        linkedSpec.delimiterTokenPair(begin, end)
    }

    @Internal
    String getBeginToken() {
        warnOnUsage('getBeginToken')
        linkedSpec.beginToken
    }

    @Internal
    String getEndToken() {
        warnOnUsage('getEndToken')
        linkedSpec.endToken
    }

    void setTokens(Map<String, Object> newTokens) {
        warnOnUsage('setTokens')
        linkedSpec.tokens = newTokens
    }

    void tokens(Map<String, Object> moreTokens) {
        warnOnUsage('tokens')
        linkedSpec.tokens(moreTokens)
    }

    void token(String key, Object value) {
        warnOnUsage('token')
        linkedSpec.token(key, value)
    }

    @SuppressWarnings('UnusedMethodParameter')
    void addTokenProvider(Provider<Map<String, Object>> tokenProvider) {
        notSupported('addTokenProvider')
    }

    private void notSupported(String method) {
        throw new TerraformConfigurationException("${name} is a legacy task and '${method}' is not supported")
    }

    private void warnOnUsage(String method) {
        logger.warn "Task '${name}' is deprecated. Use method ${method} ${REMOTE_S3} '${sourceSetName}'."
    }

    private RemoteStateS3Spec getLinkedSpec() {
        BackendSpec backendSpec = backend.get()
        if (backendSpec instanceof RemoteStateS3Spec) {
            (RemoteStateS3Spec) backendSpec
        } else {
            notS3()
        }
    }

    private Provider<RemoteStateS3Spec> getLinkedSpecProvider() {
        backend.map { BackendSpec it ->
            if (it instanceof RemoteStateS3Spec) {
                (RemoteStateS3Spec) it
            } else {
                notS3()
            }
        }
    }

    private RemoteStateS3Spec notS3() {
        throw new TerraformUnknownBackendException('Cannot execute operation as the backend is not S3')
    }

    private final String sourceSetName
    private final Provider<BackendSpec> backend
    private final Provider<File> backendConfigFile
    private final Provider<File> destinationDir
    private final static String REMOTE_S3 = 'from remote.s3 or remote.of("s3") on Terraform source set'
}
