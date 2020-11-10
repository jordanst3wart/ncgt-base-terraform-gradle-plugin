package org.ysb33r.gradle.terraform.remotestate;

import org.gradle.api.provider.Provider;

/**
 * Providers of the metaadata describing a remote state S3 configuration.
 *
 * @since 0.17.0
 */
public interface RemoteStateS3Provider {
    /** The default AWS region where remote state will be stored.
     *
     */
    Provider<String> getRegion();
    /** The default AWS bucket where remote state will be stored.
     *
     */
    Provider<String> getBucket();

    /** If DynamoDB is used for state locking, then this contains the ARN of the table.
     *
     * @since 0.17.0
     */
    Provider<String> getDynamoDbLockTableArn();
}
