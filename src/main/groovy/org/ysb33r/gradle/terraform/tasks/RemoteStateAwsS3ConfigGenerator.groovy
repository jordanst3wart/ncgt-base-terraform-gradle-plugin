/*
 * Copyright 2017-2020 the original author or authors.
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
import groovy.transform.PackageScope
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.ysb33r.gradle.terraform.plugins.TerraformBasePlugin

import static org.ysb33r.grolifant.api.v4.StringUtils.updateStringProperty

@CompileStatic
class RemoteStateAwsS3ConfigGenerator extends AbstractRemoteStateConfigGenerator {

    public static final String CONFIG_FILE_NAME = 'terraform-s3-backend-config.tf'

    @SuppressWarnings('UnnecessaryCast')
    RemoteStateAwsS3ConfigGenerator() {
        group = TerraformBasePlugin.TERRAFORM_TASK_GROUP
        description = 'Generates configuration for remote state in S3'

        this.awsRegion = project.objects.property(String)
        this.bucketName = project.objects.property(String)
        this.remoteStateName = project.objects.property(String)

        tokens = [
            aws_region       : this.awsRegion,
            remote_state_name: this.remoteStateName,
            bucket_name      : this.bucketName
        ] as Map<String, Object>
    }

    /**
     *  Sets the S3 bucket used for state storage.
     *
     * @param bucketName Bucket name
     */
    void setS3BucketName(Object bucketName) {
        projectOperations.updateStringProperty(this.bucketName, bucketName)
    }

    /** The S3 bucket used for state storage
     *
     * @return Bucket name
     */
    @Input
    Provider<String> getS3BucketName() {
        this.bucketName
    }

    /** Sets a new remote state name
     *
     * @param rsn Anything that can be lazy-evaluted to a string.
     */
    void setRemoteStateName(Object rsn) {
        projectOperations.updateStringProperty(this.remoteStateName, rsn)
    }

    /** The name that will be used to identify remote state.
     *
     * @return Remote state name
     */
    @Input
    Provider<String> getRemoteStateName() {
        this.remoteStateName
    }

    /**
     * The AWS region used for remote state.
     *
     * @param region Anything convertible to a string.
     */
    void setAwsRegion(Object region) {
        projectOperations.updateStringProperty(this.awsRegion, region)
    }

    /** Get AWS region used for remote storage.
     *
     * @return Region
     */
    @Input
    Provider<String> getAwsRegion() {
        this.awsRegion
    }

    @Override
    protected String getConfigFileName() {
        CONFIG_FILE_NAME
    }

    @Override
    protected String getTemplateResourcePath() {
        TEMPLATE_RESOURCE_PATH
    }

    private final Property<String> awsRegion
    private final Property<String> remoteStateName
    private final Property<String> bucketName

    @PackageScope
    static final String TEMPLATE_RESOURCE_PATH = '/terraform-gradle-templates/aws-s3-remote-state.tf'
}
