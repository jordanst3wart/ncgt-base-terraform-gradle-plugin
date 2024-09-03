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
package org.ysb33r.gradle.terraform

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.provider.Provider
import org.gradle.process.ExecSpec
import org.ysb33r.grolifant.api.core.ProjectOperations

import java.util.concurrent.Callable

/**
 * Handles workspaces within a task.
 *
 * @since 1.0.3
 */
@CompileStatic
class WorkspaceExtension {
    public static final String NAME = 'workspaceControl'

    WorkspaceExtension(
        final Provider<TerraformSourceDirectorySet> tsdsProvider,
        final Provider<File> logDir,
        final Callable<TerraformExecSpec> execSpecProvider,
        final String workspaceName,
        final ProjectOperations projectOperations
    ) {
        this.workspaceName = workspaceName
        this.tsdsProvider = tsdsProvider
        this.logDir = logDir
        this.execSpecProvider = execSpecProvider
        this.projectOperations = projectOperations
    }

    /**
     * Switches workspaces to the correct one if the source set has workspaces and the current workspace is not the
     * correct one. If no additional workspace or the task is workspace agnostic, then it will do-nothing.
     */
    void switchWorkspace() {
        if (hasWorkspaces()) {
            def workspaces = listWorkspaces()
            String current = workspaces.find { k, v -> v == true }.key

            if (current != workspaceName) {
                if (workspaces.containsKey(workspaceName)) {
                    runWorkspaceSubcommand('select', workspaceName)
                } else {
                    runWorkspaceSubcommand('new', workspaceName)
                }
            }
        }
    }

    /**
     * Indicated whether this task is associated with a source set which has workspaces other than just default.
     *
     * @return {@code true} if there are workspaces.
     */
    @SuppressWarnings('UnnecessaryGetter')
    boolean hasWorkspaces() {
        TerraformSourceDirectorySet tsds = tsdsProvider.orNull
        tsds == null ? false : tsds.hasWorkspaces()
    }

    /**
     * Runs a {@code terraform workspace} subcommand.
     *
     * @param cmd Subcommand to run.
     * @return Output from command.
     *
     */
    String runWorkspaceSubcommand(String cmd, String... args) {
        final strm = new ByteArrayOutputStream()
        final execSpec = execSpecProvider.call()
        execSpec.command('workspace')
        execSpec.cmdArgs(cmd)
        execSpec.cmdArgs(args)
        execSpec.standardOutput(strm)
        Action<ExecSpec> runner = new Action<ExecSpec>() {
            @Override
            void execute(ExecSpec spec) {
                execSpec.copyToExecSpec(spec)
            }
        }
        logDir.get().mkdirs()
        projectOperations.exec(runner).assertNormalExitValue()
        strm.toString()
    }

    /**
     * Lists the workspaces as currently known to Terraform
     *
     * @return List of workspaces.
     */
    Map<String, Boolean> listWorkspaces() {
        runWorkspaceSubcommand('list').readLines().findAll {
            !it.empty
        }.collectEntries {
            if (it.startsWith('*')) {
                [it.substring(1).trim(), true]
            } else {
                [it.trim(), false]
            }
        }
    }

    private final String workspaceName
    private final Provider<TerraformSourceDirectorySet> tsdsProvider
    private final Provider<File> logDir
    private final Callable<TerraformExecSpec> execSpecProvider
    private final ProjectOperations projectOperations
}
