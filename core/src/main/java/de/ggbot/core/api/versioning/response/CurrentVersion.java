package de.ggbot.core.api.versioning.response;

/**
 * Represents the latest available version information returned inside a
 * {@link de.ggbot.core.api.versioning.VersionCheckResponse}.
 *
 * <p>Fields map directly to the {@code currentVersion} object in the API response JSON.
 */
public class CurrentVersion {

    /** The latest version string, e.g. {@code "1.4.0"}. */
    private String version;

    /** SHA-256 / MD5 hash of the latest addon file. */
    private String fileHash;

    /** URL for manual (browser) download of the latest version. May be empty. */
    private String manualDownload;

    /** Direct download URL for the latest version file. May be empty. */
    private String manualDownloadDirect;

    /** Whether this version has been officially released on the LabyMod marketplace. */
    private boolean isOfficiallyReleased;

    /** Whether this version has been released via the Flint platform. */
    private boolean isFlintReleased;

    /** Whether this version will eventually be released via the Flint platform. */
    private boolean willBeFlintReleased;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /** Creates an empty {@code CurrentVersion} instance. All fields default to empty/false. */
    public CurrentVersion() {
    }

    /**
     * Creates a fully initialised {@code CurrentVersion} instance.
     *
     * @param version               latest version string
     * @param fileHash              file hash of the latest version
     * @param manualDownload        URL for manual browser download
     * @param manualDownloadDirect  direct download URL
     * @param isOfficiallyReleased  whether officially released
     * @param isFlintReleased       whether released via Flint
     * @param willBeFlintReleased   whether planned for Flint release
     */
    public CurrentVersion(String version, String fileHash, String manualDownload,
                          String manualDownloadDirect, boolean isOfficiallyReleased,
                          boolean isFlintReleased, boolean willBeFlintReleased) {
        this.version = version;
        this.fileHash = fileHash;
        this.manualDownload = manualDownload;
        this.manualDownloadDirect = manualDownloadDirect;
        this.isOfficiallyReleased = isOfficiallyReleased;
        this.isFlintReleased = isFlintReleased;
        this.willBeFlintReleased = willBeFlintReleased;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Returns the latest version string.
     *
     * @return version string, e.g. {@code "1.4.0"}
     */
    public String getVersion() { return version; }

    /**
     * Returns the file hash of the latest addon release.
     *
     * @return hex-encoded hash string
     */
    public String getFileHash() { return fileHash; }

    /**
     * Returns the URL for manual browser-based download.
     *
     * @return URL string, may be empty
     */
    public String getManualDownload() { return manualDownload; }

    /**
     * Returns the direct (automated) download URL for the latest file.
     *
     * @return URL string, may be empty
     */
    public String getManualDownloadDirect() { return manualDownloadDirect; }

    /**
     * Returns whether this version has been officially released on the LabyMod marketplace.
     *
     * @return {@code true} if officially released
     */
    public boolean isOfficiallyReleased() { return isOfficiallyReleased; }

    /**
     * Returns whether this version has been released via the Flint platform.
     *
     * @return {@code true} if released on Flint
     */
    public boolean isFlintReleased() { return isFlintReleased; }

    /**
     * Returns whether this version will eventually be published on the Flint platform.
     *
     * @return {@code true} if a Flint release is planned
     */
    public boolean isWillBeFlintReleased() { return willBeFlintReleased; }

    // -------------------------------------------------------------------------
    // Setters
    // -------------------------------------------------------------------------

    /**
     * Sets the latest version string.
     *
     * @param version version string
     * @return this instance for chaining
     */
    public CurrentVersion setVersion(String version) { this.version = version; return this; }

    /**
     * Sets the file hash of the latest addon release.
     *
     * @param fileHash hex-encoded hash string
     * @return this instance for chaining
     */
    public CurrentVersion setFileHash(String fileHash) { this.fileHash = fileHash; return this; }

    /**
     * Sets the manual browser download URL.
     *
     * @param manualDownload URL string
     * @return this instance for chaining
     */
    public CurrentVersion setManualDownload(String manualDownload) { this.manualDownload = manualDownload; return this; }

    /**
     * Sets the direct download URL.
     *
     * @param manualDownloadDirect URL string
     * @return this instance for chaining
     */
    public CurrentVersion setManualDownloadDirect(String manualDownloadDirect) { this.manualDownloadDirect = manualDownloadDirect; return this; }

    /**
     * Sets whether the version is officially released.
     *
     * @param officiallyReleased {@code true} if officially released
     * @return this instance for chaining
     */
    public CurrentVersion setOfficiallyReleased(boolean officiallyReleased) { this.isOfficiallyReleased = officiallyReleased; return this; }

    /**
     * Sets whether the version has been released on Flint.
     *
     * @param flintReleased {@code true} if released on Flint
     * @return this instance for chaining
     */
    public CurrentVersion setFlintReleased(boolean flintReleased) { this.isFlintReleased = flintReleased; return this; }

    /**
     * Sets whether the version is planned for a Flint release.
     *
     * @param willBeFlintReleased {@code true} if planned
     * @return this instance for chaining
     */
    public CurrentVersion setWillBeFlintReleased(boolean willBeFlintReleased) { this.willBeFlintReleased = willBeFlintReleased; return this; }

    @Override
    public String toString() {
        return "CurrentVersion{version='" + version + "', fileHash='" + fileHash +
               "', isOfficiallyReleased=" + isOfficiallyReleased + '}';
    }
}
