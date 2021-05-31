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
package org.ysb33r.gradle.terraform.tasks

import groovy.transform.CompileStatic
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet

import static org.ysb33r.gradle.terraform.internal.TerraformConvention.DEFAULT_WORKSPACE

/** The {@code terraform workspace delete} command.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.10.0
 */
@CompileStatic
class TerraformCleanupWorkspaces extends AbstractTerraformTask {

    TerraformCleanupWorkspaces() {
        super('workspace', [], [], null)
        alwaysOutOfDate()
        doesNotRequireSessionCredentials()
    }

    @Option(option = 'force', description = 'Forces local removal of workspaces')
    @Internal
    boolean forceRemoval = false

    @Override
    @SuppressWarnings('UnnecessaryGetter')
    void exec() {
        TerraformSourceDirectorySet tsds = getSourceSet()
        List<String> expectedWorkspaces = tsds ? tsds.workspaceNames : []
        def currentWorkspaces = listWorkspaces()
        def currentWorkspace = currentWorkspaces.find { k, v -> v }.key
        Set<String> removeWorkspaces = currentWorkspaces.keySet().findAll { it != DEFAULT_WORKSPACE }
        removeWorkspaces.removeAll(expectedWorkspaces)
        logger.info "Required additional workspaces are: ${expectedWorkspaces}"
        if (removeWorkspaces.empty) {
            logger.info 'Found no workspaces to remove'
        } else {
            logger.info "Will remove old workspaces: ${removeWorkspaces.join(', ')}"

            if (removeWorkspaces.contains(currentWorkspace)) {
                logger.info "Switching to '${DEFAULT_WORKSPACE}' workspace first"
                runWorkspaceSubcommand('select', DEFAULT_WORKSPACE)
            }
        }
        removeWorkspaces.forEach { String ws ->
            if (forceRemoval) {
                runWorkspaceSubcommand(DELETE_CMD, '-force', ws)
            } else {
                runWorkspaceSubcommand(DELETE_CMD, ws)
            }
        }
    }

    private static final String DELETE_CMD = 'delete'
}
