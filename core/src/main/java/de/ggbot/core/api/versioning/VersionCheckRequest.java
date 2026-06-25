package de.ggbot.core.api.versioning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a version-check request sent to {@code POST /v1/check/:addonSlug}.
 *
 * <p>All setter methods return {@code this} to support a fluent builder-style API:
 * <pre>{@code
 * VersionCheckRequest req = new VersionCheckRequest()
 *     .setAddonVersion("1.3.2")
 *     .setLabymodVersion("4.1")
 *     .setOs("windows");
 * }</pre>
 *
 * <p>Fields that are not set will be serialised as {@code null} (omitted from JSON)
 * or their documented defaults.
 *
 * @LabyMod
 * All the information sent below has to be sent because we need to know the environment of the user and all other
 * information seen below to determine if the addon, or specific features within are compatible with it.
 * We do not offer API versioning at this time, so we need to be able to disable or reroute specific features if conditions are met.
 * Also, we want to be able to disable any features if any security issues arise on specific versions / operating systems / labymod versions / minecraft versions
 * and so on and possibly notify the user that certain features are disabled.
 * We do not collect any identifying information about the user.
 */
public class VersionCheckRequest {

    /**
     * The API protocol version understood by this client.
     * Defaults to {@code "2"}.
     */
    private String apiVersion = "2";

    /** The currently installed addon version (semver string, e.g. {@code "1.3.2"}). */
    private String addonVersion;

    /** The running LabyMod client version (e.g. {@code "4.1"}). */
    private String labymodVersion;

    /** The running Minecraft version (e.g. {@code "1.20.1"}). */
    private String minecraftVersion;

    /**
     * The host operating system, lowercase.
     * Accepted values: {@code "windows"}, {@code "linux"}, {@code "mac"}.
     */
    private String os;

    /** Numeric OS version (e.g. {@code 10} for Windows 10). */
    private Integer osVersion;

    /** SHA-256 / MD5 hash of the locally installed addon file. */
    private String fileHash;

    /**
     * List of Maven dependency coordinates present in the client's classpath.
     * Format: {@code "groupId:artifactId:version"}.
     */
    private List<String> mavenDependencies = new ArrayList<>();

    /**
     * The release channel the user is subscribed to.
     * Accepted values: {@code "release"}, {@code "beta"}, {@code "alpha"}.
     * Defaults to {@code "release"}.
     */
    private String releaseChannel = "release";

    /**
     * Whether the current operating system is officially supported by this addon.
     * Defaults to {@code true}.
     */
    private boolean isCurrentOsSupported = true;

    /**
     * Whether the request originates from a Flint-enhanced LabyMod client.
     * Defaults to {@code false}.
     */
    private boolean isFlintAddon = false;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** Creates a {@code VersionCheckRequest} with all default values. */
    public VersionCheckRequest() {
    }

    /**
     * Creates a {@code VersionCheckRequest} with the most common fields pre-filled.
     *
     * @param addonVersion     installed addon version
     * @param labymodVersion   running LabyMod version
     * @param minecraftVersion running Minecraft version
     * @param os               host operating system (lowercase)
     */
    public VersionCheckRequest(String addonVersion, String labymodVersion,
                               String minecraftVersion, String os) {
        this.addonVersion = addonVersion;
        this.labymodVersion = labymodVersion;
        this.minecraftVersion = minecraftVersion;
        this.os = os;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Returns the API protocol version.
     *
     * @return API version string (default {@code "2"})
     */
    public String getApiVersion() {
        return apiVersion;
    }

    /**
     * Returns the installed addon version.
     *
     * @return addon version string, or {@code null} if not set
     */
    public String getAddonVersion() {
        return addonVersion;
    }

    /**
     * Returns the running LabyMod client version.
     *
     * @return LabyMod version string, or {@code null} if not set
     */
    public String getLabymodVersion() {
        return labymodVersion;
    }

    /**
     * Returns the running Minecraft version.
     *
     * @return Minecraft version string, or {@code null} if not set
     */
    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    /**
     * Returns the host operating system identifier.
     *
     * @return OS string (e.g. {@code "windows"}), or {@code null} if not set
     */
    public String getOs() {
        return os;
    }

    /**
     * Returns the numeric OS version.
     *
     * @return OS version number, or {@code null} if not set
     */
    public Integer getOsVersion() {
        return osVersion;
    }

    /**
     * Returns the file hash of the locally installed addon.
     *
     * @return file hash string, or {@code null} if not set
     */
    public String getFileHash() {
        return fileHash;
    }

    /**
     * Returns an unmodifiable view of the Maven dependencies list.
     *
     * @return list of Maven dependency coordinates (never {@code null})
     */
    public List<String> getMavenDependencies() {
        return Collections.unmodifiableList(mavenDependencies);
    }

    /**
     * Returns the release channel the user is subscribed to.
     *
     * @return release channel string (default {@code "release"})
     */
    public String getReleaseChannel() {
        return releaseChannel;
    }

    /**
     * Returns whether the current OS is officially supported.
     *
     * @return {@code true} if the OS is supported (default)
     */
    public boolean isCurrentOsSupported() {
        return isCurrentOsSupported;
    }

    /**
     * Returns whether the request originates from a Flint-enhanced client.
     *
     * @return {@code true} if Flint addon context
     */
    public boolean isFlintAddon() {
        return isFlintAddon;
    }

    // -------------------------------------------------------------------------
    // Setters (fluent)
    // -------------------------------------------------------------------------

    /**
     * Sets the API protocol version.
     *
     * @param apiVersion API version string (default {@code "2"})
     * @return this instance for chaining
     */
    public VersionCheckRequest setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    /**
     * Sets the installed addon version.
     *
     * @param addonVersion semver string, e.g. {@code "1.3.2"}
     * @return this instance for chaining
     */
    public VersionCheckRequest setAddonVersion(String addonVersion) {
        this.addonVersion = addonVersion;
        return this;
    }

    /**
     * Sets the running LabyMod client version.
     *
     * @param labymodVersion version string, e.g. {@code "4.1"}
     * @return this instance for chaining
     */
    public VersionCheckRequest setLabymodVersion(String labymodVersion) {
        this.labymodVersion = labymodVersion;
        return this;
    }

    /**
     * Sets the running Minecraft version.
     *
     * @param minecraftVersion version string, e.g. {@code "1.20.1"}
     * @return this instance for chaining
     */
    public VersionCheckRequest setMinecraftVersion(String minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
        return this;
    }

    /**
     * Sets the host operating system identifier (will be lowercased automatically).
     *
     * @param os OS identifier, e.g. {@code "windows"}, {@code "linux"}, {@code "mac"}
     * @return this instance for chaining
     */
    public VersionCheckRequest setOs(String os) {
        this.os = os != null ? os.toLowerCase() : null;
        return this;
    }

    /**
     * Sets the numeric OS version.
     *
     * @param osVersion numeric version, e.g. {@code 10} for Windows 10
     * @return this instance for chaining
     */
    public VersionCheckRequest setOsVersion(int osVersion) {
        this.osVersion = osVersion;
        return this;
    }

    /**
     * Sets the file hash of the locally installed addon file.
     *
     * @param fileHash hex-encoded hash string
     * @return this instance for chaining
     */
    public VersionCheckRequest setFileHash(String fileHash) {
        this.fileHash = fileHash;
        return this;
    }

    /**
     * Replaces the Maven dependencies list.
     *
     * @param mavenDependencies list of {@code "groupId:artifactId:version"} strings
     * @return this instance for chaining
     * @throws IllegalArgumentException if {@code mavenDependencies} is {@code null}
     */
    public VersionCheckRequest setMavenDependencies(List<String> mavenDependencies) {
        if (mavenDependencies == null) throw new IllegalArgumentException("mavenDependencies must not be null");
        this.mavenDependencies = new ArrayList<>(mavenDependencies);
        return this;
    }

    /**
     * Appends a single Maven dependency coordinate to the list.
     *
     * @param dependency {@code "groupId:artifactId:version"} string
     * @return this instance for chaining
     */
    public VersionCheckRequest addMavenDependency(String dependency) {
        this.mavenDependencies.add(dependency);
        return this;
    }

    /**
     * Sets the release channel.
     *
     * @param releaseChannel one of {@code "release"}, {@code "beta"}, {@code "alpha"}
     * @return this instance for chaining
     */
    public VersionCheckRequest setReleaseChannel(String releaseChannel) {
        this.releaseChannel = releaseChannel;
        return this;
    }

    /**
     * Sets whether the current OS is officially supported by this addon.
     *
     * @param isCurrentOsSupported {@code true} if supported
     * @return this instance for chaining
     */
    public VersionCheckRequest setIsCurrentOsSupported(boolean isCurrentOsSupported) {
        this.isCurrentOsSupported = isCurrentOsSupported;
        return this;
    }

    /**
     * Sets whether the request originates from a Flint-enhanced client.
     *
     * @param isFlintAddon {@code true} if Flint context
     * @return this instance for chaining
     */
    public VersionCheckRequest setIsFlintAddon(boolean isFlintAddon) {
        this.isFlintAddon = isFlintAddon;
        return this;
    }

    // -------------------------------------------------------------------------
    // Serialisation helper
    // -------------------------------------------------------------------------

    /**
     * Serialises this request to a minimal JSON string suitable for the HTTP request body.
     * Uses only standard Java APIs (no external libraries).
     *
     * @return JSON string representation of this request
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder("{");
        appendField(sb, "apiVersion", apiVersion);
        appendField(sb, "addonVersion", addonVersion);
        appendField(sb, "labymodVersion", labymodVersion);
        appendField(sb, "minecraftVersion", minecraftVersion);
        appendField(sb, "os", os);
        if (osVersion != null) {
            sb.append("\"osVersion\":").append(osVersion).append(",");
        }
        appendField(sb, "fileHash", fileHash);
        appendField(sb, "releaseChannel", releaseChannel);
        sb.append("\"isCurrentOsSupported\":").append(isCurrentOsSupported).append(",");
        sb.append("\"isFlintAddon\":").append(isFlintAddon).append(",");

        // Maven dependencies array
        sb.append("\"mavenDependencies\":[");
        for (int i = 0; i < mavenDependencies.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escape(mavenDependencies.get(i))).append("\"");
        }
        sb.append("]");

        // Remove trailing comma before array - already handled above
        sb.append("}");

        // Clean up any trailing commas before closing braces
        return sb.toString().replace(",}", "}").replace(",]", "]");
    }

    /**
     * Appends a JSON string field to a {@link StringBuilder}, skipping {@code null} values.
     *
     * @param sb    target builder
     * @param key   JSON field name
     * @param value string value, may be {@code null}
     */
    private static void appendField(StringBuilder sb, String key, String value) {
        if (value == null) return;
        sb.append("\"").append(key).append("\":\"").append(escape(value)).append("\",");
    }

    /**
     * Escapes special characters in a string for safe JSON embedding.
     *
     * @param s raw string
     * @return JSON-safe escaped string
     */
    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    public String toString() {
        return "VersionCheckRequest{" +
               "apiVersion='" + apiVersion + '\'' +
               ", addonVersion='" + addonVersion + '\'' +
               ", labymodVersion='" + labymodVersion + '\'' +
               ", minecraftVersion='" + minecraftVersion + '\'' +
               ", os='" + os + '\'' +
               ", osVersion=" + osVersion +
               ", releaseChannel='" + releaseChannel + '\'' +
               ", isFlintAddon=" + isFlintAddon +
               '}';
    }
}
