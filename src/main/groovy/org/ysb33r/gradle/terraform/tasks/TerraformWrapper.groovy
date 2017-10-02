package org.ysb33r.gradle.terraform.tasks

import groovy.transform.CompileStatic
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.TaskAction
import org.ysb33r.gradle.terraform.TerraformExtension

import javax.inject.Inject

@CompileStatic
class TerraformWrapper extends DefaultTask {

    @Inject
    TerraformWrapper(TerraformCacheBinary cacheTask) {
        this.terraformExtension = project.extensions.getByType(TerraformExtension)
        this.cacheTask = cacheTask
        outputs.files(TEMPLATE_MAPPING.values().collect {
            new File(project.projectDir, it)
        })
        inputs.file(cacheTask.locationPropertiesFile)

        dependsOn(cacheTask)
    }

    @TaskAction
    void exec() {
        List<File> templates = prepareTemplates()
        project.copy(new Action<CopySpec>() {
            @Override
            void execute(CopySpec copySpec) {
                copySpec.with {
                    from templates
                    into project.projectDir
                    fileMode = 0755
                }
                copySpec.filter ReplaceTokens, beginToken: '~~', endToken: '~~', tokens: [
                    APP_BASE_NAME               : 'terraform',
                    GRADLE_WRAPPER_RELATIVE_PATH: project.relativePath(project.rootDir),
                    DOT_GRADLE_RELATIVE_PATH    : project.relativePath(cacheTask.locationPropertiesFile.get().parentFile),
                    APP_LOCATION_FILE           : cacheTask.locationPropertiesFile.get().name,
                    CACHE_TASK_NAME             : cacheTask.name
                ]
            }
        })
        templates.each { it.delete() }
    }

    private List<File> prepareTemplates() {
        File templateLocation = intermediateTemplateLocation
        intermediateTemplateLocation.mkdirs()
        TEMPLATE_MAPPING.keySet().collect { String template ->
            File templateFile = new File(templateLocation, TEMPLATE_MAPPING[template])
            TerraformWrapper.getResourceAsStream("${TEMPLATE_RESOURCE_PATH}/${template}").withCloseable { input ->
                templateFile.withOutputStream { output ->
                    output << (InputStream) input
                }
            }
            templateFile
        }
    }

    private File getIntermediateTemplateLocation() {
        new File(project.buildDir, "tmp/${name}/templates")
    }

    private final TerraformExtension terraformExtension
    private final TerraformCacheBinary cacheTask

    private static final String TEMPLATE_RESOURCE_PATH = '/terraform-wrapper'
    private static final Map<String, String> TEMPLATE_MAPPING = [
        'wrapper-template.sh': 'terraformw',
        'wrapper-template.bat': 'terraformw.bat',
//        'wrapper-template.ps': 'terraformw.ps',
    ]
}
