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
package org.ysb33r.gradle.terraform

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory

import static org.gradle.util.GradleVersion.current
import static org.gradle.util.GradleVersion.version
import static org.ysb33r.gradle.terraform.internal.TerraformConvention.sourceSetDisplayName

@CompileStatic
class TerraformSourceSets implements NamedDomainObjectContainer<TerraformSourceDirectorySet> {

    TerraformSourceSets(Project project) {
        boolean legacyMode = current() < version('5.5')
        this.project = project
        NamedDomainObjectFactory<TerraformSourceDirectorySet> factory = { ObjectFactory objects, String name ->
            objects.newInstance(
            TerraformSourceDirectorySet,
                project,
                name,
                sourceSetDisplayName(name)
            )
        }.curry(project.objects) as NamedDomainObjectFactory<TerraformSourceDirectorySet>

        sourceDirectorySets = legacyMode ? createContainerLegacyMode(factory) : createContainer(factory)
    }

    @CompileDynamic
    private NamedDomainObjectContainer<TerraformSourceDirectorySet> createContainer(
        NamedDomainObjectFactory factory
    ) {
        project.objects.domainObjectContainer(TerraformSourceDirectorySet, factory)
    }

    @CompileDynamic
    private NamedDomainObjectContainer<TerraformSourceDirectorySet> createContainerLegacyMode(
        NamedDomainObjectFactory factory
    ) {
        project.container(TerraformSourceDirectorySet, factory)
    }

    private final Project project

    @Delegate(interfaces = true)
    private final NamedDomainObjectContainer<TerraformSourceDirectorySet> sourceDirectorySets
}
