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
package org.ysb33r.gradle.terraform.internal

import groovy.transform.CompileStatic
@java.lang.SuppressWarnings('NoWildcardImports')
import org.ysb33r.gradle.terraform.tasks.*

/** Maps terraform tasks to conventions.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.8.0
 */
@CompileStatic
@SuppressWarnings('LineLength')
enum DefaultTerraformTasks {
    INIT(1, 'init', TerraformInit, 'Initialises Terraform', true),
    IMPORT(2, 'import', TerraformImport, 'Imports a resource'),
    SHOW(3, 'showState', TerraformShowState, 'Generates a report on the current state'),
    OUTPUT(4, 'output', TerraformOutput, 'Generates a file of output variables'),
    PLAN(10, 'plan', TerraformPlan, 'Generates Terraform execution plan'),
    APPLY(11, 'apply', TerraformApply, 'Builds or changes infrastructure', false),
    DESTROY_PLAN(14, 'destroyPlan', TerraformDestroyPlan, 'Generates Terraform destruction plan'),
    DESTROY(15, 'destroy', TerraformDestroy, 'Destroys infrastructure', false),
    VALIDATE(20, 'validate', TerraformValidate, 'Validates the Terraform configuration'),
    STATE_MV(30, 'stateMv', TerraformStateMv, 'Moves a resource from one area to another'),
    STATE_PUSH(31, 'statePush', TerraformStatePush, 'Pushes local state file to remote'),
    STATE_PULL(32, 'statePull', TerraformStatePull, 'Pulls remote state local to local file'),
    STATE_RM(33, 'stateRm', TerraformStateRm, 'Removes a resource from state'),
    UNTAINT(34, 'untaint', TerraformUntaint, 'Remove tainted status from resource'),
    FMT_CHECK(50, 'fmtCheck', TerraformFmtCheck, 'Checks whether files are correctly formatted', true),
    FMT_APPLY(51, 'fmtApply', TerraformFmtApply, 'Formats source files in source set', true),
    CLEANUP_WORKSPACES(60, 'cleanupWorkspaces', TerraformCleanupWorkspaces, 'Deletes any dangling workspaces', true)

    static List<DefaultTerraformTasks> ordered() {
        values().sort { a, b -> a.order <=> b.order } as List
    }

    /**
     * Find instance by command name
     *
     * @param cmd Command
     * @return Task metadata
     * @throw {@link IllegalArgumentException} is no match
     *
     * @since 0.10.0
     */
    static DefaultTerraformTasks byCommand(String cmd) {
        def task = values().find { cmd == it.command }
        if (!task) {
            throw new IllegalArgumentException("${cmd} is not a valid command alias for a Terraform task")
        }
        task
    }

    final int order
    final String command
    final Class type
    final String description
    final boolean workspaceAgnostic

    @SuppressWarnings('ParameterCount')
    private DefaultTerraformTasks(
        int order,
        String name,
        Class type,
        String description,
        boolean workspaceAgnostic = false
    ) {
        this.order = order
        this.command = name
        this.type = type
        this.description = description
        this.workspaceAgnostic = workspaceAgnostic
    }
}