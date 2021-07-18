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
package org.ysb33r.gradle.terraform.internal.remotestate;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtensionContainer;
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet;
import org.ysb33r.gradle.terraform.remotestate.BackendSpec;
import org.ysb33r.gradle.terraform.remotestate.TerraformRemoteStateExtension;
import org.ysb33r.grolifant.api.core.ProjectOperations;

/**
 * Utilities for working with backends.
 *
 * @author Schalk W. Cronjé
 * @since 0.12
 */
public class BackendFactory {

    /**
     * Attaches a backend to an instance of {@link TerraformRemoteStateExtension}.
     *
     * @param projectOperations ProjectOperations to attach.
     * @param objectFactory     ObjectFactory to use.
     * @param remoteState       Instance of {@link TerraformRemoteStateExtension}.
     * @param name              Name of backend.
     * @param backend           Class of backend
     * @param <T>               Class of backend.
     * @return Reference to global extension.
     */
    public static <T extends BackendSpec> T createBackend(
            ProjectOperations projectOperations,
            ObjectFactory objectFactory,
            TerraformRemoteStateExtension remoteState,
            String name,
            Class<T> backend
    ) {
        return ((ExtensionAware) remoteState).getExtensions().create(name, backend, projectOperations, objectFactory);
    }

    /**
     * Creates an instance of a backend on a {@link TerraformSourceDirectorySet}.
     *
     * @param projectOperations ProjectOperations to attach
     * @param objectFactory     ObjectFactory to use.
     * @param tsds              Terraform source directory set.
     * @param name              Name of backend.
     * @param backend           Implementation class of backend.
     * @param <T>               Class of backend.
     * @return Reference to the instantiated backend.
     */
    public static <T extends BackendSpec> T createBackend(
            ProjectOperations projectOperations,
            ObjectFactory objectFactory,
            TerraformSourceDirectorySet tsds,
            String name,
            Class<T> backend
    ) {
        ExtensionContainer extensionsOnSourceSet = ((ExtensionAware) tsds).getExtensions();
        TerraformRemoteStateExtension trse = extensionsOnSourceSet.getByType(TerraformRemoteStateExtension.class);
        ExtensionContainer extensionsOnBackends = ((ExtensionAware) trse).getExtensions();
        return extensionsOnBackends.create(name, backend, projectOperations, objectFactory);
    }
}
