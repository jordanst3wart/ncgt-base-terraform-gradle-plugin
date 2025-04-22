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
package org.ysb33r.gradle.terraform

import groovy.transform.CompileStatic
import org.ysb33r.grolifant.api.core.Version

/**
 * Major version groupings for Terraform
 *
 * @author Schalk W. Cronjé
 *
 */
@CompileStatic
enum TerraformMajorVersion {
    VERSION_11_OR_OLDER,
    VERSION_12,
    VERSION_13,
    VERSION_14,
    VERSION_15,
    VERSION_100,
    VERSION_110,
    VERSION_120,
    VERSION_130,
    VERSION_140,
    VERSION_150,
    VERSION_160,
    VERSION_170,
    VERSION_180,
    VERSION_190,
    UNKNOWN

    static TerraformMajorVersion version(String version) {
        Version ver = Version.of(version)
        if (ver.major == 0) {
            if (ver.minor < 12) {
                VERSION_11_OR_OLDER
            } else {
                try {
                    valueOf("VERSION_${ver.minor}")
                } catch (IllegalArgumentException e) {
                    UNKNOWN
                }
            }
        } else if (ver.major == 1) {
            try {
                valueOf("VERSION_1${ver.minor}0")
            } catch (IllegalArgumentException e) {
                UNKNOWN
            }
        } else {
            UNKNOWN
        }
    }
}