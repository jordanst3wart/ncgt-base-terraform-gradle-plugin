package org.ysb33r.gradle.terraform.tasks

import groovy.transform.CompileStatic
import org.gradle.api.provider.Provider
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.grolifant.api.wrapper.script.AbstractCacheBinaryTask

@CompileStatic
class TerraformCacheBinary extends AbstractCacheBinaryTask {

    public static final String LOCATION_PROPERTIES_DEFAULT = 'terraform.properties'

    TerraformCacheBinary() {
        super(LOCATION_PROPERTIES_DEFAULT)
        this.terraformExtension = project.extensions.getByType(TerraformExtension)
    }

    @Override
    protected String getBinaryVersion() {
        switch (terraformExtension.resolvableExecutableType.type) {
            case 'version':
                return terraformExtension.resolvableExecutableType.value.get()
            default:
                ''
        }
    }

    @Override
    protected String getPropertiesDescription() {
        "Describes the Terraform usage for the ${project.name} project"
    }

    @Override
    protected String getBinaryLocation() {
        terraformExtension.resolvableExecutable.getExecutable().canonicalPath
    }

    private Provider<File> locationPropertiesFile
    private final TerraformExtension terraformExtension
}
