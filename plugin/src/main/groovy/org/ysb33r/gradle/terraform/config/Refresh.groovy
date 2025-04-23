package org.ysb33r.gradle.terraform.config

import groovy.transform.CompileStatic

@CompileStatic
class Refresh implements ConfigExtension {
    final String name = 'refresh'
    boolean refresh = true

    @Override
    List<String> getCommandLineArgs() {
        ["-refresh=${refresh}".toString()]
    }
}