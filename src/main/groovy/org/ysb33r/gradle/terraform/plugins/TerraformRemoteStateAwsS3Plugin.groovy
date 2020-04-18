package org.ysb33r.gradle.terraform.plugins

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.ysb33r.gradle.terraform.remotestate.RemoteStateS3

import static org.ysb33r.gradle.terraform.remotestate.TerraformRemoteStateExtension.findExtension

@CompileStatic
class TerraformRemoteStateAwsS3Plugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.apply plugin: TerraformRemoteStateBasePlugin
        ((ExtensionAware) findExtension(project)).extensions.create(RemoteStateS3.NAME, RemoteStateS3, project)

        project.apply plugin: TerraformPlugin
    }
}
