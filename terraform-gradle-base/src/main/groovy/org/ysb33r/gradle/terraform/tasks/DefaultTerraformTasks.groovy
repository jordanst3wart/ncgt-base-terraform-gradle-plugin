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
package org.ysb33r.gradle.terraform.tasks

import groovy.transform.CompileStatic

/** Maps terraform tasks to conventions.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.8.0
 */
@CompileStatic
@SuppressWarnings('LineLength')
enum DefaultTerraformTasks {
    INIT('init', TerraformInit, 'Initialises Terraform'),
    IMPORT('import', TerraformImport, 'Imports a resource'),
    SHOW('showState', TerraformShowState, 'Generates a report on the current state'),
    OUTPUT('output', TerraformOutput, 'Generates a file of output variables'),
    PLAN('plan', TerraformPlan, 'Generates Terraform execution plan'),
    APPLY('apply', TerraformApply, 'Builds or changes infrastructure'),
    DESTROY_PLAN('destroyPlan', TerraformDestroyPlan, 'Generates Terraform destruction plan'),
    DESTROY('destroy', TerraformDestroy, 'Destroys infrastructure'),
    VALIDATE('validate', TerraformValidate, 'Validates the Terraform configuration'),
    STATE_MV('stateMv', TerraformStateMv, 'Moves a resource from one area to another'),
    STATE_PUSH('statePush', TerraformStatePush, 'Pushes local state file to remote'),
    STATE_PULL('statePull', TerraformStatePull, 'Pulls remote state local to local file'),
    STATE_RM('stateRm', TerraformStateRm, 'Removes a resource from state'),
    UNTAINT('untaint', TerraformUntaint, 'Remove tainted status from resource'),
    FMT_CHECK('fmtCheck', TerraformFmtCheck, 'Checks whether files are correctly formatted'),
    FMT_APPLY('fmtApply', TerraformFmtApply, 'Formats source files in source set')

    static List<DefaultTerraformTasks> tasks() {
        values() as List
    }

    final String command
    final Class type
    final String description

    @SuppressWarnings('ParameterCount')
    private DefaultTerraformTasks(
        String name,
        Class type,
        String description
    ) {
        this.command = name
        this.type = type
        this.description = description
    }
}