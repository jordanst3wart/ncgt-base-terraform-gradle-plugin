package org.ysb33r.gradle.terraform.config

import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.testfixtures.ProjectBuilder
import org.ysb33r.gradle.terraform.TerraformExecSpec
import AbstractTerraformTask
import spock.lang.Specification

class SourceSpec extends Specification {

    Project project = ProjectBuilder.builder().build()
    TestTask task = project.tasks.create('foo',TestTask)

    def "Configure a Source instance"() {
        given:
        Source source = new Source(task)
        Copy copy = project.tasks.create('copy',Copy)

        when:
        copy.into "${buildDir}/cloud"

        then:
        source.getInputSource() == project.file("${buildDir}/cloud")
    }

    static class TestTask extends AbstractTerraformTask {

        @Override
        protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
            execSpec
        }
    }
}