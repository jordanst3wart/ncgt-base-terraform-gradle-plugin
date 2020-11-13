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
import org.ysb33r.grolifant.api.core.ProjectOperations
import org.ysb33r.grolifant.api.core.LegacyLevel

import static org.ysb33r.gradle.terraform.internal.TerraformConfigUtils.locateTerraformRCExtension
import static org.ysb33r.gradle.terraform.internal.TerraformConvention.sourceSetDisplayName

@CompileStatic
class TerraformSourceSets implements NamedDomainObjectContainer<TerraformSourceDirectorySet> {

    TerraformSourceSets(Project project) {
        this.projectOperations = ProjectOperations.create(project)
        def terraformrc = locateTerraformRCExtension(project)
        def objects = project.objects
        def tasks = project.tasks
        NamedDomainObjectFactory<TerraformSourceDirectorySet> factory = { String name ->
            objects.newInstance(
                TerraformSourceDirectorySet,
                project,
                objects,
                tasks,
                terraformrc,
                name,
                sourceSetDisplayName(name)
            )
        }

        sourceDirectorySets = LegacyLevel.PRE_5_5 ?
            createContainerLegacyMode(factory, project) :
            createContainer(factory, project)
    }

    @CompileDynamic
    private static NamedDomainObjectContainer<TerraformSourceDirectorySet> createContainer(
        NamedDomainObjectFactory factory,
        Project project
    ) {
        project.objects.domainObjectContainer(TerraformSourceDirectorySet, factory)
    }

    @CompileDynamic
    private static NamedDomainObjectContainer<TerraformSourceDirectorySet> createContainerLegacyMode(
        NamedDomainObjectFactory factory,
        Project project
    ) {
        project.container(TerraformSourceDirectorySet, factory)
    }

    private final ProjectOperations projectOperations

    @Delegate(interfaces = true)
    private final NamedDomainObjectContainer<TerraformSourceDirectorySet> sourceDirectorySets
}
