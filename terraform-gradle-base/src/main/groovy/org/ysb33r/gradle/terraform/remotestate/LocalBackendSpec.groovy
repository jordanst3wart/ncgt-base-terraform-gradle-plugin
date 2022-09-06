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
package org.ysb33r.gradle.terraform.remotestate

import groovy.transform.CompileStatic
import org.gradle.api.model.ObjectFactory
import org.ysb33r.grolifant.api.core.ProjectOperations

/**
 * Terraform local backend specification.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.12
 */
@CompileStatic
class LocalBackendSpec extends AbstractBackendSpec {

    public static final String NAME = 'local'

    LocalBackendSpec(ProjectOperations po, ObjectFactory objects) {
        super(po, objects)
    }

    final String name = NAME
    final String defaultTextTemplate = ''

    /** Sets a path where local state will be stored.
     *
     * @param p Path that can be evaluated to a file.
     */
    void setPath(Object p) {
        tokenPath('path', p)
    }
}
