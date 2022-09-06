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
import org.gradle.api.provider.Provider
import org.ysb33r.gradle.terraform.aws.AwsCredentialsSpec
import org.ysb33r.grolifant.api.core.ProjectOperations

/**
 * Implementation of credentials specification.
 *
 * @since 0.15
 */
@CompileStatic
class DefaultAwsCredentialsSpec implements AwsCredentialsSpec {

    DefaultAwsCredentialsSpec(ProjectOperations projectOperations) {
        this.projectOperations = projectOperations
    }

    @Override
    void setAccessKey(String prop) {
        map[AwsAuthentication.AWS_KEY] = projectOperations.resolveProperty(prop)
    }

    @Override
    void setAccessKey(Provider<String> prop) {
        map[AwsAuthentication.AWS_KEY] = prop
    }

    @Override
    void setSecretKey(String prop) {
        map[AwsAuthentication.AWS_SECRET] = projectOperations.resolveProperty(prop)
    }

    @Override
    void setSecretKey(Provider<String> prop) {
        map[AwsAuthentication.AWS_SECRET] = prop
    }

    @Override
    void setProfile(String prop) {
        map[AwsAuthentication.AWS_PROFILE] = projectOperations.resolveProperty(prop)
    }

    @Override
    void setProfile(Provider<String> prop) {
        map[AwsAuthentication.AWS_PROFILE] = prop
    }

    @Override
    void setConfigFile(String prop) {
        map[AwsAuthentication.AWS_CONFIG_FILE] = projectOperations.resolveProperty(prop)
    }

    @Override
    void setConfigFile(Provider<File> prop) {
        map[AwsAuthentication.AWS_CONFIG_FILE] = prop.map { it.absolutePath }
    }

    @Override
    void setCredentialsFile(String prop) {
        map[AwsAuthentication.AWS_CREDENTIALS_FILE] = projectOperations.resolveProperty(prop)
    }

    @Override
    void setCredentialsFile(Provider<File> prop) {
        map[AwsAuthentication.AWS_CREDENTIALS_FILE] = prop.map { it.absolutePath }
    }

    Map<String, Object> getAsMap() {
        this.map as Map<String,Object>
    }

    private final ProjectOperations projectOperations
    private final Map<String, Provider<String>> map = [:]
}
