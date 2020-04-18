/*
 * Copyright 2017-2020 the original author or authors.
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
package org.ysb33r.gradle.terraform.internal;

import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet;
import org.ysb33r.gradle.terraform.internal.remotestate.S3Conventions;

import java.util.function.BiConsumer;

/**
 * Additional tasks that are triggered by specific plugins
 *
 * @author Schalk W. Cronjé
 * @since 0.8.0
 */
public enum SupplementaryConvention {
    REMOTE_STATE_S3(
        "org.ysb33r.terraform.remotestate.s3",
        S3Conventions::taskCreator,
        S3Conventions::taskLazyCreator
    );

    private SupplementaryConvention(
        String pluginId,
        BiConsumer<Project, TerraformSourceDirectorySet> creator,
        BiConsumer<Project, NamedDomainObjectProvider<TerraformSourceDirectorySet>> lazyCreator) {
        this.pluginId = pluginId;
        this.createTasks = creator;
        this.lazyCreateTasks = lazyCreator;
    }

    public final String getPluginId() {
        return pluginId;
    }

    public final BiConsumer<Project, TerraformSourceDirectorySet> getCreateTasks() {
        return createTasks;
    }

    public final BiConsumer<Project, NamedDomainObjectProvider<TerraformSourceDirectorySet>> getLazyCreateTasks() {
        return lazyCreateTasks;
    }

    private final String pluginId;
    private final BiConsumer<Project, TerraformSourceDirectorySet> createTasks;
    private final BiConsumer<Project, NamedDomainObjectProvider<TerraformSourceDirectorySet>> lazyCreateTasks;
}
