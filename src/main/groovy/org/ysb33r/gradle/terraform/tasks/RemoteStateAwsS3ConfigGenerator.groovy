package org.ysb33r.gradle.terraform.tasks

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.ysb33r.gradle.terraform.plugins.TerraformBasePlugin

import static org.ysb33r.grolifant.api.StringUtils.updateStringProperty

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
        updateStringProperty(project, this.bucketName, bucketName)
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
        updateStringProperty(project, this.remoteStateName, rsn)
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
        updateStringProperty(project, this.awsRegion, region)
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
