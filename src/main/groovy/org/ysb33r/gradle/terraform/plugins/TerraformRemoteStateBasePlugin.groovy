package org.ysb33r.gradle.terraform.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.remotestate.TerraformRemoteStateExtension

class TerraformRemoteStateBasePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.apply plugin: TerraformBasePlugin

        ((ExtensionAware) project.extensions.getByType(TerraformExtension))
            .extensions.create(TerraformRemoteStateExtension.NAME, TerraformRemoteStateExtension, project)
    }
}
