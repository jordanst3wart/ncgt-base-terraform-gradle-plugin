package org.ysb33r.gradle.terraform

import groovy.transform.CompileStatic

/**
 * Major version groupings for Terraform
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.10.0
 */
@CompileStatic
enum TerraformMajorVersion {
    VERSION_11_OR_OLDER,
    VERSION_12,
    VERSION_13,
    VERSION_14,
    VERSION_15,
    UNKNOWN

    static TerraformMajorVersion fromMinor(int ver) {
        if (ver < 12) {
            VERSION_11_OR_OLDER
        } else {
            try {
                valueOf("VERSION_${ver}")
            } catch (IllegalArgumentException e) {
                UNKNOWN
            }
        }
    }
}