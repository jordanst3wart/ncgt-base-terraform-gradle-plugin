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
package org.ysb33r.gradle.terraform.config;

import groovy.lang.Closure;

import java.util.Collections;
import java.util.List;

/**
 * An extension that can be added to a task for representing a specific grouping of Terraform
 * command-line parameters.
 *
 * @author Schalk W. Cronjé
 */
public interface TerraformTaskConfigExtension {
    /**
     * The name under which the extension should be created.
     *
     * @return Name of the extension
     */
    String getName();

    /**
     * Returns a list of closures which can be used to determine an input property for the purposes of
     * up to date calculations.
     * <p>
     * Closures should return objects that are serializable.
     *
     * @return Property closures. Can be empty (but never {@code null}) which means that the extension holds no
     * properties that should be used fot up to date calculations.
     */
    List<Closure> getInputProperties();

    /**
     * Returns the list of Terraform command-line arguments.
     *
     * @return List of arguments to be added. Can be empty, but never {@code null}
     */
    List<String> getCommandLineArgs();

    /**
     * Returns the list of Terraform variables in the form name=value
     *
     * @return Terraform variables
     *
     * @since 0.13
     */
    default List<String> getTfVars() {
        return Collections.EMPTY_LIST;
    }
}
