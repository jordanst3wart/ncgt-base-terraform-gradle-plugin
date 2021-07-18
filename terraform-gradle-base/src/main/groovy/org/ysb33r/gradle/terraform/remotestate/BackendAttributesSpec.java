package org.ysb33r.gradle.terraform.remotestate;

import org.gradle.api.provider.Provider;

import java.io.File;
import java.util.Map;

/**
 * Attributes that describe a backend.
 *
 * @author Schalk W. Cronjé
 *
 * @since 1.2
 */
public interface BackendAttributesSpec {
    /**
     * The default template used for the partial configuration of the backend.
     *
     * @return Default template as a string.
     */
    String getDefaultTextTemplate();

    /**
     * Returns location of template file.
     *
     * @return Location of template file if set.
     */
    Provider<File> getTemplateFile();

    /**
     * Returns text template.
     *
     * @return text of template if set.
     */
    Provider<BackendTextTemplate> getTextTemplate();

    /**
     * A provider for all the tokens that were set.
     *
     * @return Provider to a map of tokens.
     */
    Provider<Map<String, ?>> getTokenProvider();

    /**
     * Starting delimiter for tokens.
     * <p>
     * Only useful when a custom template is used.
     *
     * @return Delimiter
     *
     * @deprecated Use {@link #getBeginTokenProvider}.
     */
    @Deprecated
    default String getBeginToken() {
        return getBeginTokenProvider().getOrNull();
    }

    /**
     * Terminating delimiter for tokens
     * <p>
     * Only useful when a custom template is used.
     *
     * @return Delimiter
     *
     * @deprecated Use {@link #getEndTokenProvider}.
     */
    @Deprecated
    default String getEndToken() {
        return getEndTokenProvider().getOrNull();
    }

    /**
     * Starting delimiter for tokens.
     * <p>
     * Only useful when a custom template is used.
     *
     * @return Provider for start delimiter
     */
    Provider<String> getBeginTokenProvider();

    /**
     * Terminating delimiter for tokens
     * <p>
     * Only useful when a custom template is used.
     *
     * @return Provider for end delimiter
     */
    Provider<String> getEndTokenProvider();

    /**
     * Returns the current set of tokens
     *
     * @return Tokens used for replacements.
     */
    Map<String, Object> getTokens();
}
