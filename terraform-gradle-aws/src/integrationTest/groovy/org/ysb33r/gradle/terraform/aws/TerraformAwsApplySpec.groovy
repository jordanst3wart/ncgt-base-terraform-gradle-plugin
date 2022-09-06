/*
 * Copyright 2017-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ysb33r.gradle.terraform.aws

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.ysb33r.gradle.terraform.aws.testfixtures.IntegrationSpecification
import org.ysb33r.grolifant.api.core.OperatingSystem
import spock.lang.IgnoreIf
import spock.util.environment.RestoreSystemProperties

import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

import static java.nio.file.FileVisitResult.CONTINUE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.ysb33r.grashicorp.HashicorpUtils.escapedFilePath

@IgnoreIf({ IntegrationSpecification.SKIP_TESTS })
@RestoreSystemProperties
class TerraformAwsApplySpec extends IntegrationSpecification {

    public static final String FILE_CONTENTS = 'foo!!'
    public static final String AWS_KEY = 'abcdefghijklo'
    public static final String AWS_SECRET = 'asdfgsdafsdfdssdaf'
    public static final String AWS_PROFILE = 'prodadmin'
    public static final String ROLE_ARN = 'arn:aws:iam::000000000000:role/AccountAdminRole'
    public static final String REGION = 'ca-central-1'
    public static final OperatingSystem OS = OperatingSystem.current()

    File srcDir
    File destFile
    File configFile
    File credentialsFile

    void setup() {
        srcDir = new File(projectDir, 'src/tf/main')
        srcDir.mkdirs()
        destFile = createTF()
        configFile = new File(projectDir, '.aws-config')
        credentialsFile = new File(projectDir, '.aws-credentials')
    }

    void cleanup() {
        Files.walkFileTree(
            new File(projectDir, 'build/tf/main/plugins').toPath(),
            new FileVisitor<Path>() {
                @Override
                FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    CONTINUE
                }

                @Override
                FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (attrs.symbolicLink) {
                        println "Deleting: ${file}"
                        Files.delete(file)
                    }
                    CONTINUE
                }

                @Override
                FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    println "Failed to visit: ${file}, because ${exc.message}"
                    CONTINUE
                }

                @Override
                FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    CONTINUE
                }
            }
        )
    }

    void 'Run terraform plan with properties as aws credentials'() {
        setup:
        createBuildFile(usePropertiesForTest())

        when:
        BuildResult result = getGradleRunner([
            'tfPlan',
            "-Pmy.profile=${AWS_PROFILE}".toString(),
            "-Pmy.config=${configFile.absolutePath}".toString(),
            "-Pmy.credentials=${credentialsFile.absolutePath}".toString()
        ]).build()

        then:
        result.task(':tfPlan').outcome == SUCCESS
        verifyAll {
            result.output.contains("\"AWS_ACCESS_KEY_ID\"           = \"${AWS_KEY}\"")
            result.output.contains("\"AWS_SECRET_ACCESS_KEY\"       = \"${AWS_SECRET}\"")
            result.output.contains("\"AWS_PROFILE\"                 = \"${AWS_PROFILE}\"")
            result.output.contains("\"AWS_CONFIG_FILE\"             = \"${safePath(configFile)}\"")
            result.output.contains("\"AWS_SHARED_CREDENTIALS_FILE\" = \"${safePath(credentialsFile)}\"")
        }
    }

    void 'Run terraform plan with environment variables'() {
        setup:
        createBuildFile(useEnvironmentForTest())

        when:
        BuildResult result = getGradleRunner(['tfPlan'])
            .withEnvironment(System.getenv() + [
                AWS_ACCESS_KEY_ID    : "ENV_${AWS_KEY}".toString(),
                AWS_SECRET_ACCESS_KEY: AWS_SECRET
            ])
            .withDebug(false)
            .build()

        then:
        result.task(':tfPlan').outcome == SUCCESS
        result.output.contains("\"AWS_ACCESS_KEY_ID\"     = \"ENV_${AWS_KEY}\"")
        result.output.contains("\"AWS_SECRET_ACCESS_KEY\" = \"${AWS_SECRET}\"")
    }

    void 'Run terraform plan with assume role using properties'() {
        setup:
        createBuildFile(usePropertiesForAssumeRoleTest())

        when:
        BuildResult result = getGradleRunner(['tfPlan']).build()

        then:
        result.task(':tfPlan').outcome == SUCCESS
        result.output.contains("\"AWS_ACCESS_KEY_ID\"     = \"${AWS_KEY}_FAKE\"")
        result.output.contains("\"AWS_SECRET_ACCESS_KEY\" = \"${AWS_SECRET}_FAKE\"")
        result.output.contains("\"AWS_SESSION_TOKEN\"     = \"${ROLE_ARN}_${REGION}_FAKE\"")
    }

    void 'Run terraform plan with assume role using environment'() {
        setup:
        createBuildFile(useEnvironmentForAssumeRoleTest())

        when:
        BuildResult result = getGradleRunner(['tfPlan'])
            .withEnvironment(System.getenv() + [
                AWS_ACCESS_KEY_ID    : "ENV_${AWS_KEY}".toString(),
                AWS_SECRET_ACCESS_KEY: AWS_SECRET
            ])
            .withDebug(false)
            .build()

        then:
        result.task(':tfPlan').outcome == SUCCESS
        result.output.contains("\"AWS_ACCESS_KEY_ID\"     = \"ENV_${AWS_KEY}_FAKE\"")
        result.output.contains("\"AWS_SECRET_ACCESS_KEY\" = \"${AWS_SECRET}_FAKE\"")
        result.output.contains("\"AWS_SESSION_TOKEN\"     = \"${ROLE_ARN}_${REGION}_FAKE\"")
    }

    GradleRunner getGradleRunner(List<String> tasks) {
        getGradleRunner(
            IS_GROOVY_DSL,
            projectDir,
            [
                'tfInit',
                '-s',
                '-i',
                '-Dorg.ysb33r.gradle.terraform.integration.tests.fake.session.tokens=1',
                "-Pmy.aws.key=${AWS_KEY}".toString(),
                "-Pmy.aws.secret=${AWS_SECRET}".toString()
            ] + (tasks as List)
        )
    }

    File createTF() {
        File destFile = new File(testProjectDir.root, 'TF/foo.bar')
        new File(srcDir, 'init.tf').text = """
        terraform {
              required_providers {
                environment = {
                    source = "EppO/environment"
                    version = "1.1.0"
                }
            }
        }

        data "environment_variables" "token" {
            filter = "^AWS_.+"
        }

        resource "null_resource" "token" {
            triggers = data.environment_variables.token.items
        }
        """
        destFile
    }

    void createBuildFile(String credsType) {
        buildFile.text = """
        plugins {
            id 'org.ysb33r.terraform.aws'
        }
        
        terraformSourceSets {
            main {
                aws {
                    ${credsType}    
                }
            }
        }

        tfApply {
            logProgress = true
        }
        """
    }

    String usePropertiesForTest() {
        '''
        usePropertiesForAws 'default', {
            accessKey = 'my.aws.key' 
            secretKey = 'my.aws.secret'
            profile = 'my.profile'
            configFile = 'my.config'
            credentialsFile = 'my.credentials'
        }
        '''
    }

    String useEnvironmentForTest() {
        'useAwsCredentialsFromEnvironment()'
    }

    String usePropertiesForAssumeRoleTest() {
        """usePropertiesForAssumeRole 'default', 'my.aws.key' , 'my.aws.secret', {
            roleArn = '${ROLE_ARN}'
            region = '${REGION}'
            durationSeconds = 5
        }
        """
    }

    String useEnvironmentForAssumeRoleTest() {
        """useAwsCredentialsFromEnvironmentForAssumeRole 'default', {
            roleArn = '${ROLE_ARN}'
            region = '${REGION}'
            durationSeconds = 5
        }
        """
    }

    String safePath(File file) {
        escapedFilePath(OS, file.absoluteFile)
    }
}