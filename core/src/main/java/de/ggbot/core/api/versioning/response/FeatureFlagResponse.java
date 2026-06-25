package de.ggbot.core.api.versioning.response;

/**
 * Represents a single feature-flag entry inside the {@code features} map of a
 * {@link de.ggbot.core.api.versioning.VersionCheckResponse}.
 *
 * <p>Example JSON:
 * <pre>{@code
 * "addon.feature.new_ui": {
 *   "enabled": true,
 *   "versionCompatabilityConversion": { "path": "https://compat.example.com/v1/feature/" }
 * }
 * }</pre>
 */
public class FeatureFlagResponse {

    /** Whether the feature flag is enabled for the requesting client context. */
    private boolean enabled;

    /**
     * Optional URL used for version-compatibility shim routing.
     * May be {@code null} if no conversion is required.
     */
    private String versionCompatabilityConversionPath;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** Creates an empty {@code FeatureFlagResponse}. */
    public FeatureFlagResponse() {
    }

    /**
     * Creates a {@code FeatureFlagResponse} with enabled state and optional compat URL.
     *
     * @param enabled                          whether the flag is enabled
     * @param versionCompatabilityConversionPath optional compat shim URL, may be {@code null}
     */
    public FeatureFlagResponse(boolean enabled, String versionCompatabilityConversionPath) {
        this.enabled = enabled;
        this.versionCompatabilityConversionPath = versionCompatabilityConversionPath;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Returns whether this feature flag is enabled for the requesting context.
     *
     * @return {@code true} if enabled
     */
    public boolean isEnabled() { return enabled; }

    /**
     * Returns the version-compatibility conversion path URL if present.
     * When non-null, API calls for this feature should be proxied to this URL.
     *
     * @return compat URL string, or {@code null}
     */
    public String getVersionCompatabilityConversionPath() { return versionCompatabilityConversionPath; }

    /**
     * Convenience method: returns {@code true} if a version compatibility conversion URL is set.
     *
     * @return {@code true} if compat URL is present
     */
    public boolean hasVersionCompatabilityConversion() { return versionCompatabilityConversionPath != null; }

    // -------------------------------------------------------------------------
    // Setters
    // -------------------------------------------------------------------------

    /**
     * Sets the enabled state.
     *
     * @param enabled {@code true} to enable the feature flag
     * @return this instance for chaining
     */
    public FeatureFlagResponse setEnabled(boolean enabled) { this.enabled = enabled; return this; }

    /**
     * Sets the version-compatibility conversion path URL.
     *
     * @param path URL string, or {@code null} to clear
     * @return this instance for chaining
     */
    public FeatureFlagResponse setVersionCompatabilityConversionPath(String path) {
        this.versionCompatabilityConversionPath = path;
        return this;
    }

    @Override
    public String toString() {
        return "FeatureFlagResponse{enabled=" + enabled +
               ", compatPath=" + versionCompatabilityConversionPath + '}';
    }
}
