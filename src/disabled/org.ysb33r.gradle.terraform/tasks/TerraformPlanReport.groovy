package org.ysb33r.gradle.terraform.tasks


import AbstractTerraformPlan

import static AbstractTerraformPlan.PlanPurpose.REPORT_PLAN
import static AbstractTerraformPlan.PlanType.BUILD_PLAN

/**
 * @since 0.1
 */
class TerraformPlanReport extends AbstractTerraformPlan {
    // output plan to text i.e. capture stdout to file

    TerraformPlanReport() {
        super(REPORT_PLAN,BUILD_PLAN)
        inputs.property('modified-module-depth',{getModuleDepth()})
    }

    @Override
    protected File getReportFile() {
        project.file("${buildDir}/reports/tf/${name}-plan.txt")
    }
}
