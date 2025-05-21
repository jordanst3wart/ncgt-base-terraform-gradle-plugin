package org.ysb33r.gradle.terraform.internal

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.ysb33r.gradle.terraform.TerraformSourceSet
import org.ysb33r.gradle.terraform.tasks.DefaultTerraformTasks
import org.ysb33r.gradle.terraform.tasks.DefaultTerraformTasks.APPLY
import org.ysb33r.gradle.terraform.tasks.DefaultTerraformTasks.INIT
import org.ysb33r.gradle.terraform.tasks.RemoteStateTask
import org.ysb33r.gradle.terraform.tasks.TerraformInit
import java.io.File

object Convention {
    const val FORMAT_ALL = "fmtAll"
    const val TERRAFORM_SETUP_EXT = "terraformSetup"
    const val TERRAFORM_SOURCESETS = "terraformSourceSets"
    const val TERRAFORM_TASK_GROUP = "Terraform"

    @JvmStatic
    fun taskName(sourceSetName: String, commandType: String): String {
        return "${commandType}${sourceSetName.capitalize()}"
    }

    @JvmStatic
    fun backendTaskName(sourceSetName: String): String {
        return "create${taskName(sourceSetName, "backendConfiguration").capitalize()}"
    }

    @JvmStatic
    fun sourceSetDisplayName(sourceSetName: String): String {
        return "Terraform source set for $sourceSetName"
    }

    @JvmStatic
    fun createTasksByConvention(project: Project, sourceSet: TerraformSourceSet) {
        DefaultTerraformTasks.tasks().forEach {
            registerTask(sourceSet, project, it)
        }
        registerBackendConfigurationTask(sourceSet, project)
    }

    private fun registerTask(
        sourceSet: TerraformSourceSet,
        project: Project,
        taskDetails: DefaultTerraformTasks,
    ) {
        val name = sourceSet.name
        val newTaskName = taskName(sourceSet.name, taskDetails.command)
        project.tasks.register(
            newTaskName,
            taskDetails.type
        ) { t ->
            t.sourceSet.set(sourceSet)
            t.group = TERRAFORM_TASK_GROUP
            t.description = "${taskDetails.description} for '${name}'"
            // TODO simplify this
            if (taskDetails != INIT) {
                t.mustRunAfter(taskName(name, INIT.command))
            }
            if (taskDetails == DefaultTerraformTasks.PLAN) {
                t.dependsOn(taskName(name, INIT.command))
            }
            if (taskDetails == DefaultTerraformTasks.DESTROY_PLAN) {
                t.dependsOn(taskName(name, INIT.command))
            }
            if (taskDetails == DefaultTerraformTasks.VALIDATE) {
                t.dependsOn(taskName(name, INIT.command))
            }
            if (taskDetails == APPLY) {
                t.dependsOn(taskName(name, DefaultTerraformTasks.PLAN.command))
            }
        }
    }

    private fun registerBackendConfigurationTask(
        sourceSet: TerraformSourceSet,
        project: Project
    ) {
        val remoteStateTask: TaskProvider<RemoteStateTask> = project.tasks.register(
            backendTaskName(sourceSet.name),
            RemoteStateTask::class.java
        ) { it ->
            it.group = TERRAFORM_TASK_GROUP
            it.description = "Write partial backend configuration file for '${sourceSet.name}'"
            it.backendText.set(sourceSet.backendText.map { text -> text })
            // TODO clean this up I could add this logic to the source set
            it.backendConfig.set(File(project.layout.buildDirectory.get().asFile, "${sourceSet.name}/tf/remoteState/backend-config.tf"))
        }

        project.tasks.named(taskName(sourceSet.name, "init"), TerraformInit::class.java).configure { it ->
            it.dependsOn(remoteStateTask)
            it.backendConfig.set(File(project.layout.buildDirectory.get().asFile, "${sourceSet.name}/tf/remoteState/backend-config.tf"))
            it.useBackendConfig.set(true)
        }
    }
}