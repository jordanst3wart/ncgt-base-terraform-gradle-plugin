package org.ysb33r.gradle.terraform.internal

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet
import org.ysb33r.gradle.terraform.tasks.DefaultTerraformTasks
import org.ysb33r.gradle.terraform.tasks.DefaultTerraformTasks.APPLY
import org.ysb33r.gradle.terraform.tasks.DefaultTerraformTasks.INIT
import org.ysb33r.gradle.terraform.tasks.RemoteStateTask
import org.ysb33r.gradle.terraform.tasks.TerraformInit
import org.ysb33r.gradle.terraform.tasks.TerraformTask
import java.io.File

/** Provide convention naming.
 *
 * @author Schalk W. Cronjé
 */
object Convention {
    const val FORMAT_ALL = "fmtAll"
    const val TERRAFORM_RC_EXT = "terraformrc"
    const val TERRAFORM_SOURCESETS = "terraformSourceSets"
    const val TERRAFORM_TASK_GROUP = "Terraform"

    /** Provides a task name
     *
     * @param sourceSetName Name of source set the task will be associated with.
     * @param commandType The Terraform command that this task will wrap.
     * @return Name of task
     *
     * @since 0.10
     */
    @JvmStatic
    fun taskName(sourceSetName: String, commandType: String): String {
        return "${commandType}${sourceSetName.capitalize()}"
    }

    /**
     * The name of the backend configuration task.
     *
     * @param sourceSetName Name of source set.
     *
     * @return Name of task
     *
     * @since 0.12
     */
    @JvmStatic
    fun backendTaskName(sourceSetName: String): String {
        return "create${taskName(sourceSetName, "backendConfiguration").capitalize()}"
    }

    /** Returns the default text used for a Terraform source set
     *
     * @param sourceSetName Name of the source set
     * @return Display name
     */
    @JvmStatic
    fun sourceSetDisplayName(sourceSetName: String): String {
        return "Terraform source set for ${sourceSetName}"
    }

    /** Creates or registers the tasks associated with an additional workspace in a sourceset.
     *
     * For any source set other than [main], tasks will be named using a pattern such as
     * [terraform<SourceSetName> Init<WorkspaceName>].
     *
     * @param project Project to attach source set to.
     * @param sourceSetName Name of Terraform source set.
     */
    @JvmStatic
    fun createTasksByConvention(project: Project, sourceSet: TerraformSourceDirectorySet) {
        if (!hasTaskRegistered(project.tasks, taskName(sourceSet.name, APPLY.command))) {
            DefaultTerraformTasks.tasks().forEach {
                val newTaskName = taskName(sourceSet.name, it.command)
                registerTask(sourceSet, project, it, newTaskName)
            }
            registerBackendConfigurationTask(sourceSet, project)
        } else {
            throw IllegalStateException("duplicate tasks creation running for sourceSet ${sourceSet.name}")
        }
    }

    private fun registerBackendConfigurationTask(
        sourceSet: TerraformSourceDirectorySet,
        project: Project
    ) {
        val remoteStateTask: TaskProvider<RemoteStateTask> = project.tasks.register(
            backendTaskName(sourceSet.name),
            RemoteStateTask::class.java
        ) { it ->
            it.group = TERRAFORM_TASK_GROUP
            it.description = "Write partial backend configuration file for '${sourceSet.name}'"
            it.backendText.set(sourceSet.backendPropertyText().map { text -> text })
            // TODO clean this up I could add this logic to the source set
            it.backendConfig.set(File("${project.buildDir}/${sourceSet.name}/tf/remoteState/backend-config.tf"))
        }

        project.tasks.named(taskName(sourceSet.name, "init"), TerraformInit::class.java).configure { it ->
            it.dependsOn(remoteStateTask)
            it.backendConfig.set(File("${project.buildDir}/${sourceSet.name}/tf/remoteState/backend-config.tf"))
            it.useBackendConfig.set(true)
        }
    }

    private fun registerTask(
        sourceSet: TerraformSourceDirectorySet,
        project: Project,
        taskDetails: DefaultTerraformTasks,
        newTaskName: String
    ) {
        val taskProvider: TaskProvider<TerraformTask> = project.tasks.register(
            newTaskName,
            taskDetails.type as Class<TerraformTask>
        ) as TaskProvider<TerraformTask>
        taskProvider.configure(taskConfigurator(sourceSet, taskDetails))
    }

    private fun taskConfigurator(
        sourceSet: TerraformSourceDirectorySet,
        type: DefaultTerraformTasks
    ): Action<TerraformTask> {
        val name = sourceSet.name
        return Action<TerraformTask> { t ->
            t.setSourceSet(sourceSet)
            t.group = TERRAFORM_TASK_GROUP
            t.description = "${type.description} for '${name}'"
            if (type != DefaultTerraformTasks.INIT) {
                t.mustRunAfter(taskName(name, INIT.command))
            }
        }
    }

    private fun hasTaskRegistered(tasks: TaskContainer, name: String): Boolean {
        return tasks.names.contains(name)
    }
}