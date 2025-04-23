package org.ysb33r.gradle.terraform.config

import groovy.transform.CompileStatic

@CompileStatic
class Json implements ConfigExtension {
    final String name = 'json'

    boolean enabled = false

    /** Command-line parameter for JSON output.
     *
     */
    protected static final String JSON_FORMAT = '-json'

    @Override
    List<String> getCommandLineArgs() {
        if (enabled) {
            return [JSON_FORMAT]
        }
        return []
    }
}
