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
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet
import org.ysb33r.gradle.terraform.internal.remotestate.TextTemplates
import org.ysb33r.gradle.terraform.remotestate.RemoteStateS3Spec
import org.ysb33r.gradle.terraform.remotestate.TerraformBackendExtension
import org.ysb33r.gradle.terraform.remotestate.TerraformRemoteStateExtension
import org.ysb33r.grolifant.api.core.ProjectOperations

/**
 * Adds {@link RemoteStateS3Spec} backends to the global {@link TerraformBackendExtension} and to each of the same
 * extensions on each {@link TerraformSourceDirectorySet}.
 *
 * Sets a remote state name ({@code key} in terraform speak) on each source set's extension.
 *
 * Tells the global {@link TerraformBackendExtension} to use {@link RemoteStateS3Spec} as its backend provider.
 */
@CompileStatic
class TerraformRemoteStateAwsS3Plugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.pluginManager.apply(TerraformPlugin)
        ProjectOperations.maybeCreateExtension(project)
        def globalRemote = TerraformRemoteStateExtension.findExtension(project)
        TerraformBackendExtension.find(project).addBackend(RemoteStateS3Spec.NAME, RemoteStateS3Spec)
        globalRemote.backend = RemoteStateS3Spec
        ((ExtensionAware) globalRemote).extensions.getByType(RemoteStateS3Spec).textTemplate =
            TextTemplates.LegacyS3ReplaceTokens.INSTANCE

        project.extensions.getByType(NamedDomainObjectContainer<TerraformSourceDirectorySet>).configureEach({
            TerraformSourceDirectorySet tsds ->
            TerraformRemoteStateExtension trse = ((ExtensionAware) tsds).extensions
                .getByType(TerraformRemoteStateExtension)
            trse.follow(globalRemote)
            RemoteStateS3Spec s3 = ((ExtensionAware) trse).extensions.getByType(RemoteStateS3Spec)
            s3.remoteStateName = trse.prefix.map {
                "${it}-${tsds.name}.tfstate".toString()
            }
        } as Action<TerraformSourceDirectorySet>)
    }
}
