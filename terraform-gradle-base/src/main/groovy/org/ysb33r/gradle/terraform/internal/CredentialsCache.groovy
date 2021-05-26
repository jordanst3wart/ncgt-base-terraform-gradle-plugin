/*
 * Copyright 2017-2021 the original author or authors.
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
package org.ysb33r.gradle.terraform.internal

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.Synchronized
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.ysb33r.gradle.terraform.credentials.SessionCredentials

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Caches session credentials between tasks.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.11
 */
@CompileStatic
@Slf4j
class CredentialsCache {

    @Synchronized
    static Map<String, String> get(
        String projectName,
        String sourceSetName,
        String workspaceName,
        Provider<Set<SessionCredentials>> creds
    ) {
        def key = new Key(projectName, sourceSetName, workspaceName)
        if (!CREDENTIAL_SESSIONS.containsKey(key)) {
            CREDENTIAL_SESSIONS.put(key, creds.get())
        }
        Set<SessionCredentials> oldSet = CREDENTIAL_SESSIONS.get(key)
        Map<String, String> env = [:]
        Set<SessionCredentials> newSet = Transform.toSet(oldSet) { SessionCredentials it ->
            if (it.expired) {
                SessionCredentials next = it.refresh()
                env.putAll(next.environment)
                next
            } else {
                env.putAll(it.environment)
                it
            }
        }
        CREDENTIAL_SESSIONS.put(key, newSet)
        env
    }

    @SuppressWarnings('UnusedMethodParameter')
    private static class Listener implements BuildListener {
        void buildStarted(Gradle gradle) {
        }

        void settingsEvaluated(Settings settings) {
        }

        void projectsLoaded(Gradle gradle) {
        }

        void projectsEvaluated(Gradle gradle) {
        }

        @Synchronized
        void buildFinished(BuildResult result) {
            CREDENTIAL_SESSIONS.clear()
            log.debug('Terraform credentials cache cleared')
        }
    }

    @EqualsAndHashCode
    @ToString
    private static class Key {
        final String projectName
        final String sourceSetName
        final String workspaceName

        Key(String p, String s, String w) {
            this.projectName = p
            this.sourceSetName = s
            this.workspaceName = w
        }
    }

    @Synchronized
    static void registerListener(Gradle gradle) {
        if (!REGISTERED.get()) {
            REGISTERED.set(true)
            gradle.addBuildListener(new CredentialsCache.Listener())
            log.debug('Terraform credentials cache build listener registered')
        }
    }

    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false)

    private static final ConcurrentHashMap<Key, Set<SessionCredentials>> CREDENTIAL_SESSIONS =
        new ConcurrentHashMap<Key, Set<SessionCredentials>>()

}
