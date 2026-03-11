package de.ggbot.core.api;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.versioning.ErrorReportRequest;
import de.ggbot.core.api.versioning.VersionCheckRequest;
import de.ggbot.core.api.versioning.VersionCheckResponse;
import de.ggbot.core.api.versioning.VersioningApiClient;
import net.labymod.api.models.addon.info.dependency.MavenDependency;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

/**
 * Handles communication with the remote versioning API.
 *
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>Checking if the addon version is up to date</li>
 *     <li>Sending runtime error reports to the backend</li>
 * </ul>
 */
public class VersioningHandler {

  /** Base API endpoint for the versioning service */
  private static final String BASE_URL = "https://labyversion.ggbot.de";

  /** Addon identifier used by the backend */
  private static final String ADDON_SLUG = "labymod-addon";

  /** Current API version used by the addon */
  private static final String API_VERSION = "0.14.3";

  /** Reference to the addon instance */
  private final GGBot addon;

  /** Client responsible for performing API requests */
  private final VersioningApiClient versioningApiClient;

  /** Cached response of the version check */
  private VersionCheckResponse versionCheckResponse;

  /**
   * Creates a new {@link VersioningHandler}.
   * Immediately performs a version check on creation.
   *
   * @param addon the addon instance
   */
  public VersioningHandler(GGBot addon) {
    this.addon = addon;
    this.versioningApiClient = new VersioningApiClient(BASE_URL);

    checkVersion();
  }

  /**
   * Converts a {@link MavenDependency} to a serialized string representation.
   *
   * <p>Format:</p>
   * <pre>
   * repo:group:name:version[:classifier]
   * </pre>
   *
   * @param dependency the dependency to convert
   * @return serialized dependency string
   */
  private String convertMavenDependencyToString(MavenDependency dependency) {
    String base = String.join(
        ":",
        dependency.getRepo(),
        dependency.getGroup(),
        dependency.getName(),
        dependency.getVersion()
    );

    if (dependency.getClassifier() != null) {
      base += ":" + dependency.getClassifier();
    }

    return base;
  }

  /**
   * Determines the operating system version.
   *
   * <p>
   * Windows 11 requires special handling since it reports
   * itself as Windows 10 but has a build number ≥ 22000.
   * </p>
   *
   * @return normalized OS version number
   */
  private int getOSVersion() {
    String osName = System.getProperty("os.name").toLowerCase();
    String osVersion = System.getProperty("os.version");

    if (osName.contains("win")) {
      String[] parts = osVersion.split("\\.");

      // Windows 11 detection (build number >= 22000)
      if (parts.length > 2 && Integer.parseInt(parts[2]) >= 22000) {
        return 11;
      }

      return Integer.parseInt(parts[0]);
    }

    return Integer.parseInt(osVersion.split("\\.")[0]);
  }

  /**
   * Returns the normalized OS name used by the API.
   *
   * @return normalized OS name
   */
  private String getOSName() {
    String osName = System.getProperty("os.name").toLowerCase();
    return osName.contains("win") ? "windows" : osName;
  }

  /**
   * Populates common request fields shared by both
   * version check and error report requests.
   *
   * @param request the request instance to populate
   */
  private void populateBaseRequest(Object request) {
    String osName = getOSName();
    int osVersion = getOSVersion();

    if (request instanceof VersionCheckRequest req) {
      applyCommonFields(req, osName, osVersion);
    } else if (request instanceof ErrorReportRequest req) {
      applyCommonFields(req, osName, osVersion);
    }
  }

  /**
   * Applies shared fields to a {@link VersionCheckRequest}.
   */
  private void applyCommonFields(VersionCheckRequest req, String osName, int osVersion) {
    req.setAddonVersion(addon.addonInfo().getVersion())
        .setLabymodVersion(addon.labyAPI().getVersion())
        .setApiVersion(API_VERSION)
        .setMinecraftVersion(addon.labyAPI().minecraft().getVersion())
        .setOs(osName)
        .setOsVersion(osVersion)
        .setFileHash(addon.addonInfo().getFileHash())
        .setReleaseChannel(addon.addonInfo().getReleaseChannel())
        .setIsFlintAddon(addon.addonInfo().isFlintAddon())
        .setIsCurrentOsSupported(addon.addonInfo().isCurrentOsSupported());

    for (MavenDependency dependency : addon.addonInfo().getMavenDependencies()) {
      req.addMavenDependency(convertMavenDependencyToString(dependency));
    }
  }

  /**
   * Applies shared fields to an {@link ErrorReportRequest}.
   */
  private void applyCommonFields(ErrorReportRequest req, String osName, int osVersion) {
    req.setAddonVersion(addon.addonInfo().getVersion())
        .setLabymodVersion(addon.labyAPI().getVersion())
        .setApiVersion(API_VERSION)
        .setMinecraftVersion(addon.labyAPI().minecraft().getVersion())
        .setOs(osName)
        .setOsVersion(osVersion)
        .setFileHash(addon.addonInfo().getFileHash())
        .setReleaseChannel(addon.addonInfo().getReleaseChannel())
        .setIsFlintAddon(addon.addonInfo().isFlintAddon())
        .setIsCurrentOsSupported(addon.addonInfo().isCurrentOsSupported());

    for (MavenDependency dependency : addon.addonInfo().getMavenDependencies()) {
      req.addMavenDependency(convertMavenDependencyToString(dependency));
    }
  }

  /**
   * Builds a fully populated {@link VersionCheckRequest}.
   *
   * @return version check request
   */
  private VersionCheckRequest buildBaseVersionCheckRequest() {
    VersionCheckRequest request = new VersionCheckRequest();
    populateBaseRequest(request);
    return request;
  }

  /**
   * Builds a fully populated {@link ErrorReportRequest}.
   *
   * @return error report request
   */
  private ErrorReportRequest buildBaseErrorReportRequest() {
    ErrorReportRequest request = new ErrorReportRequest();
    populateBaseRequest(request);
    return request;
  }

  /**
   * Sends a version check request to the API.
   */
  private void checkVersion() {
    VersionCheckRequest request = buildBaseVersionCheckRequest();

    versioningApiClient
        .check(ADDON_SLUG, request)
        .thenAccept(response -> this.versionCheckResponse = response);
  }

  /**
   * Returns the latest version check response.
   *
   * @return cached response or null if not yet received
   */
  public VersionCheckResponse getVersionCheckResponse() {
    return versionCheckResponse;
  }

  /**
   * Reports an error using a {@link Throwable}.
   *
   * <p>
   * The method automatically extracts:
   * <ul>
   *     <li>Error class</li>
   *     <li>Error message</li>
   *     <li>Full stacktrace</li>
   * </ul>
   *
   * @param throwable the exception to report
   * @return unique error report ID
   */
  public UUID reportError(Throwable throwable) {
    String className = throwable.getClass().getName();
    String message = throwable.getMessage();

    String error = (message != null)
        ? className + ": " + message
        : className;

    // Convert full stack trace into a string
    StringWriter sw = new StringWriter();
    throwable.printStackTrace(new PrintWriter(sw));

    return reportError(error, sw.toString());
  }

  /**
   * Reports an error manually.
   *
   * @param error error message/title
   * @param stacktrace full stacktrace
   * @return unique error report ID
   */
  public UUID reportError(String error, String stacktrace) {
    UUID id = UUID.randomUUID();

    ErrorReportRequest request = buildBaseErrorReportRequest()
        .setId(id.toString())
        .setError(error)
        .setStacktrace(stacktrace);

    versioningApiClient.reportError(ADDON_SLUG, request);

    return id;
  }
}