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
package org.ysb33r.gradle.terraform.aws

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.gradle.api.provider.Provider
import org.ysb33r.grolifant.api.core.ProjectOperations
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider

/**
 * A special credentials provider for use in the Terraform Gradle plugin, which can be add to an
 * authentication chain.
 *
 * @author Schalk W. Cronjé
 */
@CompileStatic
class TerraformAwsCredentialsProvider implements AwsCredentialsProvider {

    /** A provider that will look for AWS credentials.
     *
     * The convention is to search project and system properties first and then look for the
     * equivalent environmental variables. Failing that values will be set to {@code null}
     *
     * @param accessKeydId A provider to the access key id
     * @param secret A provider to the secret
     * @param po An instance of {@link ProjectOperations}.
     *
     * @since 0.11
     */
    TerraformAwsCredentialsProvider(
        Provider<String> accessKeyId,
        Provider<String> secret,
        ProjectOperations po
    ) {
        this.access = accessKeyId
        this.secret = secret
        this.projectOperations = po
    }

    /**
     * Resolves credentials
     *
     * @return A set of credentials suitable for AWS.
     */
    @Override
    AwsCredentials resolveCredentials() {
        new TerraformAwsCredentialsProvider.Credentials(
            projectOperations.providerTools.getOrNull(access),
            projectOperations.providerTools.getOrNull(secret),
        )
    }

    private final Provider<String> access
    private final Provider<String> secret
    private final ProjectOperations projectOperations

    @ToString
    static class Credentials implements AwsCredentials {
        private final String accessKeyId
        private final String secretKey

        Credentials(String access, String secret) {
            this.accessKeyId = access
            this.secretKey = secret
        }

        @Override
        String accessKeyId() {
            this.accessKeyId
        }

        @Override
        String secretAccessKey() {
            this.secretKey
        }
    }
}
