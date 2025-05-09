package org.ysb33r.gradle.terraform.tasks

/** Maps terraform tasks to conventions. */
enum class DefaultTerraformTasks(
    val command: String,
    val type: Class<*>,
    val description: String
) {
    INIT("init", TerraformInit::class.java, "Initialises Terraform"),
    SHOW_STATE("showState", TerraformShowState::class.java, "Generates a report on the current state"),
    // SHOW("show", TerraformShow::class.java, "Generates a report from a plan file"),
    PLAN("plan", TerraformPlan::class.java, "Generates Terraform execution plan"),
    APPLY("apply", TerraformApply::class.java, "Builds or changes infrastructure"),
    DESTROY_PLAN("destroyPlan", TerraformDestroyPlan::class.java, "Generates Terraform destruction plan"),
    DESTROY("destroy", TerraformDestroy::class.java, "Destroys infrastructure"),
    VALIDATE("validate", TerraformValidate::class.java, "Validates the Terraform configuration"),
    FMT_CHECK("fmtCheck", TerraformFmtCheck::class.java, "Checks whether files are correctly formatted"),
    FMT_APPLY("fmtApply", TerraformFmtApply::class.java, "Formats source files in source set");

    companion object {
        fun tasks(): List<DefaultTerraformTasks> {
            return values().toList()
        }
    }
}