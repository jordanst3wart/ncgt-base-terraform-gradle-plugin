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
package org.ysb33r.gradle.terraform.remotestate

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.ysb33r.grolifant.api.core.ProjectOperations

/**
 * Describes the attributed for a remote S3 backend.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.10
 */
@CompileStatic
@SuppressWarnings('MethodCount')
@Slf4j
class RemoteStateS3Spec extends AbstractBackendSpec {
    public static final String NAME = 's3'
    public static final String TOKEN_BUCKET = 'bucket'
    public static final String TOKEN_REMOTE_STATE_NAME = 'key'
    public static final String TOKEN_REGION = 'region'

    /** Utility method to find this extension on a project.
     *
     * @param project Project context
     * @return Extension after it has been attached.
     *
     * @since 0.12
     */
    static RemoteStateS3Spec findExtension(Project project) {
        ((ExtensionAware) TerraformRemoteStateExtension.findExtension(project)).extensions.getByType(RemoteStateS3Spec)
    }

    /**
     * Utility to find this extension on a terraform source set.
     *
     * @param project Project context
     * @param sourceSetName Name of source set.
     * @return Extension after it has been attached.
     *
     * @since 0.12
     */
    static RemoteStateS3Spec findExtension(Project project, String sourceSetName) {
        def remote = TerraformRemoteStateExtension.findExtension(project, sourceSetName)
        ((ExtensionAware) remote).extensions.getByType(RemoteStateS3Spec)
    }

    final String defaultTextTemplate = '''
bucket = "@@bucket@@"
key    = "@@key@@"
region = "@@region@@"
'''

    RemoteStateS3Spec(ProjectOperations po, ObjectFactory objects) {
        super(po, objects)
    }

    /**
     * Name of backend.
     *
     * @return Name of backend.
     *
     * @since 0.12
     */
    final String name = NAME

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
     * Sets this as a token called {@code key}
     *
     * @param rsn Anything that can be lazy-evaluated to a string.
     */
    void setRemoteStateName(Object rsn) {
        token(TOKEN_REMOTE_STATE_NAME, rsn)
    }

    /** Sets a new remote state name
     *
     * Sets this as a token called {@code key}
     *
     * Alternative to {@link #setRemoteStateName}.
     *
     * @param rsn Anything that can be lazy-evaluated to a string.
     *
     * @since 0.12
     */
    void setKey(Object rsn) {
        remoteStateName = rsn
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
}
