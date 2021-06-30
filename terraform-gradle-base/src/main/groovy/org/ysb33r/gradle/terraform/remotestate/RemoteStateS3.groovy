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
import org.gradle.api.provider.Provider
import org.ysb33r.grolifant.api.core.ProjectOperations

import java.util.concurrent.Callable

/** An extension that is added to {@link TerraformRemoteStateExtension}.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.8.0
 */
@CompileStatic
class RemoteStateS3 extends RemoteStateS3Spec implements RemoteStateS3Provider {
    public static final String NAME = 's3'

    /** Utility method to find this extension on a project.
     *
     * @param project Project context
     * @return Extension after it has been attached.
     */
    static RemoteStateS3 findExtension(Project project) {
        ((ExtensionAware) TerraformRemoteStateExtension.findExtension(project)).extensions.getByType(RemoteStateS3)
    }

    /**
     * Utility to find this extension on a terraform source set.
     *
     * @param project Project context
     * @param sourceSetName Name of source set.
     * @return Extension after it has been attached.
     *
     * @since 0.10.0
     */
    static RemoteStateS3 findExtension(Project project, String sourceSetName) {
        def remote = TerraformRemoteStateExtension.findExtension(project, sourceSetName)
        ((ExtensionAware) remote).extensions.getByType(RemoteStateS3)
    }

    /**
     * Constructs a description of Terraform remote state stored in S3.
     *
     * @param project Project this is associated with.
     *   The object reference is not cached, so it is configuration-cache safe.
     */
    RemoteStateS3(Project project) {
        super(ProjectOperations.find(project))

        this.attributesProvider = project.provider(new Callable<Map<String, ?>>() {
            @Override
            Map<String, ?> call() throws Exception {
                if (following) {
                    Map<String, ?> combined = [:]
                    combined.putAll(following.attributesMap.get())
                    combined.putAll(tokenProvider.get())
                    combined
                } else {
                    tokenProvider.get()
                }
            }
        })
    }

    /**
     * Returns a provider to a map of all S3 backend attributes that could possible be configured.
     *
     * @return Map provider
     *
     * @since 1.0
     */
    @Override
    Provider<Map<String, ?>> getAttributesMap() {
        this.attributesProvider
    }

    /** Make settings follow that of another {@code RemoteStateS3} provider.
     *
     * Following a provider will apply those items first and then customise with any local settings.
     *
     * @param s3 Another S3 state provider.
     */
    void follow(RemoteStateS3Provider s3) {
        this.following = s3
    }

    private RemoteStateS3Provider following
    private final Provider<Map<String, ?>> attributesProvider
}
