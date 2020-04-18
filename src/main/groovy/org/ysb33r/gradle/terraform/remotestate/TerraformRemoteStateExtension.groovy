package org.ysb33r.gradle.terraform.remotestate

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.grolifant.api.StringUtils

import java.util.concurrent.Callable

/** Extension that is added to the project {@link TerraformExtension}
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.8.0
 */
@CompileStatic
class TerraformRemoteStateExtension {

    public static final String NAME = 'remote'

    /** A utility to locate the extension.
     *
     * @param project Project context
     * @return {@link TerraformRemoteStateExtension} if it has been added.
     */
    static TerraformRemoteStateExtension findExtension(Project project) {
        ((ExtensionAware) project.extensions.getByType(TerraformExtension))
            .extensions.getByType(TerraformRemoteStateExtension)
    }

    TerraformRemoteStateExtension(Project project) {
        this.project = project
        this.prefix = project.objects.property(String)
        setPrefix(project.name)
    }

    /** Assign the prefix.
     *
     * @param p Object that can be converted to a string. Can be a {@code Provider} as well.
     */
    void setPrefix(Object p) {
        this.prefix.set(project.provider({
            StringUtils.stringize(p)
        } as Callable<String>))
    }

    /** A prefix that is added to remote state names.
     *
     */
    Provider<String> getPrefix() {
        this.prefix
    }

    private final Project project
    private final Property<String> prefix
}
