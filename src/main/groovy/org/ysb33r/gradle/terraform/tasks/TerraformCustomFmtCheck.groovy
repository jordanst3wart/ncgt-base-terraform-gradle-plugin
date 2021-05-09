/*
 * Copyright 2017-2021 the original author or authors.
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
package org.ysb33r.gradle.terraform.tasks

import groovy.transform.CompileStatic
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.process.ExecResult
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.errors.TerraformSourceFormatViolation

import java.util.concurrent.Callable

/**
 * Checks the format of Terraform source in an arbritrary collection of directories.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.10
 */
@CompileStatic
class TerraformCustomFmtCheck extends AbstractTerraformCustomFmt {

    /**
     * Whether all child directories of the input directory should be checked recursively.
     *
     * Default is to recurse.
     */
    @Input
    boolean recursive = true

    TerraformCustomFmtCheck() {
        super()
        this.sourceDirs = []
        this.sourceDirProvider = project.provider(
            new Callable<Set<File>>() {
                @Override
                Set<File> call() throws Exception {
                    projectOperations.fileize(sourceDirs).findAll {
                        it.exists() && it.directory
                    }.toSet()
                }
            }
        )
    }

    @Internal
    Provider<Set<File>> getSourceDirectoryProvider() {
        this.sourceDirProvider
    }

    /**
     * The list of toplevel directories to check
     *
     * @return List of directories
     */
    @InputFiles
    @SkipWhenEmpty
    @Override
    Provider<Set<File>> getSourceDirectories() {
        this.sourceDirProvider
    }

    /** Replace existing directories with a new set.
     *
     * @param d Directories to add. Anything convertible by the like of `project.file` is acceptable.
     */
    void setDirs(Iterable<?> d) {
        this.sourceDirs.clear()
        this.sourceDirs.addAll(d)
    }

    /** Add directories to be set.
     *
     * @param d Directories to add. Anything convertible by the like of `project.file` is acceptable.
     */
    void dirs(Object... d) {
        this.sourceDirs.addAll(d)
    }

    @Override
    protected void handleExecResult(ExecResult result) {
        if (result.exitValue == 3) {
            throw new TerraformSourceFormatViolation('Source format does not match convention')
        } else if (result.exitValue) {
            result.rethrowFailure()
        }
    }

    @Override
    protected void addCommandSpecificsForFmt(TerraformExecSpec execSpec) {
        execSpec.cmdArgs '-check'

        if (logger.infoEnabled) {
            execSpec.cmdArgs '-diff'
        }

        if (!logger.quietEnabled) {
            execSpec.cmdArgs '-list=true'
        }

        if (recursive) {
            execSpec.cmdArgs '-recursive'
        }
    }

    private final Provider<Set<File>> sourceDirProvider
    private final List<Object> sourceDirs
}
