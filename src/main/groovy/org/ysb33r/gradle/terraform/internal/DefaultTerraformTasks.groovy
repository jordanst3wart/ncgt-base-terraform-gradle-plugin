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
package org.ysb33r.gradle.terraform.internal

import groovy.transform.CompileStatic
import org.ysb33r.gradle.terraform.tasks.TerraformApply
import org.ysb33r.gradle.terraform.tasks.TerraformDestroy
import org.ysb33r.gradle.terraform.tasks.TerraformImport
import org.ysb33r.gradle.terraform.tasks.TerraformInit
import org.ysb33r.gradle.terraform.tasks.TerraformOutput
import org.ysb33r.gradle.terraform.tasks.TerraformPlan
import org.ysb33r.gradle.terraform.tasks.TerraformPlanProvider
import org.ysb33r.gradle.terraform.tasks.TerraformShowState
import org.ysb33r.gradle.terraform.tasks.TerraformStateMv
import org.ysb33r.gradle.terraform.tasks.TerraformStatePush
import org.ysb33r.gradle.terraform.tasks.TerraformStateRm
import org.ysb33r.gradle.terraform.tasks.TerraformUpgrade

import org.ysb33r.gradle.terraform.tasks.TerraformValidate

/** Maps terraform tasks to conventions.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.8.0
*/
@CompileStatic
enum DefaultTerraformTasks {
    INIT(0, 'init', TerraformInit, 'Initialises Terraform'),
    IMPORT(1, 'import', TerraformImport, 'Imports a resource'),
    SHOW(2, 'showState', TerraformShowState, 'Generates a report on the current state'),
    OUTPUT(2, 'output', TerraformOutput, 'Generates a file of output variables'),
    PLAN(10, 'plan', TerraformPlan, 'Generates Terraform execution plan'),
    APPLY(11, 'apply', TerraformApply, 'Builds or changes infrastructure', TerraformPlanProvider),
    DESTROY(12, 'destroy', TerraformDestroy, 'Destroys infrastructure', TerraformPlanProvider),
    VALIDATE(20, 'validate', TerraformValidate, 'Validates the Terraform configuration'),
    STATE_MV(30, 'stateMv', TerraformStateMv, 'Moves a resource from one area to another'),
    STATE_PUSH(31, 'statePush', TerraformStatePush, 'Pushes local state file to remote'),
    STATE_RM(32, 'stateRm', TerraformStateRm, 'Removes a resource from state'),
    UPGRADE(40,'upgrade', TerraformUpgrade,'Upgrades Terraform source to current version')

    static List<DefaultTerraformTasks> ordered() {
        DefaultTerraformTasks.values().sort { a, b -> a.order <=> b.order } as List
    }

    final int order
    final String command
    final Class type
    final String description
    final Class dependsOnProvider

    private DefaultTerraformTasks(int order, String name, Class type, String description, Class dependsOn = null) {
        this.order = order
        this.command = name
        this.type = type
        this.description = description
        this.dependsOnProvider = dependsOn
    }
}