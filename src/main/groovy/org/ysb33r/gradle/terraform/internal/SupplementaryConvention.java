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
