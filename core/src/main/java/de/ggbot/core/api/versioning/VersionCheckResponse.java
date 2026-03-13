package de.ggbot.core.api.versioning;

import de.ggbot.core.api.versioning.response.CurrentVersion;
import de.ggbot.core.api.versioning.response.FeatureFlagResponse;
import de.ggbot.core.api.versioning.response.MessageResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents the full response returned by {@code POST /v1/check/:addonSlug}.
 *
 * <p>Instances of this class are created by
 * {@link VersioningApiClient#check(String, VersionCheckRequest)} after deserialising
 * the JSON response body.
 *
 * <p>Example usage:
 * <pre>{@code
 * client.check("my-addon", req).thenAccept(resp -> {
 *     if (!resp.isSupported()) {
 *         System.out.println("Addon not supported: " + resp.getMessage());
 *     }
 *     if (resp.isUpdateAvailable(req.getAddonVersion())) {
 *         System.out.println("Update to " + resp.getCurrentVersion().getVersion());
 *     }
 * });
 * }</pre>
 */
public class VersionCheckResponse {

    /**
     * Whether the addon is supported in the current context.
     * If {@code false}, the addon should disable itself or show a message.
     */
    private boolean supported;

    /**
     * Optional human-readable message from the server explaining the support status.
     * May be {@code null} if no message was provided.
     */
    private String message;

    /** Information about the latest available version of the addon. */
    private CurrentVersion currentVersion;

    /**
     * Map of feature-flag key → enabled state for the requesting context.
     * Keys are defined server-side, e.g. {@code "addon.feature.new_ui"}.
     */
    private Map<String, FeatureFlagResponse> features = Collections.emptyMap();

    /**
     * List of messages/announcements to show to the user in the LabyMod client.
     * Empty if no messages are applicable to the current context.
     */
    private List<MessageResponse> messages = Collections.emptyList();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** Creates an empty {@code VersionCheckResponse}. */
    public VersionCheckResponse() {
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Returns whether the addon is supported in the requesting client's context.
     *
     * @return {@code true} if the addon should be active
     */
    public boolean isSupported() { return supported; }

    /**
     * Returns the optional server-provided support status message.
     *
     * @return message string, or {@code null}
     */
    public String getMessage() { return message; }

    /**
     * Returns information about the latest available addon version.
     *
     * @return {@link CurrentVersion} instance (never {@code null} after deserialisation)
     */
    public CurrentVersion getCurrentVersion() { return currentVersion; }

    /**
     * Returns an unmodifiable map of feature-flag states for this context.
     *
     * @return feature flags map, keyed by feature flag string identifier
     */
    public Map<String, FeatureFlagResponse> getFeatures() { return Collections.unmodifiableMap(features); }

    /**
     * Returns the feature flag response for a specific key.
     *
     * @param featureKey the feature flag identifier, e.g. {@code "addon.feature.new_ui"}
     * @return the {@link FeatureFlagResponse}, or {@code null} if not present
     */
    public FeatureFlagResponse getFeature(String featureKey) { return features.get(featureKey); }

    /**
     * Convenience method: returns whether the named feature flag is enabled.
     * Returns {@code false} if the key is unknown.
     *
     * @param featureKey the feature flag identifier
     * @return {@code true} if the feature is enabled
     */
    public boolean isFeatureEnabled(String featureKey) {
        FeatureFlagResponse f = features.get(featureKey);
        return f != null && f.isEnabled();
    }

    /**
     * Returns an unmodifiable list of messages to display to the user.
     *
     * @return list of {@link MessageResponse} objects
     */
    public List<MessageResponse> getMessages() { return Collections.unmodifiableList(messages); }

    /**
     * Convenience method: returns {@code true} if an update is available.
     * Compares the installed version string against the server's current version
     * using a simple string inequality check (not semver-aware).
     * For semver-aware comparison, use the version strings with a semver library.
     *
     * @param installedVersion the currently installed version string
     * @return {@code true} if the server reports a different (presumably newer) version
     */
    public boolean isUpdateAvailable(String installedVersion) {
        if (currentVersion == null || currentVersion.getVersion() == null) return false;
        return !currentVersion.getVersion().equals(installedVersion);
    }

    // -------------------------------------------------------------------------
    // Setters
    // -------------------------------------------------------------------------

    /**
     * Sets the supported flag.
     *
     * @param supported {@code true} if addon is supported
     * @return this instance for chaining
     */
    public VersionCheckResponse setSupported(boolean supported) { this.supported = supported; return this; }

    /**
     * Sets the support status message.
     *
     * @param message human-readable message, may be {@code null}
     * @return this instance for chaining
     */
    public VersionCheckResponse setMessage(String message) { this.message = message; return this; }

    /**
     * Sets the current version information.
     *
     * @param currentVersion {@link CurrentVersion} instance
     * @return this instance for chaining
     */
    public VersionCheckResponse setCurrentVersion(CurrentVersion currentVersion) { this.currentVersion = currentVersion; return this; }

    /**
     * Sets the feature flags map.
     *
     * @param features map of feature key → {@link FeatureFlagResponse}
     * @return this instance for chaining
     */
    public VersionCheckResponse setFeatures(Map<String, FeatureFlagResponse> features) {
        this.features = features != null ? features : Collections.emptyMap();
        return this;
    }

    /**
     * Sets the messages list.
     *
     * @param messages list of {@link MessageResponse}
     * @return this instance for chaining
     */
    public VersionCheckResponse setMessages(List<MessageResponse> messages) {
        this.messages = messages != null ? messages : Collections.emptyList();
        return this;
    }

    @Override
    public String toString() {
        return "VersionCheckResponse{supported=" + supported +
               ", message='" + message +
               "', currentVersion=" + currentVersion + '}';
    }
}
