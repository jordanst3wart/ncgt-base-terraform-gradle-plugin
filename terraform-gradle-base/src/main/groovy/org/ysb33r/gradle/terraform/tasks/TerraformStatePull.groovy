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
import org.gradle.api.Transformer
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.options.Option

import javax.inject.Inject

/** The {@code terraform state rm} command.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.7.0
 */
@CompileStatic
class TerraformStatePull extends AbstractTerraformStateTask {

    @Inject
    TerraformStatePull(String workspaceName) {
        super('pull', workspaceName)
        stateFileProvider = sourceDir.map({ String s, File it ->
            new File(it, s)
        }.curry(this.stateFile) as Transformer<File, File>)
        captureStdOutTo(stateOutputFile)
    }

    @Option(option = 'state-file', description = 'where to write local state file (relative to tf directory)')
    void setStateFileType(String path) {
        this.stateFile = path
    }

    @Internal
    Provider<File> getStateOutputFile() {
        stateFileProvider
    }

    @Override
    void exec() {
        super.exec()
        logger.lifecycle("\nState file has been written to ${stateOutputFile.get().absolutePath}\n")
    }

    private String stateFile
    private final Provider<File> stateFileProvider
}
