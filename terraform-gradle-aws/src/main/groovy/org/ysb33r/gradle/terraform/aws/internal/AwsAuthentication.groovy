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
package org.ysb33r.gradle.terraform.aws.internal

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.credentials.AwsCredentials
import org.ysb33r.gradle.terraform.aws.AssumedRoleSpec
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse
import software.amazon.awssdk.services.sts.model.Credentials

import java.time.Instant

/**
 * Utilities for AWS Authentication
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.11
 */
@CompileStatic
@Slf4j
class AwsAuthentication {
    public static final String AWS_KEY = 'AWS_ACCESS_KEY_ID'
    public static final String AWS_SECRET = 'AWS_SECRET_ACCESS_KEY'
    public static final String AWS_TOKEN = 'AWS_SESSION_TOKEN'

    static class SimpleAwsCredentials implements AwsCredentials {
        String accessKey
        String secretKey
        String sessionToken
        Instant expiry
    }

    static SimpleAwsCredentials assumeRole(
        AwsCredentialsProviderChain providerChain,
        AssumedRoleSpec spec
    ) {
        if (FAKE_SESSION_TOKENS) {
            return fakeTokensForTesting(providerChain, spec)
        }

        StsClient.builder()
            .region(Region.of(spec.region))
            .credentialsProvider(providerChain)
            .build()
            .withCloseable { StsClient client ->
                AssumeRoleRequest roleRequest = (AssumeRoleRequest) AssumeRoleRequest.builder()
                    .roleArn(spec.roleArn)
                    .roleSessionName(spec.sessionName)
                    .durationSeconds(spec.durationSeconds)
                    .build()

                AssumeRoleResponse roleResponse = client.assumeRole(roleRequest)
                Credentials myCreds = roleResponse.credentials()
                if (log.debugEnabled) {
                    def exTime = myCreds.expiration()
                    def tokenInfo = myCreds.sessionToken()
                    log.debug("AWS Session token ${tokenInfo} expires on ${exTime}")
                }

                new SimpleAwsCredentials(
                    accessKey: myCreds.accessKeyId(),
                    secretKey: myCreds.secretAccessKey(),
                    sessionToken: myCreds.sessionToken(),
                    expiry: myCreds.expiration()
                )
            }
    }

    private static SimpleAwsCredentials fakeTokensForTesting(
        AwsCredentialsProviderChain providerChain,
        AssumedRoleSpec spec
    ) {
        def creds = providerChain.resolveCredentials()
        new SimpleAwsCredentials(
            accessKey: "${creds.accessKeyId()}_FAKE",
            secretKey: "${creds.secretAccessKey()}_FAKE",
            sessionToken: "${spec.roleArn}_${spec.region}_FAKE",
            expiry: Instant.now().plusSeconds(spec.durationSeconds)
        )
    }

    private static final boolean FAKE_SESSION_TOKENS =
        System.getProperty('org.ysb33r.gradle.terraform.integration.tests.fake.session.tokens')
}
