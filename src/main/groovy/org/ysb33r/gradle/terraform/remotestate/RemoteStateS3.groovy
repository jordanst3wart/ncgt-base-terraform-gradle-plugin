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
package org.ysb33r.gradle.terraform.remotestate

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.ysb33r.grolifant.api.core.ProjectOperations
import org.ysb33r.grolifant.api.v4.StringUtils

import java.util.concurrent.Callable

/** An extension that is added to {@link TerraformRemoteStateExtension}.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.8.0
 */
@CompileStatic
class RemoteStateS3 implements RemoteStateS3Provider {
    public static final String NAME = 's3'

    /** Utility method to find this extension on a project.
     *
     * @param project Project context
     * @return Extension after it has been attached.
     */
    static RemoteStateS3 findExtension(Project project) {
        ((ExtensionAware) TerraformRemoteStateExtension.findExtension(project)).extensions.getByType(RemoteStateS3)
    }

    /**
     * Utility to find this extension on a terraform source set.
     *
     * @param project Project context
     * @param sourceSetName Name of source set.
     * @return Extension after it has been attached.
     *
     * @since 0.10.0
     */
    static RemoteStateS3 findExtension(Project project, String sourceSetName) {
        def remote = TerraformRemoteStateExtension.findExtension(project, sourceSetName)
        ((ExtensionAware) remote).extensions.getByType(RemoteStateS3)
    }

    /**
     * Constructs a description of Terraform remote state stored in S3.
     *
     * @param project Project this is associated with.
     *   The object reference is not cached, so it is configuration-cache safe.
     */
    RemoteStateS3(Project project) {
        this.projectOperations = ProjectOperations.find(project)
        this.region = project.objects.property(String)
        this.bucket = project.objects.property(String)
        this.dynamoDbLockTableArn = project.objects.property(String)
    }

    /** Set the default AWS region where the remote state will be stored.
     *
     * @param p Object that can be converted to a string. Can be a {@code Provider} as well.
     */
    void setRegion(Object p) {
        this.region.set(projectOperations.provider({
            StringUtils.stringize(p)
        } as Callable<String>))
    }

    /** The default AWS region where remote state will be stored.
     *
     */
    Provider<String> getRegion() {
        this.region
    }

    /** Set the default AWS bucket where the remote state will be stored.
     *
     * @param p Object that can be converted to a string. Can be a {@code Provider} as well.
     */
    void setBucket(Object p) {
        this.bucket.set(projectOperations.provider({
            StringUtils.stringize(p)
        } as Callable<String>))
    }

    /** The default AWS bucket where remote state will be stored.
     *
     */
    Provider<String> getBucket() {
        this.bucket
    }

    /** Set the ARN to the DynamobDb table used for locking.
     *
     * @param p Object that can be converted to a string. Can be a {@code Provider} as well.
     *
     * @since 0.17.0
     */
    void setDynamoDbLockTableArn(Object p) {
        this.dynamoDbLockTableArn.set(projectOperations.provider({
            StringUtils.stringize(p)
        } as Callable<String>))
    }

    /** If DynamoDB is used for state locking, then this contains the ARN of the table.
     *
     * @since 0.17.0
     */
    Provider<String> getDynamoDbLockTableArn() {
        this.dynamoDbLockTableArn
    }

    /** Make settings follow that of another {@code RemoteStateS3} provider.
     *
     * @param s3 Providers
     */
    void follow(RemoteStateS3Provider s3) {
        this.region.set(s3.region)
        this.dynamoDbLockTableArn.set(s3.dynamoDbLockTableArn)
        this.bucket.set(s3.bucket)
    }

    private final ProjectOperations projectOperations
    private final Property<String> region
    private final Property<String> bucket
    private final Property<String> dynamoDbLockTableArn
}
