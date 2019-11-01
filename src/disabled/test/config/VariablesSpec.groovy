package org.ysb33r.gradle.terraform.config

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.ysb33r.gradle.terraform.TerraformExecSpec
import AbstractTerraformTask
import spock.lang.Specification

class VariablesSpec extends Specification {

    Project project = ProjectBuilder.builder().build()
    TestTask task = project.tasks.create('foo',TestTask)

    def "Configure a Variables instance"() {
        given:
        Variables variables = new Variables(task)

        when:
        variables.var 'aVar', 'a-value'
        variables.map 'aMap', foo : 'bar', john : 'doe'
        variables.list 'aList', 'a-value', 'another-value'
        variables.list 'bList', ['bee','honey']
        variables.file 'a.tfvars'

        and:
        Map<String,Object> vars = variables.getVars()
        Set<File> files = variables.getFiles().files

        then:
        files == [project.file('a.tfvars')] as Set
        vars.size() == 4
        vars['aVar'] == 'a-value'
        vars['aMap'] == '{"foo" : "bar", "john" : "doe"}'
        vars['aList'] == '["a-value", "another-value"]'
        vars['bList'] == '["bee", "honey"]'
    }

    static class TestTask extends AbstractTerraformTask {

        @Override
        protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
            execSpec
        }
    }
}