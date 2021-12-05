package org.ysb33r.gradle.terraform.config;

import groovy.lang.Closure;

import java.util.Collections;
import java.util.List;

/**
 * An extension that can be added to a task for representing a specific grouping of Terraform
 * command-line parameters.
 *
 * @author Schalk W. Cronjé
 */
public interface TerraformTaskConfigExtension {
    /**
     * The name under which the extension should be created.
     *
     * @return Name of the extension
     */
    String getName();

    /**
     * Returns a list of closures which can be used to determine an input property for the purposes of
     * up to date calculations.
     * <p>
     * Closures should return objects that are serializable.
     *
     * @return Property closures. Can be empty (but never {@code null}) which means that the extension holds no
     * properties that should be used fot up to date calculations.
     */
    List<Closure> getInputProperties();

    /**
     * Returns the list of Terraform command-line arguments.
     *
     * @return List of arguments to be added. Can be empty, but never {@code null}
     */
    List<String> getCommandLineArgs();

    /**
     * Returns the list of Terraform variables in the form name=value
     *
     * @return Terraform variables
     *
     * @since 0.13
     */
    default List<String> getTfVars() {
        return Collections.EMPTY_LIST;
    }
}
