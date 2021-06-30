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
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.ysb33r.gradle.terraform.plugins.TerraformBasePlugin
import org.ysb33r.gradle.terraform.remotestate.RemoteStateS3Spec

import java.util.concurrent.Callable

import static org.ysb33r.gradle.terraform.remotestate.RemoteStateS3Spec.TOKEN_BUCKET
import static org.ysb33r.gradle.terraform.remotestate.RemoteStateS3Spec.TOKEN_REGION
import static org.ysb33r.gradle.terraform.remotestate.RemoteStateS3Spec.TOKEN_REMOTE_STATE_NAME
import static org.ysb33r.grolifant.api.v4.StringUtils.stringize

@CompileStatic
class RemoteStateAwsS3ConfigGenerator extends AbstractRemoteStateConfigGenerator {

    public static final String CONFIG_FILE_NAME = 'terraform-s3-backend-config.tf'

    @SuppressWarnings('UnnecessaryCast')
    RemoteStateAwsS3ConfigGenerator() {
        group = TerraformBasePlugin.TERRAFORM_TASK_GROUP
        description = 'Generates configuration for remote state in S3'
        textTemplate = """
bucket = "@@${TOKEN_BUCKET}@@"
key    = "@@${TOKEN_REMOTE_STATE_NAME}@@.tfstate"
region = "@@${TOKEN_REGION}@@"
        """

        this.remoteStateS3Spec = new RemoteStateS3Spec(projectOperations)
    }

    /**
     * Configures a {@link RemoteStateS3Spec}.
     *
     * @param spec Action to configure spec
     *
     * @since 1.0
     */
    void s3Spec(Action<RemoteStateS3Spec> spec) {
        spec.execute(this.remoteStateS3Spec)
    }

    /**
     * Configures a {@link RemoteStateS3Spec}.
     *
     * @param spec Closure to configure spec
     *
     * @since 1.0
     */
    void s3Spec(@DelegatesTo(RemoteStateS3Spec) Closure spec) {
        Closure configurator = (Closure) spec.clone()
        configurator.resolveStrategy = Closure.DELEGATE_FIRST
        configurator.delegate = this.remoteStateS3Spec
        configurator()
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
        this.remoteStateS3Spec.s3BucketName = bucketName
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
        projectOperations.provider(new Callable<String>() {
            @Override
            String call() throws Exception {
                tokens[TOKEN_BUCKET] ? stringize(tokens[TOKEN_BUCKET]) : null
            }
        })
    }

    /** Sets a new remote state name
     *
     * Sets this as a token called {@code remote_state_name}
     *
     * @param rsn Anything that can be lazy-evaluated to a string.
     *
     * @deprecated Configure via {@link #s3Spec} instead.
     *
     */
    @Deprecated
    void setRemoteStateName(Object rsn) {
        this.remoteStateS3Spec.remoteStateName = rsn
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
        projectOperations.provider(new Callable<String>() {
            @Override
            String call() throws Exception {
                tokens[TOKEN_REMOTE_STATE_NAME] ? stringize(tokens[TOKEN_REMOTE_STATE_NAME]) : null
            }
        })
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
        this.remoteStateS3Spec.awsRegion = region
    }

    /** Get AWS region used for remote storage.
     *
     * @return Region*
     * @deprecated
     */
    @Internal
    @Deprecated
    Provider<String> getAwsRegion() {
        projectOperations.provider(new Callable<String>() {
            @Override
            String call() throws Exception {
                tokens[TOKEN_REGION] ? stringize(tokens[TOKEN_REGION]) : null
            }
        })
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
    @Override
    Map<String, Object> getTokens() {
        Map<String, Object> newTokens = [:]
        newTokens.putAll(super.tokens)
        if (newTokens.containsKey(TOKEN_BUCKET)) {
            newTokens.putIfAbsent('aws_bucket', newTokens[TOKEN_BUCKET])
        }
        if (newTokens.containsKey(TOKEN_REGION)) {
            newTokens.putIfAbsent('aws_region', newTokens[TOKEN_REGION])
        }
        if (newTokens.containsKey(TOKEN_REMOTE_STATE_NAME)) {
            newTokens.putIfAbsent('name', newTokens[TOKEN_REMOTE_STATE_NAME])
        }
        newTokens
    }

    @Override
    protected String getConfigFileName() {
        CONFIG_FILE_NAME
    }

    private final RemoteStateS3Spec remoteStateS3Spec
}
