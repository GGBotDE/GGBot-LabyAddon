package de.ggbot.core.api;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.versioning.ErrorReportRequest;
import de.ggbot.core.api.versioning.VersionCheckRequest;
import de.ggbot.core.api.versioning.VersionCheckResponse;
import de.ggbot.core.api.versioning.VersioningApiClient;
import de.ggbot.core.api.versioning.response.MessageResponse;
import de.ggbot.core.gui.MessagePopupActivity;
import de.ggbot.core.utils.AsyncScheduler;
import kotlin.jvm.internal.Lambda;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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

  /** Base API endpoint for the main GGBot API */
  public static final String API_BASE_URL = "https://api.ggbot.de/api";

  /** Addon identifier used by the backend */
  private static final String ADDON_SLUG = "labymod-addon";

  /** Current API version used by the addon */
  private static final String API_VERSION =  "0.15.1";

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
   * @param feature the feature name to retrieve the base URL for
   * @return the base URL for the specified feature or the default API base URL if not defined
   */
  public String getBaseUrlForFeature(String feature) {
    if (versionCheckResponse == null)
      return API_BASE_URL;

    if(!versionCheckResponse.getFeatures().containsKey(feature))
      return API_BASE_URL;

    String url = versionCheckResponse.getFeatures().get(feature).getVersionCompatabilityConversionPath();
    return url != null && !url.isEmpty() ? url : API_BASE_URL;
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