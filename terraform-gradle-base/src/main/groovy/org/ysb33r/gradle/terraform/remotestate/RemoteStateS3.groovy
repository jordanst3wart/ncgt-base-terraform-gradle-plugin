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
package org.ysb33r.gradle.terraform.remotestate

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware

/** An extension that is added to {@link TerraformRemoteStateExtension}.
 *
 * @author Schalk W. Cronjé
 *
 * @deprecated Use RemoteStateS3Spec instead.
 *
 * @since 0.8.0
 */
@CompileStatic
@Deprecated
class RemoteStateS3 {

    /** Utility method to find this extension on a project.
     *
     * This will return the project's {@link RemoteStateS3Spec} instead.
     *
     * @param project Project context
     * @return Extension after it has been attached.
     */
    static RemoteStateS3Spec findExtension(Project project) {
        ((ExtensionAware) TerraformRemoteStateExtension.findExtension(project)).extensions.getByType(RemoteStateS3Spec)
    }

    /**
     * Utility to find this extension on a terraform source set.
     *
     * This will return the source set's {@link RemoteStateS3Spec} instead
     *
     * @param project Project context
     * @param sourceSetName Name of source set.
     * @return Extension after it has been attached.
     *
     * @since 0.10.0
     */
    static RemoteStateS3Spec findExtension(Project project, String sourceSetName) {
        def remote = TerraformRemoteStateExtension.findExtension(project, sourceSetName)
        ((ExtensionAware) remote).extensions.getByType(RemoteStateS3Spec)
    }
}
