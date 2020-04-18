package org.ysb33r.gradle.terraform.internal

import groovy.transform.CompileStatic
import org.ysb33r.gradle.terraform.tasks.TerraformApply
import org.ysb33r.gradle.terraform.tasks.TerraformDestroy
import org.ysb33r.gradle.terraform.tasks.TerraformImport
import org.ysb33r.gradle.terraform.tasks.TerraformInit
import org.ysb33r.gradle.terraform.tasks.TerraformPlan
import org.ysb33r.gradle.terraform.tasks.TerraformPlanProvider
import org.ysb33r.gradle.terraform.tasks.TerraformShowState
import org.ysb33r.gradle.terraform.tasks.TerraformStateMv
import org.ysb33r.gradle.terraform.tasks.TerraformStatePush
import org.ysb33r.gradle.terraform.tasks.TerraformStateRm
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
    PLAN(10, 'plan', TerraformPlan, 'Generates Terraform execution plan'),
    APPLY(11, 'apply', TerraformApply, 'Builds or changes infrastructure', TerraformPlanProvider),
    DESTROY(12, 'destroy', TerraformDestroy, 'Destroys infrastructure', TerraformPlanProvider),
    VALIDATE(20, 'validate', TerraformValidate, 'Validates the Terraform configuration'),
    STATE_MV(30, 'stateMv', TerraformStateMv, 'Moves a resource from one area to another'),
    STATE_PUSH(31, 'statePush', TerraformStatePush, 'Pushes local state file to remote'),
    STATE_RM(32, 'stateRm', TerraformStateRm, 'Removes a resource from state')

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