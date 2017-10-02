package org.ysb33r.gradle.terraform.config

import groovy.transform.CompileStatic

/** Allows for lock configurations on a task
 *
 * @since 0.1
 */
@CompileStatic
class Lock {
    boolean enabled = false
    Integer timeout = 0
}
