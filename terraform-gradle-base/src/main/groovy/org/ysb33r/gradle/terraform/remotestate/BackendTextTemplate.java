package org.ysb33r.gradle.terraform.remotestate;

/**
 * Represents a text template for creating a backend configuration file.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.12
 */
public interface BackendTextTemplate {

    /**
     * Returns a template based upon backend attributes.
     *
     * @param backendAttributes Attributes can be used to generate the template.
     *
     * @return Text template
     */
    String template(BackendAttributesSpec backendAttributes);
}
