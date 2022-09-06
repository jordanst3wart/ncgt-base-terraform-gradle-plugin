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
package org.ysb33r.gradle.terraform.aws;

import org.gradle.api.provider.Provider;

import java.io.File;

/**
 * Configures a set of properties that are related to AWS Credentials.
 *
 * @since 0.15
 */
public interface AwsCredentialsSpec {

    /**
     * Set property to AWS access key.
     *
     * @param prop Property that will hold the value.
     */
    void setAccessKey(String prop);

    /**
     * Set property to AWS access key.
     *
     * @param prop Provider that will hold the value.
     */
    void setAccessKey(Provider<String> prop);

    /**
     * Set property to AWS secret key.
     *
     * @param prop Property that will hold the value.
     */
    void setSecretKey(String prop);

    /**
     * Set property to AWS secret key.
     *
     * @param prop Provider that will hold the value.
     */
    void setSecretKey(Provider<String> prop);

    /**
     * Set property to AWS profile.
     *
     * @param prop Property that will hold the value.
     */
    void setProfile(String prop);

    /**
     * Set property to AWS profile.
     *
     * @param prop Provider that will hold the value.
     */
    void setProfile(Provider<String> prop);

    /**
     * Set property that will point to AWS configuration file.
     *
     * @param prop Property that will hold the value.
     */
    void setConfigFile(String prop);

    /**
     * Set property that will point to AWS configuration file.
     *
     * @param prop Provider that will hold the value.
     */
    void setConfigFile(Provider<File> prop);

    /**
     * Set property to AWS shared credentials file.
     *
     * @param prop Property that will hold the value.
     */
    void setCredentialsFile(String prop);

    /**
     * Set property to AWS shared credentials file.
     *
     * @param prop Provider that will hold the value.
     */
    void setCredentialsFile(Provider<File> prop);
}
