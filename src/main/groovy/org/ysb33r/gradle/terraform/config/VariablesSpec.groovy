/*
 * Copyright 2017-2020 the original author or authors.
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
package org.ysb33r.gradle.terraform.config

import groovy.transform.CompileStatic

/**
 *
 * @since 0.2
 */
@CompileStatic
interface VariablesSpec {
    /** Adds one variable.
     *
     * <p> This will replace any previous entry by the same name.
     *
     * @param name Name of variable.
     * @param value Lazy-evaluated form of variable. Anything resolvable via
     * {@link org.ysb33r.grolifant.api.StringUtils#stringize(Object)}
     * is accepted.
     */
    void var(final String name, final Object value)

    /** Adds a map as a variable.
     *
     * <p> This will replace any previous entry by the same name.
     *
     * @param name Name of variable.
     * @param val1 First
     * @param vals Lazy-evaluated forms of variable.
     *  Anything resolvable via {@link org.ysb33r.grolifant.api.MapUtils#stringizeValues(Map < String, Object >)}
     * is accepted.
     */
    void map(Map<String, ?> map, final String name)

    /** Adds a list as a variable.
     *
     * <p> This will replace any previous entry by the same name.
     *
     * @param name Name of variable.
     * @param val1 First
     * @param vals Lazy-evaluated forms of variable. Anything resolvable via
     * {@link org.ysb33r.grolifant.api.StringUtils#stringize(Iterable <?>)} is accepted.
     */
    void list(final String name, Object val1, Object... vals)

    /** Adds a list as a variable.
     *
     * <p> This will replace any previous entry by the same name.
     *
     * @param name Name of variable.
     * @param vals Lazy-evaluated forms of variable. Anything resolvable via
     * {@link org.ysb33r.grolifant.api.StringUtils#stringize(Iterable <?>)} is accepted.
     */
    void list(final String name, Iterable<?> vals)

    /** Adds a name of a file containing {@code terraform} variables.
     *
     * @param fileName Files that can be converted via
     * {@link org.ysb33r.grolifant.api.StringUtils#stringize(Object o)} and resolved relative to the appropriate
     * {@link org.ysb33r.gradle.terraform.TerraformSourceDirectorySet}.
     */
    void file(final Object fileName)
}