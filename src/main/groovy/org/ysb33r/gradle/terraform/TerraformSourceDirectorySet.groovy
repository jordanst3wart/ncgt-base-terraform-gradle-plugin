/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ysb33r.gradle.terraform

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet

import java.util.concurrent.Callable

@CompileStatic
class TerraformSourceDirectorySet implements PatternFilterable {

    /** Source directory name
     *
     */
    final String name

    /** Displayable name of source set.
     *
     */
    final String displayName

    TerraformSourceDirectorySet(Project project, String name, String displayName) {
        this.name = name
        this.displayName = displayName
        this.project = project
        this.patternSet.include('**/*.tf', '**/*.tfvars', '*.tfstate')

        srcDir = "src/tf/${name}"
        dataDir = project.provider({
            project.file("${project.buildDir}/tf/${name}")
        } as Callable<File>)
        logDir = project.provider({
            project.file("${project.buildDir}/reports/tf/${name}/logs")
        } as Callable<File>)
        reportsDir = project.provider({
            project.file("${project.buildDir}/reports/tf/${name}")
        } as Callable<File>)
    }

    String toString() {
        this.displayName
    }

    Provider<File> getSrcDir() {
        this.sourceDir
    }

    TerraformSourceDirectorySet setSrcDir(Object dir) {
        this.sourceDir = project.provider { ->
            project.file(dir)
        }
        this
    }

    Provider<File> getDataDir() {
        this.dataDir
    }

    TerraformSourceDirectorySet setDataDir(Object dir) {
        this.dataDir = project.provider { ->
            project.file(dir)
        }
        this
    }

    Provider<File> getLogDir() {
        this.logDir
    }

    TerraformSourceDirectorySet setLogDir(Object dir) {
        this.logDir = project.provider { ->
            project.file(dir)
        }
        this
    }

    Provider<File> getReportsDir() {
        this.reportsDir
    }

    TerraformSourceDirectorySet setReportsDir(Object dir) {
        this.reportsDir = project.provider { ->
            project.file(dir)
        }
        this
    }

    PatternFilterable getFilter() {
        this.patternSet
    }

    FileTree getAsFileTree() {
        project.fileTree(sourceDir).matching(this.patternSet)
    }

    private final Project project
    private Provider<File> sourceDir
    private Provider<File> dataDir
    private Provider<File> logDir
    private Provider<File> reportsDir

    @Delegate
    private final PatternSet patternSet = new PatternSet()
}
