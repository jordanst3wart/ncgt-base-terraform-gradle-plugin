package org.ysb33r.gradle.terraform.tasks

import groovy.transform.CompileStatic
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.grolifant.api.wrapper.script.AbstractScriptWrapperTask

import javax.inject.Inject

@CompileStatic
class TerraformWrapper extends AbstractScriptWrapperTask {

    @Inject
    TerraformWrapper(TerraformCacheBinary cacheTask) {
        super()
        this.terraformExtension = project.extensions.getByType(TerraformExtension)
        this.cacheTask = cacheTask
        outputs.files(TEMPLATE_MAPPING.values().collect {
            new File(project.projectDir, it)
        })
        inputs.file(cacheTask.locationPropertiesFile)
        dependsOn(cacheTask)
        deleteTemplatesAfterUsage = true
        useWrapperTemplatesInResources(TEMPLATE_RESOURCE_PATH, TEMPLATE_MAPPING)
    }

    @Override
    protected String getBeginToken() {
        TEMPLATE_TOKEN_DELIMITER
    }

    @Override
    protected String getEndToken() {
        TEMPLATE_TOKEN_DELIMITER
    }

    @Override
    protected Map<String, String> getTokenValuesAsMap() {
        [
            APP_BASE_NAME               : 'terraform',
            GRADLE_WRAPPER_RELATIVE_PATH: project.relativePath(project.rootDir),
            DOT_GRADLE_RELATIVE_PATH    : project.relativePath(cacheTask.locationPropertiesFile.get().parentFile),
            APP_LOCATION_FILE           : cacheTask.locationPropertiesFile.get().name,
            CACHE_TASK_NAME             : cacheTask.name
        ]
    }

    private final TerraformExtension terraformExtension
    private final TerraformCacheBinary cacheTask

    private static final String TEMPLATE_TOKEN_DELIMITER = '~~'
    private static final String TEMPLATE_RESOURCE_PATH = '/terraform-wrapper'
    private static final Map<String, String> TEMPLATE_MAPPING = [
        'wrapper-template.sh' : 'terraformw',
        'wrapper-template.bat': 'terraformw.bat',
//        'wrapper-template.ps': 'terraformw.ps',
    ]
}
