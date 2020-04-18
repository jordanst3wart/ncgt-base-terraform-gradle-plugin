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
package org.ysb33r.gradle.terraform.remotestate

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.ysb33r.grolifant.api.StringUtils

import java.util.concurrent.Callable

/** An extension that is added to {@link TerraformRemoteStateExtension}.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.8.0
 */
@CompileStatic
class RemoteStateS3 {
    public static final String NAME = 's3'

    /** Utility method to find this extension.
     *
     * @param project Project context
     * @return Extension after it has been attached.
     */
    static RemoteStateS3 findExtension(Project project) {
        ((ExtensionAware)TerraformRemoteStateExtension.findExtension(project)).extensions.getByType(RemoteStateS3)
    }

    RemoteStateS3(Project project) {
        this.project = project
        this.region = project.objects.property(String)
        this.bucket = project.objects.property(String)
    }

    /** Set the default AWS region where the remote state will be stored.
     *
     * @param p Object that can be converted to a string. Can be a {@code Provider} as well.
     */
    void setRegion(Object p) {
        this.region.set(project.provider({
            StringUtils.stringize(p)
        } as Callable<String>))
    }

    /** The default AWS region where remote state will be stored.
     *
     */
    Provider<String> getRegion() {
        this.region
    }

    /** Set the default AWS bucket where the remote state will be stored.
     *
     * @param p Object that can be converted to a string. Can be a {@code Provider} as well.
     */
    void setBucket(Object p) {
        this.bucket.set(project.provider({
            StringUtils.stringize(p)
        } as Callable<String>))
    }

    /** The default AWS bucket where remote state will be stored.
     *
     */
    Provider<String> getBucket() {
        this.bucket
    }

    private final Project project
    private final Property<String> region
    private final Property<String> bucket
}
