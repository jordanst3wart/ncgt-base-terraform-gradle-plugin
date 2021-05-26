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
package org.ysb33r.gradle.terraform.aws

import groovy.transform.CompileStatic

import static org.ysb33r.grolifant.api.v4.StringUtils.stringize

/**
 * Specification for an assumed AWS role.
 *
 * @author Schalk W. Cronjé
 *
 * @since 1.1
 */
@CompileStatic
class AssumedRoleSpec {

    void setRoleArn(Object arn) {
        this.roleArn = arn
    }

    String getRoleArn() {
        stringize(this.roleArn)
    }

    void setSessionName(Object name) {
        this.sessionName = name
    }

    String getSessionName() {
        stringize(this.sessionName)
    }

    void setRegion(Object region) {
        this.region = region
    }

    String getRegion() {
        stringize(this.region)
    }

    Integer durationSeconds = 15 * 60

    private Object roleArn
    private Object sessionName
    private Object region
}
