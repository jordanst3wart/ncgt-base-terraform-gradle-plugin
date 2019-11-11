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
package org.ysb33r.gradle.terraform.config

import groovy.transform.CompileStatic
import org.ysb33r.gradle.terraform.tasks.AbstractTerraformTask

/** Allows for options related to walking the state tree.
 *
 * @since 0.1
 */
@CompileStatic
class StateOptionsFull extends StateOptionsConcurrency {
    boolean refresh = true

    StateOptionsFull(AbstractTerraformTask task) {
        super(task)
    }

    @Override
    List<String> getCommandLineArgs() {
        List<String> args = super.commandLineArgs
        args.add "-refresh=${refresh}".toString()
        args
    }
}
