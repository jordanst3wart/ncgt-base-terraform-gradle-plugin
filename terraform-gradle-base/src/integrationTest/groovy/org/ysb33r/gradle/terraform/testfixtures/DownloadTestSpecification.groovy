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
package org.ysb33r.gradle.terraform.testfixtures

import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.grolifant.api.core.OperatingSystem
import spock.lang.Specification

@SuppressWarnings('LineLength')
class DownloadTestSpecification extends Specification {

    static final String TERRAFORM_VERSION = System.getProperty('TERRAFORM_VERSION') ?: TerraformExtension.TERRAFORM_DEFAULT
    static final File TERRAFORM_CACHE_DIR = new File(
        System.getProperty('TERRAFORM_CACHE_DIR') ?: './build/terraform-binaries',
        'terraform'
    ).absoluteFile
    static final File RESOURCES_DIR = new File(System.getProperty('RESOURCES_DIR') ?: './src/downloadTest/resources')

    static final OperatingSystem OS = OperatingSystem.current()
    static final boolean SKIP_TESTS = !(OS.macOsX || OS.linux || OS.windows || OS.freeBSD)

    void setup() {
        System.setProperty('org.ysb33r.gradle.terraform.uri', TERRAFORM_CACHE_DIR.toURI().toString())
    }
}