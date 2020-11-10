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
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.TerraformSourceSets
import org.ysb33r.grolifant.api.v4.StringUtils

import java.util.concurrent.Callable

/** Extension that is added to the project {@link TerraformExtension}
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.8.0
 */
@CompileStatic
class TerraformRemoteStateExtension {

    public static final String NAME = 'remote'

    /** A utility to locate the extension.
     *
     * @param project Project context
     * @return {@link TerraformRemoteStateExtension} if it has been added.
     */
    static TerraformRemoteStateExtension findExtension(Project project) {
        ((ExtensionAware) project.extensions.getByType(TerraformExtension))
            .extensions.getByType(TerraformRemoteStateExtension)
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
    static TerraformRemoteStateExtension findExtension(Project project, String sourceSetName) {
        def sourceSet = project.extensions.getByType(TerraformSourceSets).getByName(sourceSetName)
        ((ExtensionAware)sourceSet).extensions.getByType(TerraformRemoteStateExtension)
    }

    TerraformRemoteStateExtension(Project project) {
        this.project = project
        this.prefix = project.objects.property(String)
        setPrefix(project.name)
    }

    /** Assign the prefix.
     *
     * @param p Object that can be converted to a string. Can be a {@code Provider} as well.
     */
    void setPrefix(Object p) {
        this.prefix.set(project.provider({
            StringUtils.stringize(p)
        } as Callable<String>))
    }

    /** A prefix that is added to remote state names.
     *
     */
    Provider<String> getPrefix() {
        this.prefix
    }

    /**
     * Follows the settings of another remote state extension.
     *
     * @param other Instance of {@link TerraformRemoteStateExtension} to follow.
     */
    void follow(TerraformRemoteStateExtension other) {
        setPrefix(other.prefix)
    }

    private final Project project
    private final Property<String> prefix
}
