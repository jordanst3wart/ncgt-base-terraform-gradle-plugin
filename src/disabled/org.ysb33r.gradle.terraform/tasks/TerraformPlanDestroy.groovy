package org.ysb33r.gradle.terraform.tasks

import CompileStatic
import AbstractTerraformPlan

import static AbstractTerraformPlan.PlanPurpose.SAVE_PLAN
import static AbstractTerraformPlan.PlanType.DESTROY_PLAN

/**
 * @since 0.1
 */
@CompileStatic
class TerraformPlanDestroy extends AbstractTerraformPlan {
     // -destroy
    TerraformPlanDestroy() {
        super(SAVE_PLAN,DESTROY_PLAN)
    }
}
