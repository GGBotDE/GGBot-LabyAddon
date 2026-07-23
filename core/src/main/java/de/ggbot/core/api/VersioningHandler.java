package de.ggbot.core.api;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.versioning.ErrorReportRequest;
import de.ggbot.core.api.versioning.VersionCheckRequest;
import de.ggbot.core.api.versioning.VersionCheckResponse;
import de.ggbot.core.api.versioning.VersioningApiClient;
import de.ggbot.core.api.versioning.matrix.ClientRuleEngine;
import de.ggbot.core.api.versioning.response.FeatureFlagResponse;
import de.ggbot.core.api.versioning.response.IntervalConfig;
import de.ggbot.core.api.versioning.response.MessageResponse;
import de.ggbot.core.gui.MessagePopupActivity;
import de.ggbot.core.utils.AsyncScheduler;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.network.server.ServerJoinEvent;
import net.labymod.api.models.addon.info.dependency.MavenDependency;
import net.labymod.api.notification.Notification;
import net.labymod.api.notification.Notification.NotificationButton;
import net.labymod.api.notification.Notification.Type;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Handles communication with the remote versioning API.
 *
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>Fetching the server's rule matrix and evaluating it locally
 *         (feature flags, support state, client messages, update info)</li>
 *     <li>Sending the optional, opt-out version report</li>
 *     <li>Sending optional, opt-out runtime error reports to the backend</li>
 * </ul>
 *
 * @LabyMod review team:
 * The feature flag system is not meant to be telemetry. It is meant to disable
 * certain features, when specific requirements are met.
 *
 * <p>Example: an outdated maven dependency on a specific build of the addon
 * (possibly not distributed via Flint but as a jar directly) that on Windows
 * 10 specifically causes a vulnerability when a feature calling that library
 * is used - in that case exactly that feature can be deactivated completely
 * for exactly that combination. It disables features in very specific
 * scenarios only.
 *
 * <p>The previous implementation was designed to minimize network traffic by
 * sending only the information that was actually required. This benefited both
 * us, by reducing the amount of traffic we needed to serve, and our users, by
 * allowing the addon to load faster on slower internet connections. Sending
 * data for every build version, Windows version, and other possible client
 * combinations would have resulted in a lot of unnecessary traffic. Therefore,
 * we decided that the previous approach was the most suitable solution at the
 * time. After you raised concerns about this method, we completely rewrote our
 * versioning backend to send only information about what is not allowed, with
 * corresponding rules, instead of sending definitions for every version
 * independently.
 *
 * <p>Since your review, the addon no longer uploads the environment for this:
 * it downloads the server's rule matrix ({@code GET /v1/matrix}) and evaluates
 * it locally via {@link ClientRuleEngine}, so the mandatory startup path sends
 * no OS/hash/dependency data at all. In addition, we added two clearly separated endpoints,
 * opt-out (default on, toggleable in the settings and in the onboarding for every user,
 * logged in or not): the version report (see
 * {@link #sendVersionReportIfEnabled()}) and error reports (see
 * {@link #reportError(String, String)}).
 */
public class VersioningHandler {

  /** Base API endpoint for the versioning service */
  private static final String BASE_URL = "https://labyversion.ggbot.de";

  /** Base API endpoint for the main GGBot API */
  public static final String API_BASE_URL = "https://api.ggbot.de/api";

  /** Addon identifier used by the backend */
  private static final String ADDON_SLUG = "labymod-addon";

  /** Current API version used by the addon */
  private static final String API_VERSION = "0.15.1";

  /**
   * Hosts the versioning response may redirect feature API calls to. Some API
   * calls carry the user's OAuth token, so a compromised versioning backend
   * must never be able to point them at an arbitrary URL - only our own
   * domains (and their subdomains) over HTTPS are accepted.
   */
  private static final String[] ALLOWED_BASE_URL_DOMAINS = {"ggbot.de", "ggbot.me", "gg-bot.com", "development-server.eu"};

  /** Reference to the addon instance */
  private final GGBot addon;

  /** Client responsible for performing API requests */
  private final VersioningApiClient versioningApiClient;

  /** Cached response of the version check */
  private VersionCheckResponse versionCheckResponse;

  private List<String> shownMessages;

  /**
   * Creates a new {@link VersioningHandler}.
   * Immediately performs a version check on creation.
   *
   * @param addon the addon instance
   */
  public VersioningHandler(GGBot addon) {
    this.addon = addon;
    this.versioningApiClient = new VersioningApiClient(BASE_URL);

    this.shownMessages = new ArrayList<>(List.of(addon.configuration().viewedSystemMessages.get().split(",")));
    checkVersion();
    sendVersionReportIfEnabled();
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
   * Performs the version/feature check: fetches the server's rule matrix (a
   * request that carries no environment data) and evaluates it locally via
   * {@link ClientRuleEngine}. The locally built environment context never
   * leaves this JVM here.
   *
   * <p>There is deliberately no client-side fallback to the legacy
   * {@code POST /v1/check} endpoint - that endpoint only still exists on the
   * backend for older addon builds. If the matrix cannot be fetched, the
   * response stays {@code null} and {@link #isFeatureEnabled(String)} falls
   * back to its permissive defaults.
   */
  private void checkVersion() {
    versioningApiClient
        .matrix(ADDON_SLUG)
        .thenAccept(matrix -> this.versionCheckResponse =
            ClientRuleEngine.evaluate(matrix, buildBaseVersionCheckRequest()))
        .exceptionally(throwable -> {
          addon.logger().warn("Failed to fetch the versioning matrix: " + throwable.getMessage());
          return null;
        });
  }

  /**
   * Sends the optional version report if the user has not opted out.
   *
   * @LabyMod review team:
   * The initial goal was to not have telemetry at all - the environment data
   * used to be part of the version check only so the server could answer with
   * exactly the feature flags and messages that apply to this client, because
   * after a while there may be a lot of builds and version-specific
   * messages/feature flags that we otherwise would ALL have to send to every
   * client on every start. More on that see {@link VersioningHandler}
   *
   * <p>On second thought, after you raised it in the review, we determined that it is important
   * for us to know how many users are running possibly vulnerable (possibly
   * non-Flint, jar-distributed) versions so we can weigh how many users would
   * be affected before hard-disabling a feature for them. The same data also
   * helps with the messaging we send to users: we can display messages in the
   * LabyMod client on launch/connect in SPECIFIC scenarios (e.g. "this build
   * has a known issue on your OS, please update") and want to know how many
   * users such a scenario actually matches.
   *
   * <p>That is why this exists as a separate, clearly optional, non-identifiable report:
   * it is opt-out (default on), has its own switch in the addon settings and in the
   * onboarding for every user (logged in or not). And it sends roughly the same version
   * information as before.
   */
  private void sendVersionReportIfEnabled() {
    if (!isVersionReportEnabled()) return;
    versioningApiClient
        .versionReport(ADDON_SLUG, buildBaseVersionCheckRequest())
        .exceptionally(throwable -> null); // Old backends without the endpoint: silently skip.
  }

  /**
   * Returns whether the user allows the optional version report.
   *
   * @return {@code true} if the version report may be sent
   */
  public boolean isVersionReportEnabled() {
    return Boolean.TRUE.equals(addon.configuration().generalSub.versionReportEnabled.get());
  }

  /**
   * Returns whether the user allows automatic error reports.
   *
   * @return {@code true} if error reports may be sent
   */
  public boolean isErrorReportingEnabled() {
    return Boolean.TRUE.equals(addon.configuration().generalSub.errorReportingEnabled.get());
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
   * <p>Error reporting is optional and opt-out (default on): when the user has
   * disabled it in the settings or the onboarding, the error is only logged
   * locally and nothing is sent to the backend.
   *
   * @param error error message/title
   * @param stacktrace full stacktrace
   * @return unique error report ID (also generated when reporting is disabled,
   *         so callers can still reference it in local logs)
   */
  public UUID reportError(String error, String stacktrace) {
    UUID id = UUID.randomUUID();

    if (!isErrorReportingEnabled()) {
      addon.logger().warn("[" + id + "] " + error + " (error reporting disabled, not sent)");
      return id;
    }

    ErrorReportRequest request = buildBaseErrorReportRequest()
        .setId(id.toString())
        .setError(error)
        .setStacktrace(stacktrace);

    versioningApiClient.reportError(ADDON_SLUG, request);

    return id;
  }

  /**
   * Checks if a specific feature is enabled based on the latest version check response.
   * @param feature the feature name to check
   * @return true if the feature is enabled or if no version check response is available, false if the feature is explicitly disabled
   */
  public boolean isFeatureEnabled(String feature) {
    if (versionCheckResponse == null)
      return true;

    if(!versionCheckResponse.isSupported())
      return false;

    if(!versionCheckResponse.getFeatures().containsKey(feature))
      return true;

    return versionCheckResponse.getFeatures().get(feature).isEnabled();
  }

  /**
   * Retrieves the base URL for a specific feature based on the latest version check response.
   * If the feature is not defined or the version check response is unavailable, it returns the default API base URL.
   *
   * <p>Redirect targets are locked to our own domains: some feature API calls
   * carry the user's OAuth token, so even a fully compromised versioning
   * backend must not be able to route those calls (and with them the token)
   * to an attacker-controlled host. Any URL outside the allowlist is ignored
   * and the default API base URL is used instead.
   *
   * @param feature the feature name to retrieve the base URL for
   * @return the base URL for the specified feature or the default API base URL if not defined
   */
  public String getBaseUrlForFeature(String feature) {
    if (versionCheckResponse == null)
      return API_BASE_URL;

    if(!versionCheckResponse.getFeatures().containsKey(feature))
      return API_BASE_URL;

    String url = versionCheckResponse.getFeatures().get(feature).getVersionCompatabilityConversionPath();
    if (url == null || url.isEmpty()) return API_BASE_URL;
    if (!isAllowedBaseUrl(url)) {
      addon.logger().warn("Ignoring feature base URL outside the allowed domains: " + url);
      return API_BASE_URL;
    }
    return url;
  }

  /**
   * Returns whether a redirect base URL is HTTPS and points at one of our own
   * domains (or a subdomain of one).
   *
   * @param url the URL to validate
   * @return {@code true} if the URL may be used as an API base URL
   */
  private static boolean isAllowedBaseUrl(String url) {
    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      return false;
    }
    if (!"https".equalsIgnoreCase(uri.getScheme())) return false;
    String host = uri.getHost();
    if (host == null) return false;
    host = host.toLowerCase(Locale.ROOT);
    for (String domain : ALLOWED_BASE_URL_DOMAINS) {
      if (host.equals(domain) || host.endsWith("." + domain)) return true;
    }
    return false;
  }

  /**
   * Applies the server-controlled pacing of a feature to a requested polling
   * interval. User-configurable intervals stay user-configurable, but the
   * backend can define a minimum, maximum, default and multiplier per
   * repeating feature (anything that depends on loops/intervals), so API
   * polling frequency can be tuned without shipping a new addon build.
   *
   * @param feature     the feature key the interval belongs to
   * @param requestedMs the interval the caller wants, in milliseconds
   * @return the effective interval in milliseconds
   */
  public long clampIntervalMs(String feature, long requestedMs) {
    if (versionCheckResponse == null) return requestedMs;
    FeatureFlagResponse flag = versionCheckResponse.getFeatures().get(feature);
    if (flag == null) return requestedMs;
    IntervalConfig interval = flag.getInterval();
    return interval != null ? interval.apply(requestedMs) : requestedMs;
  }

  /** Reasons for checking messages, used to determine which messages to show based on their configuration. */
  private enum CheckMessageReason {
    JOIN,
    INTERACTION,
  }

  /**
   * Saves a message as viewed to prevent it from being shown again.
   * @param message the message to mark as viewed
   */
  public void saveViewedMessage(MessageResponse message) {
    if(shownMessages.contains(message.getUuid())) return;

    shownMessages.add(message.getUuid());
    addon.configuration().viewedSystemMessages.set(String.join(",", shownMessages));
  }

  /** Checks which messages should be shown based on the reason for checking and the message configuration.
   * @param reason the reason for checking messages (e.g., player joined, player interaction)
   */
  private void checkMessages(CheckMessageReason reason) {
    if(versionCheckResponse == null || versionCheckResponse.getMessages() == null) return;
    for(MessageResponse message : versionCheckResponse.getMessages()) {

      if(message.getEnabledUntil() != null && !message.getEnabledUntil().isEmpty() &&
          java.time.LocalDateTime.parse(message.getEnabledUntil()).isBefore(java.time.LocalDateTime.now())) continue;

      if(message.getLocale() != null && !message.getLocale().isEmpty() &&
          !message.getLocale().equalsIgnoreCase(addon.labyAPI().minecraft().options().getCurrentLanguage())) continue;

      if(message.isOnlyShowAfterInteraction() && reason == CheckMessageReason.JOIN) continue;
      if(!message.isOnlyShowAfterInteraction() && reason == CheckMessageReason.INTERACTION) continue;

      if(message.isShowOnce() && shownMessages.contains(message.getUuid())) continue;

      AsyncScheduler.runLater(() -> {
        Laby.labyAPI().minecraft().executeNextTick(() -> {

          if (message.isToast()) {
            var notification = Notification.builder()
                .type(Type.SYSTEM)
                .text(Component.text(message.getMessage()))
                .title(Component.translatable("ggbot.messages.system.title", NamedTextColor.GREEN))
                .icon(Icon.url(
                    "https://www.ggbot.de/assets/img/logo.png")); // GGBot logo as icon (logo updates seasonally, so the url gets used)

            if (message.getLink() != null && !message.getLink().isEmpty()) {
              notification.onClick(
                  (notification1) -> Laby.references().chatExecutor().openUrl(message.getLink()));
              notification.addButton(
                  NotificationButton.of(Component.translatable("ggbot.messages.system.button"),
                      () -> Laby.references().chatExecutor().openUrl(message.getLink())));
            }

            addon.labyAPI().notificationController().push(notification.build());
          }

          if (!message.isToast() && !message.isPopup()) {
            var component = Component.text(message.getMessage()).color(NamedTextColor.GREEN);

            if (message.getLink() != null && !message.getLink().isEmpty()) {
              component.clickEvent(
                  net.labymod.api.client.component.event.ClickEvent.openUrl(message.getLink()));
            }

            addon.labyAPI().minecraft().chatExecutor().displayClientMessage(component);
          }

          if (message.isPopup()) {
            var activity = new MessagePopupActivity(message.getMessage(), message.getLink());
            Laby.labyAPI().minecraft().minecraftWindow().displayScreen(activity);
          }

          saveViewedMessage(message);
        });
      }, message.getShowAfter()*1000L);
    }
  }

  /** Checks which messages should be shown when the player joins a server. */
  @Subscribe
  private void checkMessagesOnJoin(ServerJoinEvent e) {
    checkMessages(CheckMessageReason.JOIN);
  }

  /** Checks which messages should be shown when the player interacts with GGBot features. */
  public void checkMessagesOnInteraction() {
    checkMessages(CheckMessageReason.INTERACTION);
  }
}