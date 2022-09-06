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
package org.ysb33r.gradle.terraform.aws.internal

import groovy.transform.CompileStatic
import org.ysb33r.gradle.terraform.aws.AssumedRoleSpec
import org.ysb33r.gradle.terraform.aws.TerraformAwsSessionCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain

import java.time.Instant

import static org.ysb33r.gradle.terraform.aws.internal.AwsAuthentication.AWS_KEY
import static org.ysb33r.gradle.terraform.aws.internal.AwsAuthentication.AWS_SECRET
import static org.ysb33r.gradle.terraform.aws.internal.AwsAuthentication.AWS_TOKEN
import static org.ysb33r.gradle.terraform.aws.internal.AwsAuthentication.SimpleAwsCredentials

/**
 * Credentials that can be used for a specific session
 *
 * @since 0.11
 */
@CompileStatic
class TerraformAwsAssumeRoleCredentials implements TerraformAwsSessionCredentials {

    TerraformAwsAssumeRoleCredentials(
        final AwsCredentialsProviderChain awsCredentialsProviderChain,
        final AssumedRoleSpec assumedRoleSpec
    ) {
        this.assumedRoleSpec = assumedRoleSpec
        this.awsCredentialsProviderChain = awsCredentialsProviderChain
        def creds = getSession(assumedRoleSpec, awsCredentialsProviderChain)
        this.env = [(AWS_KEY)   : creds.accessKey,
                    (AWS_SECRET): creds.secretKey,
                    (AWS_TOKEN) : creds.sessionToken
        ]
        this.expiry = creds.expiry
    }

    Map<String, String> getEnvironment() {
        this.env
    }

    boolean isExpired() {
        Instant.now().plusSeconds(10) >= expiry
    }

    TerraformAwsAssumeRoleCredentials refresh() {
        new TerraformAwsAssumeRoleCredentials(this.awsCredentialsProviderChain, this.assumedRoleSpec)
    }

    private final Map<String, String> env
    private final Instant expiry
    private final AssumedRoleSpec assumedRoleSpec
    private final AwsCredentialsProviderChain awsCredentialsProviderChain

    private static SimpleAwsCredentials getSession(
        AssumedRoleSpec assumedRoleSpec,
        AwsCredentialsProviderChain awsCredentialsProviderChain
    ) {
        AwsAuthentication.assumeRole(
            awsCredentialsProviderChain,
            assumedRoleSpec
        )
    }
}
