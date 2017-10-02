package org.ysb33r.gradle.terraform.plugins

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.ysb33r.gradle.terraform.tasks.TerraformCacheBinary
import org.ysb33r.gradle.terraform.tasks.TerraformWrapper

import java.util.concurrent.Callable

@CompileStatic
class TerraformWrapperPlugin implements Plugin<Project> {
    public static final String WRAPPER_TASK_NAME = 'terraformWrapper'
    public static final String CACHE_BINARY_TASK_NAME = 'cacheTerraformBinary'

    @Override
    void apply(Project project) {
        project.apply plugin : TerraformBasePlugin

        TerraformCacheBinary terraformCacheBinary = project.tasks.create(CACHE_BINARY_TASK_NAME, TerraformCacheBinary)
        TerraformWrapper wrapper = project.tasks.create(WRAPPER_TASK_NAME, TerraformWrapper, terraformCacheBinary)
    }
}
