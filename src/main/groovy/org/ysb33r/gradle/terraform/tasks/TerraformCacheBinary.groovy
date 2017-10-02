package org.ysb33r.gradle.terraform.tasks

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.ysb33r.gradle.terraform.TerraformExtension

import java.util.concurrent.Callable

@CompileStatic
class TerraformCacheBinary extends DefaultTask {

    public static final String LOCATION_PROPERTIES_DEFAULT = 'terraform.properties'

    TerraformCacheBinary() {
        this.terraformExtension = project.extensions.getByType(TerraformExtension)
        this.locationPropertiesFile = project.providers.provider({
            new File(
                project.gradle.startParameter.projectCacheDir ?: project.file("${project.projectDir}/.gradle"),
                LOCATION_PROPERTIES_DEFAULT
            )
        } as Callable<File>)
    }

    @Input
    String getBinaryVersion() {
        switch (terraformExtension.resolvableExecutableType.type) {
            case 'version':
                return terraformExtension.resolvableExecutableType.value.get()
            default:
                ''
        }
    }

    @OutputFile
    Provider<File> getLocationPropertiesFile() {
        this.locationPropertiesFile
    }

    void setLocationPropertiesFile(Object o) {
        switch (o) {
            case Provider:
                this.locationPropertiesFile = (Provider<File>) o
                break
            default:
                this.locationPropertiesFile = project.providers.provider({
                    project.file(o)
                } as Callable<File>)
        }
    }

    @TaskAction
    void exec() {
        File propsFile = locationPropertiesFile.get()
        Properties props = new Properties()
        props['location'] = binaryLocation
        props['binaryVersion'] = binaryVersion
        propsFile.withWriter { Writer w ->
            props.store(w, "Describes the Terraform usage for the ${project.name} project")
        }
    }

    private String getBinaryLocation() {
        terraformExtension.resolvableExecutable.getExecutable().canonicalPath
    }

    private Provider<File> locationPropertiesFile
    private final TerraformExtension terraformExtension
}
