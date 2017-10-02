package org.ysb33r.gradle.terraform.config

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskContainer
import org.ysb33r.gradle.terraform.tasks.base.AbstractTerraformTask

/**
 * @since 0.1
 */
@CompileStatic
class Source {
    Source(AbstractTerraformTask task) {
        this.task = task

        task.project.afterEvaluate ({ final AbstractTerraformTask terraformTask, final Source src, Project project ->
            terraformTask.dependsOn(src.getGenerator())
        }.curry(task,this))
    }

    /** The task that will process original source and create suitable workable directory for
     * the task that this is attached to.
     *
     * @param task
     */
    void generatedBy(Task task) {
        setDependsOnTask(task)
//        generator = task
    }

    /** The task that will process original source and create suitable workable directory for
     * the task that this is attached to. It could also be a task that creates a plan.
     *
     * @param task
     */
    void generatedBy(final String taskName) {
        setDependsOnTask(task)
//        generator = (Copy)(task.project.tasks.getByName(taskName))
    }

    /** The directory where Terraform will do all the from.
     *
     * @return Working directory for Terraform.
     */
    @InputDirectory
    File getInputSource() {
        if(generator == null) {
            throw new GradleException("Source processing task has not been defined")
        }
        getDestinationFromTask(dependsOnTask())
    }

    Task getGenerator() {
        dependsOnTask()
    }

    static void canGetDestinationDir(Task t) {
        if(!t.respondsTo('getDestinationDir')) {
            throw new GradleException ("${t.name} is not usuable as it does not provide a getDestinationDir method")
        }
    }

    private void setDependsOnTask(Object t) {
        if(t instanceof Task) {
            canGetDestinationDir((Task)t)
            dependsOnTask = { -> t}
        } else {
            dependsOnTask = { TaskContainer tasks ->
                Task foundTask = tasks.getByName(t.toString())
                canGetDestinationDir(foundTask)
                foundTask
            }.curry(task.project.tasks)
        }
    }

    @CompileDynamic
    File getDestinationFromTask(Task t) {
        task.project.file(t.getDestinationDir)
    }

    private Closure dependsOnTask
//    private Task generator
    final private AbstractTerraformTask task
}
