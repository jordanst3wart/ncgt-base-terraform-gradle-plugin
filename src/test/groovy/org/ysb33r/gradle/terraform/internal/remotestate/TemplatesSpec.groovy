package org.ysb33r.gradle.terraform.internal.remotestate

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.ysb33r.gradle.terraform.tasks.RemoteStateAwsS3ConfigGenerator
import spock.lang.Specification

class TemplatesSpec extends Specification {

    Project project = ProjectBuilder.builder().build()

    void 'Generate a configuration file from a default template'() {
        setup:
        def taskName = 'fooTask'
        def outputFile = project.provider { -> new File(project.projectDir, 'output.tf') }
        def tokens = [
            aws_region       : 'REGION',
            remote_state_name: 'REMOTESTATE',
            bucket_name      : 'BUCKET'
        ]

        when:
        File target = Templates.generateFromTemplate(
            taskName,
            project,
            RemoteStateAwsS3ConfigGenerator.TEMPLATE_RESOURCE_PATH,
            project.objects.property(File),
            outputFile,
            '@@',
            '@@',
            tokens
        )

        then:
        target.exists()
        target.text.contains("bucket = \"${tokens.bucket_name}\"")
    }
}