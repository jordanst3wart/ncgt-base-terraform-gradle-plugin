/*
 * Copyright 2017-2019 the original author or authors.
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
package org.ysb33r.gradle.terraform.config.multilevel

import groovy.transform.CompileStatic
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.config.TerraformTaskConfigExtension
import org.ysb33r.gradle.terraform.config.VariablesSpec

import java.util.function.Function

/** Describes configuration types that can be added to Terraform extensions and to source sets.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.2
 */
@CompileStatic
enum TerraformExtensionConfigTypes {

    VARIABLES(Variables, VariablesSpec, 'variables', { TerraformExtension te -> te.allVariables })

    private TerraformExtensionConfigTypes(
        Class clazz,
        Class publicType,
        String name,
        Closure<TerraformTaskConfigExtension> accessor
    ) {
        this.type = clazz
        this.publicType = publicType
        this.name = name
        this.accessor = accessor as Function<TerraformExtension, TerraformTaskConfigExtension>
    }

    final Class type
    final Class publicType
    final String name

    /**
     * The function that will return an instance of the configuration extension from the
     * terraform task extension.
     */
    final Function<TerraformExtension, TerraformTaskConfigExtension> accessor
}