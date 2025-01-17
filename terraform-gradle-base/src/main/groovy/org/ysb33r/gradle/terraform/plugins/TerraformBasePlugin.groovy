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
package org.ysb33r.gradle.terraform.plugins

import groovy.transform.CompileStatic
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet
import org.ysb33r.gradle.terraform.remotestate.LocalBackendSpec
import org.ysb33r.gradle.terraform.remotestate.TerraformBackendExtension
import org.ysb33r.gradle.terraform.remotestate.TerraformRemoteStateExtension
import org.ysb33r.gradle.terraform.tasks.AbstractTerraformTask
import org.ysb33r.gradle.terraform.tasks.RemoteStateConfigGenerator
import org.ysb33r.grolifant.api.core.ProjectOperations

import static org.ysb33r.gradle.terraform.internal.TerraformConfigUtils.locateTerraformRCExtension
import static org.ysb33r.gradle.terraform.internal.TerraformConfigUtils.locateTerraformRCGenerator
import static org.ysb33r.gradle.terraform.internal.TerraformConvention.sourceSetDisplayName

/** Provide the basic capabilities for dealing with Terraform tasks. Allow for downloading & caching of
 * Terraform distributions on a variety of the most common development platforms.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformBasePlugin implements Plugin<Project> {
    public static final String TERRAFORM_SOURCESETS = 'terraformSourceSets'
    public static final String TERRAFORM_TASK_GROUP = 'Terraform'
    public static final String REMOTE_STATE_VARIABLE = 'remote_state'

    static TerraformRemoteStateExtension addRemoteStateExtension(Project project, ExtensionAware instance) {
        instance.extensions.create(TerraformRemoteStateExtension.NAME, TerraformRemoteStateExtension, project)
    }

    void apply(Project project) {
        if (project == project.rootProject) {
            project.apply plugin: TerraformRCPlugin
        }

        ProjectOperations projectOperations = ProjectOperations.maybeCreateExtension(project)

        project.tasks.withType(RemoteStateConfigGenerator) { RemoteStateConfigGenerator t ->
            t.dependsOn(locateTerraformRCGenerator(t.project))
        }

        project.tasks.withType(AbstractTerraformTask) { AbstractTerraformTask t ->
            t.dependsOn(locateTerraformRCGenerator(t.project))
        }

        def terraform = createGlobalTerraformExtension(project)
        def remoteState = createGlobalRemoteStateExtension(project, terraform)
        def tss = createTerraformSourceSetsExtension(project)
        createTerraformBackendsExtension(project, projectOperations, remoteState, tss)
    }

    private static TerraformExtension createGlobalTerraformExtension(Project project) {
        project.extensions.create(TerraformExtension.NAME, TerraformExtension, project)
    }

    private static TerraformRemoteStateExtension createGlobalRemoteStateExtension(
        Project project,
        TerraformExtension terraform
    ) {
        addRemoteStateExtension(project, ((ExtensionAware) terraform))
    }

    private static NamedDomainObjectContainer<TerraformSourceDirectorySet> createTerraformSourceSetsExtension(
        Project project
    ) {
        def objectFactory = project.objects
        def terraformRc = locateTerraformRCExtension(project)
        NamedDomainObjectFactory<TerraformSourceDirectorySet> factory = { String name ->
            objectFactory.newInstance(
                TerraformSourceDirectorySet,
                project,
                project.objects,
                project.tasks,
                terraformRc,
                name,
                sourceSetDisplayName(name)
            )
        }
        NamedDomainObjectContainer<TerraformSourceDirectorySet> sourceSetContainer =
            objectFactory.domainObjectContainer(TerraformSourceDirectorySet, factory)
        project.extensions.add(TERRAFORM_SOURCESETS, sourceSetContainer)
        sourceSetContainer
    }

    @SuppressWarnings('UnnecessarySetter')
    private static TerraformBackendExtension createTerraformBackendsExtension(
        Project project,
        ProjectOperations projectOperations,
        TerraformRemoteStateExtension globalRemoteState,
        NamedDomainObjectContainer<TerraformSourceDirectorySet> tss
    ) {
        def backend = project.extensions.create(
            TerraformBackendExtension.NAME,
            TerraformBackendExtension,
            projectOperations,
            project.objects,
            globalRemoteState,
            tss
        )

        globalRemoteState.setBackend(LocalBackendSpec)
        backend
    }
}
