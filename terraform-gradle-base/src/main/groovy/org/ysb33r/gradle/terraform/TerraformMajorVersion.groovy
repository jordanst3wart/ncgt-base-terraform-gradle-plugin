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
package org.ysb33r.gradle.terraform

import groovy.transform.CompileStatic

/**
 * Major version groupings for Terraform
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.10.0
 */
@CompileStatic
enum TerraformMajorVersion {
    VERSION_11_OR_OLDER,
    VERSION_12,
    VERSION_13,
    VERSION_14,
    VERSION_15,
    UNKNOWN

    static TerraformMajorVersion fromMinor(int ver) {
        if (ver < 12) {
            VERSION_11_OR_OLDER
        } else {
            try {
                valueOf("VERSION_${ver}")
            } catch (IllegalArgumentException e) {
                UNKNOWN
            }
        }
    }
}