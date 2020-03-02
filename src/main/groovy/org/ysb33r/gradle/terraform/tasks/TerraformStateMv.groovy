/*
 * Copyright 2017-2020 the original author or authors.
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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.TerraformExecSpec

/** The {@code terraform state mv} command.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.6.0
 */
@CompileStatic
class TerraformStateMv extends AbstractTerraformStateTask {

    TerraformStateMv() {
        super('mv')
    }

    /** The source path.
     *
     * Can be like {@code packet_device.worker}, {@code module.app} or even {@code module.parent.module.app}.
     *
     * @return Source path.
     */
    @Input
    String getSourcePath() {
        this.source
    }

    /** The destination path.
     *
     * Can be like {@code packet_device.worker}, {@code module.app} or even {@code module.parent.module.app}.
     *
     * @return Destination path.
     */
    @Input
    String getDestinationPath() {
        this.destination
    }

    @Option(option = 'from-path', description = 'Source item (dotted-path) to move')
    void setSourcePath(String id) {
        this.source = id
    }

    @Option(option = 'to-path', description = 'Destination for item (dotted-path)')
    void setDestinationPath(String id) {
        this.destination = id
    }

    @Override
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
        super.addCommandSpecificsToExecSpec(execSpec)
        execSpec.cmdArgs sourcePath, destinationPath
        execSpec
    }

    private String source
    private String destination
}
