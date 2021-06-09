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
import org.gradle.api.provider.Provider
import org.ysb33r.gradle.terraform.credentials.SessionCredentials
import org.ysb33r.grolifant.api.core.ProjectOperations

import java.util.concurrent.ConcurrentHashMap

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

    CredentialsCache(ProjectOperations po) {
        this.projectName = po.projectName
    }

    @Synchronized
    Map<String, String> get(
        String sourceSetName,
        String workspaceName,
        Provider<Set<SessionCredentials>> creds
    ) {
        def key = new Key(projectName, sourceSetName, workspaceName)
        if (!credentialSessions.containsKey(key)) {
            credentialSessions.put(key, creds.get())
        }
        Set<SessionCredentials> oldSet = credentialSessions.get(key)
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
        credentialSessions.put(key, newSet)
        env
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

    private final String projectName
    private final ConcurrentHashMap<Key, Set<SessionCredentials>> credentialSessions =
        new ConcurrentHashMap<Key, Set<SessionCredentials>>()
}
