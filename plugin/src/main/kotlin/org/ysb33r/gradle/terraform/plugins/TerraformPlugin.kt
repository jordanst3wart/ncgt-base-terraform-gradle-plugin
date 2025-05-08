package org.ysb33r.gradle.terraform.plugins

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.TerraformRCExtension
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet
import org.ysb33r.gradle.terraform.internal.Convention
import org.ysb33r.gradle.terraform.tasks.TerraformTask
import org.ysb33r.gradle.terraform.tasks.RemoteStateTask
import org.ysb33r.gradle.terraform.tasks.TerraformFmtCheck
import org.ysb33r.grolifant.api.core.ProjectOperations

import org.gradle.language.base.plugins.LifecycleBasePlugin.CHECK_TASK_NAME
import org.ysb33r.gradle.terraform.internal.Convention.sourceSetDisplayName
import org.ysb33r.gradle.terraform.tasks.DefaultTerraformTasks.FMT_APPLY
import org.ysb33r.gradle.terraform.internal.Convention.createTasksByConvention
import org.ysb33r.gradle.terraform.internal.Convention.taskName
import java.io.StringWriter

/**
 * Provide the basic capabilities for dealing with Terraform tasks. Allow for downloading & caching of
 * Terraform distributions on a variety of the most common development platforms.
 */
class TerraformPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        ProjectOperations.maybeCreateExtension(project)
        configureTerraformRC(project.rootProject)

        project.tasks.withType(RemoteStateTask::class.java).configureEach { t ->
            t.group = Convention.TERRAFORM_TASK_GROUP
            t.dependsOn(TerraformRCExtension.TERRAFORM_RC_TASK)
        }

        project.tasks.withType(TerraformTask::class.java).configureEach { t ->
            t.dependsOn(TerraformRCExtension.TERRAFORM_RC_TASK)
        }

        project.extensions.create(TerraformExtension.NAME, TerraformExtension::class.java, project)
        createTerraformSourceSetsExtension(project)

        val formatAll = project.tasks.register(Convention.FORMAT_ALL)
        formatAll.configure {
            it.group = Convention.TERRAFORM_TASK_GROUP
            it.description = "Formats all terraform source"
        }
        terraformSourceSetConventionTaskRules(project, formatAll)
        configureCheck(project)
    }

    companion object {
        private fun configureTerraformRC(rootProject: Project) {
            // create projections for root rootProject
            ProjectOperations.maybeCreateExtension(rootProject)
            val terraformRcExt = rootProject.extensions
                .create(Convention.TERRAFORM_RC_EXT, TerraformRCExtension::class.java, rootProject)
            rootProject.tasks.register(TerraformRCExtension.TERRAFORM_RC_TASK) { task ->
                task.group = Convention.TERRAFORM_TASK_GROUP
                task.description = "Generates Terraform configuration file"
                task.onlyIf { !terraformRcExt.useGlobalConfig }
                task.inputs.property("details") {
                    val writer = StringWriter()
                    terraformRcExt.toHCL(writer).toString()
                }
                task.outputs.file(terraformRcExt.getTerraformRC())
                task.doLast {
                    terraformRcExt.getTerraformRC().get().bufferedWriter().use { writer ->
                        terraformRcExt.toHCL(writer)
                    }
                }
            }
        }

        private fun createTerraformSourceSetsExtension(
            project: Project
        ): NamedDomainObjectContainer<TerraformSourceDirectorySet> {
            val factory = NamedDomainObjectFactory<TerraformSourceDirectorySet> { name ->
                project.objects.newInstance(
                    TerraformSourceDirectorySet::class.java,
                    project,
                    name,
                    sourceSetDisplayName(name)
                )
            }
            val sourceSetContainer =
                project.objects.domainObjectContainer(TerraformSourceDirectorySet::class.java, factory)
            project.extensions.add(Convention.TERRAFORM_SOURCESETS, sourceSetContainer)
            return sourceSetContainer
        }

        private fun configureCheck(project: Project) {
            project.pluginManager.apply(LifecycleBasePlugin::class.java)
            val check = project.tasks.named(CHECK_TASK_NAME)
            check.configure {
                it.dependsOn(project.tasks.withType(TerraformFmtCheck::class.java))
            }
        }

        private fun terraformSourceSetConventionTaskRules(
            project: Project,
            formatAll: TaskProvider<Task>
        ) {
            project.extensions.getByType(NamedDomainObjectContainer::class.java as Class<NamedDomainObjectContainer<TerraformSourceDirectorySet>>).configureEach(
                Action<TerraformSourceDirectorySet> { tsds ->
                    createTasksByConvention(project, tsds)
                    formatAll.configure {
                        it.dependsOn(project.tasks.named(taskName(tsds.name, FMT_APPLY.command)))
                    }
                }
            )
        }
    }
}
