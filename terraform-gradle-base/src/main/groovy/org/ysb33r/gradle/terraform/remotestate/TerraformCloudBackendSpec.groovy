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
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.ysb33r.grolifant.api.core.ProjectOperations

/**
 * Support the Terraform Cloud bakend a.k.a {@code remote}.
 *
 * In the DSL it is reeferred to as {@code terraformCloud}.
 *
 * @since 0.13
 *
 * @author Schalk W. Cronjé
 */
@CompileStatic
class TerraformCloudBackendSpec extends AbstractBackendSpec {

    public static final String NAME = 'terraformCloud'

    /** Utility method to find this extension on a project.
     *
     * @param project Project context
     * @return Extension after it has been attached.
     *
     * @since 0.13
     */
    static TerraformCloudBackendSpec findExtension(Project project) {
        ((ExtensionAware) TerraformRemoteStateExtension.findExtension(project))
            .extensions.getByType(TerraformCloudBackendSpec)
    }

    /**
     * Utility to find this extension on a terraform source set.
     *
     * @param project Project context
     * @param sourceSetName Name of source set.
     * @return Extension after it has been attached.
     *
     * @since 0.13
     */
    static TerraformCloudBackendSpec findExtension(Project project, String sourceSetName) {
        def remote = TerraformRemoteStateExtension.findExtension(project, sourceSetName)
        ((ExtensionAware) remote).extensions.getByType(TerraformCloudBackendSpec)
    }

    TerraformCloudBackendSpec(ProjectOperations po, ObjectFactory objects) {
        super(po, objects)
    }

    final String name = NAME
    final String defaultTextTemplate = ''

    /** Sets a hostname if other than {@code app.terraform.io}.
     *
     * @param hostname
     */
    void setHostname(Object hostname) {
        token('hostname', hostname)
    }

    /**
     * Set organization name.
     *
     * @param org Organization name.
     */
    void setOrganization(Object org) {
        token('organization', org)
    }

    /**
     * Set authentication token.
     *
     * As an alternative the {@link org.ysb33r.gradle.terraform.TerraformRCExtension#credentials} method can be used
     * to set credentials in the configuration file.
     *
     *
     * @param tok
     */
    void setAuthToken(Object tok) {
        token('token', tok)
    }

    /**
     * The full name of one remote workspace.
     *
     * Overrides {@link #setWorkspacePrefix}.
     *
     * @param wsName Workspace name.
     */
    void setWorkspaceName(Object wsName) {
        token(WORKSPACES, [name: wsName])
    }

    /**
     * A prefix used in the names of one or more remote workspaces, all of which can be used with this configuration.
     *
     * Overrides {@link #setWorkspaceName}.
     *
     * @param prefix Workspace prefix
     */
    void setWorkspacePrefix(Object prefix) {
        token(WORKSPACES, [prefix: prefix])
    }

    private static final String WORKSPACES = 'workspaces'
}
