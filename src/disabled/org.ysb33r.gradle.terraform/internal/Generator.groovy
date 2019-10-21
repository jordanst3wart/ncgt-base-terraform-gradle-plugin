package org.ysb33r.gradle.terraform.internal

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

/**
 * @since 0.1
 */
@CompileStatic
class Generator {
    @CompileDynamic
    static Copy create(final Project project,final String baseName = 'terraform') {
        final String name = "process${baseName.capitalize()}Source"
        final String terraformDir = "src/${baseName}"
        project.tasks.create( name, Copy) {
            from terraformDir, {
                include '*.tf'
                include '*.tfvars'
                include '*.json'
            }
            into ({ String dir -> "${project.buildDir}/tf/${dir}" }.curry(baseName))
            group 'terraform'
            description "Processes ${baseName.capitalize()} source"
        }
    }
}
