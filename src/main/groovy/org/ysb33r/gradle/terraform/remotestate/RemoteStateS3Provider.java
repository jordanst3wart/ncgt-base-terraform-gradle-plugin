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
     * @return Region
     */
    Provider<String> getRegion();

    /** The default AWS bucket where remote state will be stored.
     *
     * @return S3 bucket name
     */
    Provider<String> getBucket();

    /** If DynamoDB is used for state locking, then this contains the ARN of the table.
     *
     * @return DynamoDB table ARN
     * 
     * @since 0.17.0
     */
    Provider<String> getDynamoDbLockTableArn();
}
