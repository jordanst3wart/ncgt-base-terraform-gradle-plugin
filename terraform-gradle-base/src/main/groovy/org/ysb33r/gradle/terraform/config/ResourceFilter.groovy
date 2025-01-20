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
package org.ysb33r.gradle.terraform.config

import groovy.transform.CompileStatic
import org.ysb33r.gradle.terraform.internal.Transform

/** Allows for lock configurations on a task.
 *
 * @since 0.1
 */
@CompileStatic
class ResourceFilter implements TerraformTaskConfigExtension {
    final String name = 'resources'

    @Override
    @SuppressWarnings('UnnecessaryCast')
    List<Closure> getInputProperties() {
        [{ -> this.targets }] as List<Closure>
    }

    void setTargets(Iterable<String> targets) {
        this.targets.clear()
        this.targets.addAll(targets)
    }

    void target(String... targets) {
        this.targets.addAll(targets)
    }

    void target(Iterable<String> targets) {
        this.targets.addAll(targets)
    }

    void setReplacements(Iterable<String> targets) {
        this.replacements.clear()
        this.replacements.addAll(targets)
    }

    void replace(String... targets) {
        this.replacements.addAll(targets)
    }

    void replace(Iterable<String> targets) {
        this.replacements.addAll(targets)
    }

    @Override
    List<String> getCommandLineArgs() {
        [Transform.toList((Collection) targets) {
            "-target=${it}".toString()
        }, Transform.toList((Collection) replacements) {
            "-replace=${it}".toString()
        }].flatten() as List<String>
    }

    private final List<String> targets = []
    private final List<String> replacements = []
}
