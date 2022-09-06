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
package org.ysb33r.gradle.terraform.remotestate;

import org.gradle.api.provider.Provider;

import java.util.Map;

import static org.ysb33r.gradle.terraform.internal.TerraformUtils.stringizeOrNull;
import static org.ysb33r.gradle.terraform.remotestate.RemoteStateS3Spec.*;

/**
 * Providers of the metadata describing a remote state S3 configuration.
 *
 * @deprecated
 */
@Deprecated
public interface RemoteStateS3Provider {
    /**
     * The default AWS region where remote state will be stored.
     *
     * @return Region
     */
    default Provider<String> getRegion() {
        return getAttributesMap().map(stringMap -> stringizeOrNull(stringMap.get(TOKEN_REGION)));
    }

    /**
     * The default AWS bucket where remote state will be stored.
     *
     * @return S3 bucket name
     */
    default Provider<String> getBucket() {
        return getAttributesMap().map(stringMap -> stringizeOrNull(stringMap.get(TOKEN_BUCKET)));
    }

    /**
     * If DynamoDB is used for state locking, then this contains the ARN of the table.
     *
     * @return DynamoDB table ARN.
     */
    @Deprecated
    default Provider<String> getDynamoDbLockTableArn() {
        return getAttributesMap().map(stringMap -> stringizeOrNull(stringMap.get(TOKEN_DYNAMODB_TABLE_ARN)));
    }

    /**
     * Returns a provider to a map of all S3 backend attributes that could possible be configured.
     *
     * @return Map provider
     *
     * @deprecated
     */
    @Deprecated
    Provider<Map<String, ?>> getAttributesMap();

    @Deprecated
    void setAssociatedRemoteStateExtension(TerraformRemoteStateExtension trse);

    @Deprecated
    TerraformRemoteStateExtension getAssociatedRemoteStateExtension();
}
