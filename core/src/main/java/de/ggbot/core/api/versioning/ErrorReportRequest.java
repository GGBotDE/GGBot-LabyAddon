package de.ggbot.core.api.versioning;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Represents an error-report request sent to {@code POST /v1/error/:addonSlug}.
 *
 * <p>Provides three convenient factory methods:
 * <ul>
 *   <li>{@link #fromException(VersionCheckRequest, Throwable)} – auto-extracts message and stacktrace</li>
 *   <li>{@link #fromCurrentThread(VersionCheckRequest, String)} – captures the current thread's call stack</li>
 *   <li>{@link #fromManual(VersionCheckRequest, String, String)} – fully manual input</li>
 * </ul>
 *
 * <p>All setter methods are fluent (return {@code this}).
 *
 * <p>Example:
 * <pre>{@code
 * try {
 *     // ... addon logic ...
 * } catch (Exception e) {
 *     client.reportError("my-addon", ErrorReportRequest.fromException(checkReq, e));
 * }
 * }</pre>
 */
public class ErrorReportRequest {

    /**
     * Optional client-supplied report UUID. If {@code null}, the server will generate one.
     * Useful for de-duplication on the client side.
     */
    private String id;

    /** Short error message or exception class name, e.g. {@code "NullPointerException"}. */
    private String error;

    /**
     * Full stack trace as a single multi-line string.
     * Lines should be separated by {@code \n}.
     */
    private String stacktrace;

    // --- Client context fields (mirrored from VersionCheckRequest) ---

    /** API protocol version at the time of the error. */
    private String apiVersion;

    /** Installed addon version at the time of the error. */
    private String addonVersion;

    /** Running LabyMod version at the time of the error. */
    private String labymodVersion;

    /** Running Minecraft version at the time of the error. */
    private String minecraftVersion;

    /** Host operating system (lowercase). */
    private String os;

    /** Numeric OS version. */
    private Integer osVersion;

    /** Release channel the user is subscribed to. */
    private String releaseChannel;

    /** Whether OS is officially supported. */
    private boolean isCurrentOsSupported = true;

    /** Whether the request originates from a Flint-enhanced client. */
    private boolean isFlintAddon = false;

    /** File hash of the installed addon at the time of the error. */
    private String fileHash;

    /** Maven dependencies present at the time of the error. */
    private List<String> mavenDependencies = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** Creates an empty {@code ErrorReportRequest}. */
    public ErrorReportRequest() {
    }

    // -------------------------------------------------------------------------
    // Static factory methods
    // -------------------------------------------------------------------------

    /**
     * Creates an {@code ErrorReportRequest} from a caught {@link Throwable}.
     * The exception's message and full stack trace are extracted automatically.
     * Client context is copied from the supplied {@link VersionCheckRequest}.
     *
     * @param context   the version-check request used as context, may be {@code null}
     * @param throwable the caught exception or error
     * @return populated {@code ErrorReportRequest}
     */
    public static ErrorReportRequest fromException(VersionCheckRequest context, Throwable throwable) {
        ErrorReportRequest req = new ErrorReportRequest();
        req.id = UUID.randomUUID().toString();

        if (throwable != null) {
            // Build error message: "ClassName: message"
            String className = throwable.getClass().getName();
            String msg = throwable.getMessage();
            req.error = msg != null ? className + ": " + msg : className;

            // Convert full stack trace to string
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            req.stacktrace = sw.toString();
        }

        if (context != null) {
            req.copyContextFrom(context);
        }
        return req;
    }

    /**
     * Creates an {@code ErrorReportRequest} that captures the current thread's call stack.
     * Useful for reporting non-exception code paths that should be tracked.
     *
     * @param context      the version-check request used as context, may be {@code null}
     * @param errorMessage a short description of the error condition
     * @return populated {@code ErrorReportRequest} with current thread stack
     */
    public static ErrorReportRequest fromCurrentThread(VersionCheckRequest context, String errorMessage) {
        ErrorReportRequest req = new ErrorReportRequest();
        req.id = UUID.randomUUID().toString();
        req.error = errorMessage;

        // Capture current thread stack trace (skip the top frames from this factory method)
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder();
        sb.append("Thread: ").append(Thread.currentThread().getName()).append("\n");
        // Skip [0]=getStackTrace, [1]=fromCurrentThread
        for (int i = 2; i < stack.length; i++) {
            sb.append("\tat ").append(stack[i].toString()).append("\n");
        }
        req.stacktrace = sb.toString();

        if (context != null) {
            req.copyContextFrom(context);
        }
        return req;
    }

    /**
     * Creates an {@code ErrorReportRequest} with fully manual error message and stack trace.
     *
     * @param context    the version-check request used as context, may be {@code null}
     * @param error      short error message
     * @param stacktrace multi-line stack trace string
     * @return populated {@code ErrorReportRequest}
     */
    public static ErrorReportRequest fromManual(VersionCheckRequest context, String error, String stacktrace) {
        ErrorReportRequest req = new ErrorReportRequest();
        req.id = UUID.randomUUID().toString();
        req.error = error;
        req.stacktrace = stacktrace;
        if (context != null) {
            req.copyContextFrom(context);
        }
        return req;
    }

    // -------------------------------------------------------------------------
    // Context copy helper
    // -------------------------------------------------------------------------

    /**
     * Copies all client context fields from a {@link VersionCheckRequest} into this report.
     * Useful when building an error report manually and you already have a check request.
     *
     * @param context source {@link VersionCheckRequest}
     * @return this instance for chaining
     */
    public ErrorReportRequest copyContextFrom(VersionCheckRequest context) {
        this.apiVersion = context.getApiVersion();
        this.addonVersion = context.getAddonVersion();
        this.labymodVersion = context.getLabymodVersion();
        this.minecraftVersion = context.getMinecraftVersion();
        this.os = context.getOs();
        this.osVersion = context.getOsVersion();
        this.releaseChannel = context.getReleaseChannel();
        this.isCurrentOsSupported = context.isCurrentOsSupported();
        this.isFlintAddon = context.isFlintAddon();
        this.fileHash = context.getFileHash();
        this.mavenDependencies = new ArrayList<>(context.getMavenDependencies());
        return this;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Returns the optional client-supplied report UUID.
     *
     * @return UUID string, or {@code null} if not set
     */
    public String getId() { return id; }

    /**
     * Returns the short error message.
     *
     * @return error message string, or {@code null}
     */
    public String getError() { return error; }

    /**
     * Returns the full stack trace string.
     *
     * @return multi-line stack trace, or {@code null}
     */
    public String getStacktrace() { return stacktrace; }

    /**
     * Returns the API version used at the time of the error.
     *
     * @return API version string
     */
    public String getApiVersion() { return apiVersion; }

    /**
     * Returns the addon version installed at the time of the error.
     *
     * @return addon version string
     */
    public String getAddonVersion() { return addonVersion; }

    /**
     * Returns the LabyMod version running at the time of the error.
     *
     * @return LabyMod version string
     */
    public String getLabymodVersion() { return labymodVersion; }

    /**
     * Returns the Minecraft version running at the time of the error.
     *
     * @return Minecraft version string
     */
    public String getMinecraftVersion() { return minecraftVersion; }

    /**
     * Returns the host operating system identifier.
     *
     * @return OS string (lowercase)
     */
    public String getOs() { return os; }

    /**
     * Returns the numeric OS version.
     *
     * @return OS version number, or {@code null}
     */
    public Integer getOsVersion() { return osVersion; }

    /**
     * Returns the release channel.
     *
     * @return release channel string
     */
    public String getReleaseChannel() { return releaseChannel; }

    /**
     * Returns whether the OS was officially supported.
     *
     * @return {@code true} if supported
     */
    public boolean isCurrentOsSupported() { return isCurrentOsSupported; }

    /**
     * Returns whether the request was from a Flint-enhanced client.
     *
     * @return {@code true} if Flint context
     */
    public boolean isFlintAddon() { return isFlintAddon; }

    /**
     * Returns the file hash of the installed addon.
     *
     * @return file hash string, or {@code null}
     */
    public String getFileHash() { return fileHash; }

    /**
     * Returns an unmodifiable view of the Maven dependencies list.
     *
     * @return list of dependency coordinate strings
     */
    public List<String> getMavenDependencies() { return Collections.unmodifiableList(mavenDependencies); }

    // -------------------------------------------------------------------------
    // Setters (fluent)
    // -------------------------------------------------------------------------

    /**
     * Sets the client-supplied report UUID for de-duplication.
     *
     * @param id UUID string
     * @return this instance for chaining
     */
    public ErrorReportRequest setId(String id) { this.id = id; return this; }

    /**
     * Sets the short error message.
     *
     * @param error error description string
     * @return this instance for chaining
     */
    public ErrorReportRequest setError(String error) { this.error = error; return this; }

    /**
     * Sets the full stack trace string.
     *
     * @param stacktrace multi-line stack trace
     * @return this instance for chaining
     */
    public ErrorReportRequest setStacktrace(String stacktrace) { this.stacktrace = stacktrace; return this; }

    /**
     * Sets the API version.
     *
     * @param apiVersion API version string
     * @return this instance for chaining
     */
    public ErrorReportRequest setApiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

    /**
     * Sets the addon version.
     *
     * @param addonVersion version string
     * @return this instance for chaining
     */
    public ErrorReportRequest setAddonVersion(String addonVersion) { this.addonVersion = addonVersion; return this; }

    /**
     * Sets the LabyMod version.
     *
     * @param labymodVersion version string
     * @return this instance for chaining
     */
    public ErrorReportRequest setLabymodVersion(String labymodVersion) { this.labymodVersion = labymodVersion; return this; }

    /**
     * Sets the Minecraft version.
     *
     * @param minecraftVersion version string
     * @return this instance for chaining
     */
    public ErrorReportRequest setMinecraftVersion(String minecraftVersion) { this.minecraftVersion = minecraftVersion; return this; }

    /**
     * Sets the OS identifier (will be lowercased automatically).
     *
     * @param os OS string
     * @return this instance for chaining
     */
    public ErrorReportRequest setOs(String os) { this.os = os != null ? os.toLowerCase() : null; return this; }

    /**
     * Sets the numeric OS version.
     *
     * @param osVersion OS version number
     * @return this instance for chaining
     */
    public ErrorReportRequest setOsVersion(int osVersion) { this.osVersion = osVersion; return this; }

    /**
     * Sets the release channel.
     *
     * @param releaseChannel release channel string
     * @return this instance for chaining
     */
    public ErrorReportRequest setReleaseChannel(String releaseChannel) { this.releaseChannel = releaseChannel; return this; }

    /**
     * Sets whether the OS is officially supported.
     *
     * @param isCurrentOsSupported {@code true} if supported
     * @return this instance for chaining
     */
    public ErrorReportRequest setIsCurrentOsSupported(boolean isCurrentOsSupported) {
        this.isCurrentOsSupported = isCurrentOsSupported;
        return this;
    }

    /**
     * Sets whether this is a Flint client context.
     *
     * @param isFlintAddon {@code true} if Flint context
     * @return this instance for chaining
     */
    public ErrorReportRequest setIsFlintAddon(boolean isFlintAddon) { this.isFlintAddon = isFlintAddon; return this; }

    /**
     * Sets the addon file hash.
     *
     * @param fileHash hex-encoded hash string
     * @return this instance for chaining
     */
    public ErrorReportRequest setFileHash(String fileHash) { this.fileHash = fileHash; return this; }

    /**
     * Replaces the Maven dependencies list.
     *
     * @param mavenDependencies list of coordinate strings
     * @return this instance for chaining
     */
    public ErrorReportRequest setMavenDependencies(List<String> mavenDependencies) {
        this.mavenDependencies = mavenDependencies != null ? new ArrayList<>(mavenDependencies) : new ArrayList<>();
        return this;
    }

    /**
     * Appends a single Maven dependency.
     *
     * @param dependency coordinate string
     * @return this instance for chaining
     */
    public ErrorReportRequest addMavenDependency(String dependency) {
        this.mavenDependencies.add(dependency);
        return this;
    }

    // -------------------------------------------------------------------------
    // Serialisation
    // -------------------------------------------------------------------------

    /**
     * Serialises this error report to a minimal JSON string for the HTTP request body.
     * Uses only standard Java APIs.
     *
     * @return JSON representation of this error report
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder("{");
        appendField(sb, "id", id);
        appendField(sb, "error", error);
        appendField(sb, "stacktrace", stacktrace);
        appendField(sb, "apiVersion", apiVersion);
        appendField(sb, "addonVersion", addonVersion);
        appendField(sb, "labymodVersion", labymodVersion);
        appendField(sb, "minecraftVersion", minecraftVersion);
        appendField(sb, "os", os);
        if (osVersion != null) sb.append("\"osVersion\":").append(osVersion).append(",");
        appendField(sb, "releaseChannel", releaseChannel);
        appendField(sb, "fileHash", fileHash);
        sb.append("\"isCurrentOsSupported\":").append(isCurrentOsSupported).append(",");
        sb.append("\"isFlintAddon\":").append(isFlintAddon).append(",");
        sb.append("\"mavenDependencies\":[");
        for (int i = 0; i < mavenDependencies.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escape(mavenDependencies.get(i))).append("\"");
        }
        sb.append("]");
        sb.append("}");
        return sb.toString().replace(",}", "}").replace(",]", "]");
    }

    /** Appends a JSON string field, skipping nulls. */
    private static void appendField(StringBuilder sb, String key, String value) {
        if (value == null) return;
        sb.append("\"").append(key).append("\":\"").append(escape(value)).append("\",");
    }

    /** Escapes special characters for JSON strings. */
    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    public String toString() {
        return "ErrorReportRequest{id='" + id + "', error='" + error + "', addonVersion='" + addonVersion + "'}";
    }
}
