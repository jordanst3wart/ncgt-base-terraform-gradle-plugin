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
package org.ysb33r.gradle.terraform.remotestate

import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import org.gradle.api.provider.Provider
import org.ysb33r.grolifant.api.core.ProjectOperations
import org.ysb33r.grolifant.api.v4.StringUtils

import java.util.concurrent.Callable

import static org.ysb33r.gradle.terraform.internal.TerraformUtils.escapedList
import static org.ysb33r.gradle.terraform.internal.TerraformUtils.escapedMap

/**
 * Describes the attributed for a remote S3 backend.
 *
 * @author Schalk W. Cronjé
 *
 * @since 1.0
 */
@CompileStatic
@SuppressWarnings('MethodCount')
class RemoteStateS3Spec {
    public static final String TOKEN_BUCKET = 'bucket'
    public static final String TOKEN_REMOTE_STATE_NAME = 'remote_state_name'
    public static final String TOKEN_REGION = 'region'
    public static final String TOKEN_ASSUME_ROLE_POLICY = 'assume_role_policy'
    public static final String TOKEN_DYNAMODB_TABLE_ARN = 'aws_dynamodb_lock_table_arn'

    RemoteStateS3Spec(ProjectOperations po) {
        this.projectOperations = po
        this.tokenProvider = po.provider(new Callable<Map<String, Object>>() {
            @Override
            Map<String, ?> call() throws Exception {
                tokens
            }
        })
    }

    /**
     * A provider for all the tokens that were set.
     *
     * @return Provider to a map of tokens.
     */
    Provider<Map<String, ?>> getTokenProvider() {
        this.tokenProvider
    }

    /**
     * Sets a token called {@code assume_role_duration_seconds}.
     *
     * @param value Number of seconds
     *
     *
     */
    void setAssumeRoleDurationSeconds(Integer value) {
        token('assume_role_duration_seconds', value)
    }

    /**
     * Sets a token called {@code assume_role_policy}.
     *
     * @param value Policy in the form of a map.
     *
     *
     */
    void setAssumeRolePolicy(Map<String, ?> policy) {
        token(TOKEN_ASSUME_ROLE_POLICY, projectOperations.provider { ->
            JsonOutput.toJson(policy)
        })
    }

    /**
     * Sets a token called {@code assume_role_policy}.
     *
     * @param value Policy as a JSON string.
     *
     *
     */
    void setAssumeRolePolicy(String policy) {
        token(TOKEN_ASSUME_ROLE_POLICY, policy)
    }

    /**
     * Sets a token called {@code assume_role_policy_arns}.
     *
     * @param value ARNs.
     *
     *
     */
    void setAssumeRolePolicyARNs(Iterable<Object> items) {
        token('assume_role_policy_arns', projectOperations.provider { ->
            escapedList(items)
        })
    }

    /**
     * Sets a token called {@code assume_role_policy_arns}.
     *
     * @param value ARNs.
     *
     *
     */
    void setAssumeRolePolicyARNs(Object... items) {
        assumeRolePolicyARNs = items.toList()
    }

    /**
     * Sets a token called {@code assume_role_tags}.
     *
     * @param value Tags.
     *
     *
     */
    void setAssumeRoleTags(Map<String, Object> items) {
        token('assume_role_tags', projectOperations.provider { ->
            escapedMap(items)
        })
    }

    /**
     * Sets a token called {@code assume_role_transitive_tag_keys}.
     *
     * @param value Tag keys.
     *
     *
     */
    void setAssumeRoleTransitiveTagKeys(Iterable<Object> items) {
        token('assume_role_transitive_tag_keys', projectOperations.provider { ->
            escapedList(items)
        })
    }

    /**
     * Sets a token called {@code assume_role_transitive_tag_keys}.
     *
     * @param value Tag keys.
     *
     *
     */
    void setAssumeRoleTransitiveTagKeys(Object... items) {
        assumeRoleTransitiveTagKeys = items.toList()
    }

    /**
     * Sets a token called {@code external_id}.
     *
     * @param value AWS external id for assumed role.
     *
     *
     */
    void setExternalId(Object value) {
        token('external_id', value)
    }

    /**
     * Sets a token called {@code role_arn}.
     *
     * @param value AWS ARN for assumed role.
     *
     *
     */
    void setRoleArn(Object value) {
        token('role_arn', value)
    }

    /**
     * Sets a token called {@code session_name}.
     *
     * @param value AWS ARN for assumed role.
     *
     *
     */
    void setSessionName(Object value) {
        token('session_name', value)
    }

    /**
     * Sets a token called {@code acl}.
     *
     * @param value Canned S3 ACL to be applied to state file.
     *
     * @see https://docs.aws.amazon.com/AmazonS3/latest/userguide/acl-overview.html#canned-acl
     *
     *
     */
    void setAcl(Object value) {
        token('acl', value)
    }

    /**
     * Sets a token called {@code encrypt}.
     *
     * @param value Enable server-side encryption of state file.
     *
     *
     */
    void setEncrypt(Boolean value) {
        token('encrypt', value)
    }

    /**
     * Sets a token called {@code endpoint}.
     *
     * @param value Custom S3 endpoint.
     *
     *
     */
    void setS3Endpoint(Object value) {
        token('endpoint', value)
    }

    /**
     * Sets a token called {@code force_path_style}.
     *
     * @param value {@code true} to force {@code https://<HOST>/<BUCKET>}.
     *
     *
     */
    void setForcePathStyle(Boolean value) {
        token('force_path_style', value)
    }

    /**
     * Sets a token called {@code kms_key_id}.
     *
     * @param value Key identifier.
     *
     *
     */
    void setSetKmsKeyId(Object value) {
        token('kms_key_id', value)
    }

    /**
     * Sets a token called {@code sse_customer_key}.
     *
     * @param value SSE customer key.
     *
     *
     */
    void setSseCustomerKey(Object value) {
        token('sse_customer_key', value)
    }

    /**
     * Sets a token called {@code workspace_key_prefix}.
     *
     * @param value Workspace key prefix for non-default workspace.
     *
     *
     */
    void setWorkspaceKeyPrefix(Object value) {
        token('workspace_key_prefix', value)
    }

    /**
     * Sets a token called {@code dynamodb_endpoint}.
     *
     * @param value Custom DynamoDB endpoint.
     *
     *
     */
    void setDynamoDbEndpoint(Object value) {
        token('dynamodb_endpoint', value)
    }

    /**
     * Sets a token called {@code dynamodb_table_arn}.
     *
     * @param value Full ARN to DynamoDB lock tabke.
     *
     *
     */
    void setDynamoDbTable(Object value) {
        token('dynamodb_table_arn', value)
    }

    /**
     * Sets a token called {@code aws_dynamodb_lock_table_arn}.
     *
     * @param value Full ARN to DynamoDB lock tabke.
     *
     *
     */
    void setDynamoDbLockTableArn(Object value) {
        token(TOKEN_DYNAMODB_TABLE_ARN, value)
    }

    /**
     * Sets a token called {@code access_key}.
     *
     * @param value AWS access key
     *
     *
     */
    void setAccessKey(Object value) {
        token('access_key', value)
    }

    /**
     * Sets a token called {@code secret_key}.
     *
     * @param value AWS secret
     *
     *
     */
    void setSecretKey(Object value) {
        token('secret_key', value)
    }

    /**
     * Sets a token called {@code iam_endpoint}.
     *
     * @param value Customised IAM endpoint
     *
     *
     */
    void setIamEndpoint(Object value) {
        token('iam_endpoint', value)
    }

    /**
     * Sets a token called {@code sts_endpoint}.
     *
     * @param value Customised STS endpoint
     *
     *
     */
    void setStsEndpoint(Object value) {
        token('sts_endpoint', value)
    }

    /**
     * Sets a token called {@code iam_endpoint}.
     *
     * @param value Max retries.
     *
     *
     */
    void setMaxRetries(Integer value) {
        token('max_retries', value)
    }

    /**
     * Sets a token called {@code profile}.
     *
     * @param value AWS credentials profile
     *
     *
     */
    void setProfile(Object value) {
        token('profile', value)
    }

    /**
     * Sets a token called {@code shared_credentials_file}.
     *
     * @param value AWS shared credentials file. Anything convertible to a file
     *
     *
     */
    void setCredentialsFile(Object value) {
        token('shared_credentials_file', projectOperations.provider { ->
            projectOperations.file(value).absolutePath
        })
    }

    /**
     * Sets a token called {@code skip_credentials_validation}.
     *
     * @param value {@code true} to skip credentials validation
     *
     *
     */
    void setSkipCredentialsValidation(Boolean value) {
        token('skip_credentials_validation', value)
    }

    /**
     * Sets a token called {@code skip_region_validation}.
     *
     * @param value {@code true} to skip region validation
     *
     *
     */
    void setSkipRegionValidation(Boolean value) {
        token('skip_region_validation', value)
    }

    /**
     * Sets a token called {@code skip_metadata_api_check}.
     *
     * @param value {@code true} to skip metadata API check.
     *
     *
     */
    void setSkipMetadataApiCheck(Boolean value) {
        token('skip_metadata_api_check', value)
    }

    /**
     *  Sets the S3 bucket used for state storage.
     *
     *  Sets this as a token called {@code bucket_name}
     *
     * @param bucketName Bucket name
     */
    void setS3BucketName(Object bucketName) {
        token(TOKEN_BUCKET, bucketName)
    }

    /**
     *  Alias for {@link #setS3BucketName}
     *
     * @param bucketName Bucket name
     */
    void setBucket(Object bucketName) {
        s3BucketName = bucketName
    }

    /** Sets a new remote state name
     *
     * Sets this as a token called {@code remote_state_name}
     *
     * @param rsn Anything that can be lazy-evaluated to a string.
     */
    void setRemoteStateName(Object rsn) {
        token(TOKEN_REMOTE_STATE_NAME, rsn)
    }

    /**
     * The AWS region used for remote state.
     *
     * Sets this as a token called {@code aws_region}.
     *
     * @param region Anything convertible to a string.
     */
    void setAwsRegion(Object region) {
        token(TOKEN_REGION, region)
    }

    /**
     * Alias for {@link #setAwsRegion}.
     *
     * @param region Anything convertible to a string.
     */
    void setRegion(Object region) {
        awsRegion = region
    }

    /**
     * Reset all tokens
     *
     * @since 1.0
     */
    void clear() {
        this.tokens.clear()
    }

    private void token(String key, Object value) {
        this.tokens.put(key, projectOperations.provider { -> StringUtils.stringize(value) })
    }

    private final ProjectOperations projectOperations
    private final Map<String, Object> tokens = [:] as TreeMap<String, Object>
    private final Provider<Map<String, ?>> tokenProvider
}
