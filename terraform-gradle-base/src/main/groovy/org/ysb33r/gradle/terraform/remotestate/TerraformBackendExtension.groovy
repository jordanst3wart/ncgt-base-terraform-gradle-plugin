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
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet
import org.ysb33r.grolifant.api.core.ProjectOperations

import static org.ysb33r.gradle.terraform.internal.remotestate.BackendFactory.createBackend

@CompileStatic
class TerraformBackendExtension {
    public static final String NAME = 'terraformBackends'

    static TerraformBackendExtension find(Project project) {
        project.extensions.getByType(TerraformBackendExtension)
    }

    TerraformBackendExtension(
        ProjectOperations projectOperations,
        ObjectFactory objectFactory,
        TerraformRemoteStateExtension globalRemote,
        NamedDomainObjectContainer<TerraformSourceDirectorySet> terraformSourceSets
    ) {
        this.projectOperations = projectOperations
        this.objectFactory = objectFactory
        this.terraformSourceSets = terraformSourceSets
        this.globalRemote = globalRemote

        createBackend(projectOperations, objectFactory, globalRemote, LocalBackendSpec.NAME, LocalBackendSpec)
    }

    /**
     * Adds the backend to the project's {@link TerraformRemoteStateExtension} and to
     * every {@org.ysb33r.gradle.terraform.TerraformSourceDirectorySet}'s remote extension.
     *
     * @param name Name of backend.
     * @param backend Class of backend.
     */
    def <T extends BackendSpec> void addBackend(String name, Class<T> backend) {
        addBackend(name, backend) { it -> }
    }

    /**
     * Adds the backend to the project's {@link TerraformRemoteStateExtension} and to
     * every {@link TerraformSourceDirectorySet}'s remote extension.
     *
     * @param name Name of backend.
     * @param backend Class of backend.
     * @param configurator Configurator for backend.
     */
    def <T extends BackendSpec> void addBackend(
        String name,
        Class<T> backend,
        Action<T> configurator
    ) {
        T globalExt = ((ExtensionAware) globalRemote).extensions.create(name, backend, this.projectOperations)
        configurator.execute(globalExt)
        ObjectFactory objects = objectFactory
        terraformSourceSets.configureEach { TerraformSourceDirectorySet tsds ->
            T localExt = createBackend(this.projectOperations, objects, tsds, name, backend)
            configurator.execute(localExt)
        }
    }

    /**
     * Adds the backend to the project's {@link TerraformRemoteStateExtension} and to
     * every {@link TerraformSourceDirectorySet}'s remote extension.
     *
     * @param name Name of backend.
     * @param backend Class of backend.
     * @param configurator Configurator for backend.
     */
    @CompileStatic
    <T extends BackendSpec> void addBackend(
        String name,
        Class<T> backend,
        Closure configurator
    ) {
        createBackend(projectOperations, objectFactory, globalRemote, name, backend).configure(configurator)
        terraformSourceSets.configureEach(new Action<TerraformSourceDirectorySet>() {
            @Override
            void execute(TerraformSourceDirectorySet tsds) {
                createBackend(projectOperations, objectFactory, tsds, name, backend).configure(configurator)
            }
        })
    }

    private final ObjectFactory objectFactory
    private final ProjectOperations projectOperations
    private final TerraformRemoteStateExtension globalRemote
    private final NamedDomainObjectContainer<TerraformSourceDirectorySet> terraformSourceSets
}
