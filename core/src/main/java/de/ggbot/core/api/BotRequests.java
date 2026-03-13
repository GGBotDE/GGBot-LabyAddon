package de.ggbot.core.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.ggbot.core.GGBot;
import de.ggbot.sdk.api.IngameApi;
import de.ggbot.sdk.api.ModulesApi;
import de.ggbot.sdk.core.ApiCallback;
import de.ggbot.sdk.model.GetBotHealth200Response;
import de.ggbot.sdk.model.GetGGFeaturesCityBuild200Response;
import de.ggbot.sdk.model.GetGGFeaturesCurrentPlot200Response;
import de.ggbot.sdk.model.GetGGFeaturesMoney200Response;
import de.ggbot.sdk.model.SendCommandToBotRequest;
import de.ggbot.sdk.model.Ticket;
import de.ggbot.sdk.model.Ticket.StatusEnum;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import de.ggbot.sdk.core.ApiClient;
import de.ggbot.sdk.core.ApiException;
import de.ggbot.sdk.core.Configuration;
import de.ggbot.sdk.api.BotsApi;
import de.ggbot.sdk.core.auth.OAuth;
import de.ggbot.sdk.model.Bot;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.component.format.TextColor;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static net.labymod.api.client.component.format.NamedTextColor.BLUE;
import static net.labymod.api.client.component.format.NamedTextColor.GRAY;

/**
 * Handles all bot-related API operations: fetching bot data, starting/stopping bots,
 * sending commands, retrieving logs, and accessing GGFeatures module data.
 */
public class BotRequests {

  /** Cached list of bots last fetched from the API. */
  private static List<Bot> cachedBots = new ArrayList<>();

  /** Tracks log entry IDs already displayed to prevent duplicate messages. */
  public static final Set<String> sentLogIds = new HashSet<>();

  /**
   * Returns an unmodifiable view of the cached bot list.
   *
   * @return the cached bots
   */
  public static List<Bot> getCachedBots() {
    return Collections.unmodifiableList(cachedBots);
  }

  /**
   * Finds the currently selected bot in the local cache without making any API
   * request. Returns {@code null} when no bot is selected or when the cached list
   * does not yet contain the selected ID.
   *
   * @param addon the addon instance
   * @return the matching {@link Bot}, or {@code null}
   */
  public static Bot getBotFromCache(GGBot addon) {
    String botString = addon.configuration().botlist.get();
    if (botString.isEmpty() || !botString.contains("(")) return null;
    try {
      long botId = getSelectedBotId(addon);
      for (Bot bot : cachedBots) {
        if (bot.getId() == botId) return bot;
      }
    } catch (Exception ignored) {}
    return null;
  }

  /**
   * Returns whether the selected bot is online according to the last cached data.
   * No network request is made — call {@link #updateBotList(GGBot)} first if
   * freshness is required.
   *
   * @param addon the addon instance
   * @return {@code true} if the cached bot entry is marked online
   */
  public static boolean isOnlineCached(GGBot addon) {
    Bot bot = getBotFromCache(addon);
    return bot != null && Boolean.TRUE.equals(bot.getOnline());
  }

  /**
   * Refreshes the cached bot list on a background thread and notifies the
   * render thread when done.
   *
   * @param addon      the addon instance
   * @param onComplete optional callback executed on the render thread after
   *                   the refresh completes (may be {@code null})
   */
  public static void updateBotListAsync(GGBot addon, Runnable onComplete) {
    Thread t = new Thread(() -> {
      try {
        if (addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.updatebotlist")) {
          cachedBots = new ArrayList<>(new BotsApi(
              createApiClient(addon, "de.ggbot.addon.api.updatebotlist")).getAllBots());
        }
      } catch (ApiException e) {
        addon.logger().error("Failed to refresh bot list: " + e.getMessage());
        addon.getVersioningHandler().reportError(e);
      }
      if (onComplete != null) {
        Laby.labyAPI().minecraft().executeOnRenderThread(onComplete);
      }
    }, "ggbot-botlist-refresh");
    t.setDaemon(true);
    t.start();
  }

  /**
   * Creates an {@link ApiClient} configured with the addon's OAuth token and the
   * base URL resolved for the given feature flag.
   *
   * @param addon   the addon instance providing the token and versioning information
   * @param feature the feature flag key used to resolve the base URL
   * @return a fully configured {@link ApiClient}
   */
  private static ApiClient createApiClient(GGBot addon, String feature) {
    ApiClient client = Configuration.getDefaultApiClient();
    client.setBasePath(addon.getVersioningHandler().getBaseUrlForFeature(feature));
    OAuth oauth = (OAuth) client.getAuthentication("oauth2");
    oauth.setAccessToken(addon.configuration().token.get());
    return client;
  }

  /**
   * Parses the numeric bot ID from the currently selected bot dropdown value.
   *
   * @param addon the addon instance
   * @return the selected bot's numeric ID
   */
  private static long getSelectedBotId(GGBot addon) {
    return Long.parseLong(addon.configuration().botlist.get().replaceAll("\\D+", ""));
  }

  /**
   * Refreshes the cached bot list by fetching all bots from the API.
   *
   * @param addon the addon instance used to authenticate the request
   * @throws ApiException if the API request fails
   */
  public static void updateBotList(GGBot addon) throws ApiException {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.updatebotlist")) return;
    cachedBots = new ArrayList<>(new BotsApi(
        createApiClient(addon, "de.ggbot.addon.api.updatebotlist")).getAllBots());
  }

  /**
   * Returns the status of the selected bot from the local cache.
   * Call {@link #updateBotList(GGBot)} to refresh before reading if freshness matters.
   *
   * @param addon the addon instance
   * @return bot status string, or {@code "Unknown"} if not found in cache
   */
  public static String getStatus(GGBot addon) {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.getstatus")) return "Unknown";
    Bot bot = getBotFromCache(addon);
    return bot != null ? bot.getStatus() : "Unknown";
  }

  /**
   * Returns the link name of the selected bot from the local cache.
   *
   * @param addon the addon instance
   * @return bot link name, or {@code "Unknown"}
   */
  public static String getName(GGBot addon) {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.getname")) return "Unknown";
    Bot bot = getBotFromCache(addon);
    return bot != null ? bot.getLinkName() : "Unknown";
  }

  /**
   * Starts the configured bot asynchronously.
   *
   * @param addon the addon instance
   * @throws ApiException if the API request fails
   */
  public static void startBot(GGBot addon) throws ApiException {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.startbot")) return;
    Bot bot = getBotFromCache(addon);
    if (bot == null) return;
    new BotsApi(createApiClient(addon, "de.ggbot.addon.api.startbot"))
        .startBotAsync(bot.getToken(), Callbacks.START);
  }

  /**
   * Stops the configured bot asynchronously.
   *
   * @param addon the addon instance
   * @throws ApiException if the API request fails
   */
  public static void stopBot(GGBot addon) throws ApiException {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.stopbot")) return;
    Bot bot = getBotFromCache(addon);
    if (bot == null) return;
    new BotsApi(createApiClient(addon, "de.ggbot.addon.api.stopbot"))
        .stopBotAsync(bot.getToken(), Callbacks.STOP);
  }

  /**
   * Returns whether the configured bot is currently online.
   *
   * @param addon the addon instance
   * @return {@code true} if the bot is online
   * @throws ApiException if the API request fails
   */
  public static boolean isOnline(GGBot addon) throws ApiException {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.isonline")) return false;
    ApiClient client = createApiClient(addon, "de.ggbot.addon.api.isonline");
    long botId = getSelectedBotId(addon);
    for (Bot bot : new BotsApi(client).getAllBots()) {
      if (bot.getId() == botId) return Boolean.TRUE.equals(bot.getOnline());
    }
    return false;
  }

  /**
   * Sends a command to the configured bot asynchronously.
   *
   * @param addon   the addon instance
   * @param command the command string to execute
   * @throws ApiException if the API request fails
   */
  public static void sendCommand(GGBot addon, String command) throws ApiException {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.sendcommand")) return;
    Bot bot = getBotFromCache(addon);
    if (bot == null) return;
    new BotsApi(createApiClient(addon, "de.ggbot.addon.api.sendcommand"))
        .sendCommandToBotAsync(bot.getToken(),
            new SendCommandToBotRequest().command(command), Callbacks.SEND_COMMAND);
  }

  /**
   * Fetches bot logs asynchronously and displays new entries in the client chat.
   * Already-shown entries are deduplicated via {@link #sentLogIds}.
   *
   * <p>Uses OkHttp directly because the log endpoint is not part of the generated SDK.
   *
   * @throws ApiException if the API request fails
   */
  public static void logsAsync() throws ApiException {
    GGBot addon = GGBot.getInstance();
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.logs")) return;
    if (!GGBot.isAuthenticated()) return;

    Bot bot = getBotFromCache(addon);
    if (bot == null) return;

    ApiClient client = createApiClient(addon, "de.ggbot.addon.api.logs");
    OAuth oauth = (OAuth) client.getAuthentication("oauth2");
    String baseUrl = addon.getVersioningHandler().getBaseUrlForFeature("de.ggbot.addon.api.logs");

    Request request = new Request.Builder()
        .url(baseUrl + "/bot/" + bot.getToken() + "/logs")
        .addHeader("Authorization", "Bearer " + oauth.getAccessToken())
        .build();

new OkHttpClient().newCall(request).enqueue(new Callback() {
      @Override
      public void onFailure(@NotNull Call call, @NotNull IOException e) {
        addon.logger().error("Failed to fetch bot logs: " + e.getMessage());
      }

      @Override
      public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
        if (response.body() == null) return;
        String body = response.body().string();
        Type listType = new TypeToken<List<BotLogEntry>>() {}.getType();
        List<BotLogEntry> entries = new Gson().fromJson(body, listType);
        if (entries == null || entries.isEmpty()) return;

        entries.sort(Comparator.comparing(BotLogEntry::getTimestampInstant));
        ZoneId zoneId = ZoneId.systemDefault();
        for (BotLogEntry log : entries) {
          String logId = log.getMessage() + log.getTimestamp();
          if (!sentLogIds.contains(logId)) {
            Component entry = Component
                .translatable("ggbot.messages.log.time", BLUE,
                    Component.text(getTimestampForZone(zoneId, log)))
                .append(Component.text(" ", BLUE))
                .append(Component.text(formatLevel(log.getLevel()),
                    getColorForLevel(log.getLevel())))
                .append(Component.translatable("ggbot.messages.log.textfilter", GRAY,
                    Component.text(log.getMessage())));
            addon.displayMessage(entry);
            sentLogIds.add(logId);
          }
        }
      }
    });
  }

  /**
   * Retrieves the bot's current money amount via the GGFeatures module.
   *
   * @param addon    the addon instance
   * @param callback called with the money value, or {@code 0.0} on failure
   * @throws ApiException if the API request fails
   */
  public static void getMoney(GGBot addon, Consumer<Double> callback) throws ApiException {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.getmoney")) {
      callback.accept(0.0); return;
    }
    if (!GGBot.isAuthenticated()) { callback.accept(0.0); return; }
    Bot bot = getBotFromCache(addon);
    if (bot == null) { callback.accept(0.0); return; }
    ApiClient client = createApiClient(addon, "de.ggbot.addon.api.getmoney");
    new ModulesApi(client).getGGFeaturesMoneyAsync(bot.getToken(), new ApiCallback<>() {
      @Override public void onFailure(ApiException e, int s, Map<String, List<String>> h) { callback.accept(0.0); }
      @Override public void onSuccess(GetGGFeaturesMoney200Response r, int s, Map<String, List<String>> h) { callback.accept(r.getData() != null ? r.getData() : 0.0); }
      @Override public void onUploadProgress(long a, long b, boolean c) {}
      @Override public void onDownloadProgress(long a, long b, boolean c) {}
    });
  }

  /**
   * Retrieves the bot's current health value from the in-game API.
   *
   * @param addon    the addon instance
   * @param callback called with the health value, or {@code 0.0} on failure
   * @throws ApiException if the API request fails
   */
  public static void getHealth(GGBot addon, Consumer<Double> callback) throws ApiException {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.gethealth")) {
      callback.accept(0.0); return;
    }
    if (!GGBot.isAuthenticated()) { callback.accept(0.0); return; }
    Bot bot = getBotFromCache(addon);
    if (bot == null) { callback.accept(0.0); return; }
    ApiClient client = createApiClient(addon, "de.ggbot.addon.api.gethealth");
    new IngameApi(client).getBotHealthAsync(bot.getToken(), new ApiCallback<>() {
      @Override public void onFailure(ApiException e, int s, Map<String, List<String>> h) { callback.accept(0.0); }
      @Override public void onSuccess(GetBotHealth200Response r, int s, Map<String, List<String>> h) { callback.accept(r.getData() != null ? r.getData() : 0.0); }
      @Override public void onUploadProgress(long a, long b, boolean c) {}
      @Override public void onDownloadProgress(long a, long b, boolean c) {}
    });
  }

  /**
   * Retrieves the bot's current CityBuild server name (e.g. {@code "CB1"}).
   *
   * @param addon    the addon instance
   * @param callback called with the CityBuild name, or {@code "Unknown"} on failure
   * @throws ApiException if the API request fails
   */
  public static void getCitybuild(GGBot addon, Consumer<String> callback) throws ApiException {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.getcitybuild")) {
      callback.accept("Unknown"); return;
    }
    if (!GGBot.isAuthenticated()) { callback.accept("Unknown"); return; }
    Bot bot = getBotFromCache(addon);
    if (bot == null) { callback.accept("Unknown"); return; }
    ApiClient client = createApiClient(addon, "de.ggbot.addon.api.getcitybuild");
    new ModulesApi(client).getGGFeaturesCityBuildAsync(bot.getToken(), new ApiCallback<>() {
      @Override public void onFailure(ApiException e, int s, Map<String, List<String>> h) { callback.accept("Unknown"); }
      @Override public void onSuccess(GetGGFeaturesCityBuild200Response r, int s, Map<String, List<String>> h) { callback.accept(r.getData() != null ? r.getData() : "Unknown"); }
      @Override public void onUploadProgress(long a, long b, boolean c) {}
      @Override public void onDownloadProgress(long a, long b, boolean c) {}
    });
  }

  /**
   * Retrieves the bot's current plot coordinates (e.g. {@code "0;0"}).
   *
   * @param addon    the addon instance
   * @param callback called with the plot string, or {@code "Unknown"} on failure
   * @throws ApiException if the API request fails
   */
  public static void getPlot(GGBot addon, Consumer<String> callback) throws ApiException {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.getplot")) {
      callback.accept("Unknown"); return;
    }
    if (!GGBot.isAuthenticated()) { callback.accept("Unknown"); return; }
    Bot bot = getBotFromCache(addon);
    if (bot == null) { callback.accept("Unknown"); return; }
    ApiClient client = createApiClient(addon, "de.ggbot.addon.api.getplot");
    new ModulesApi(client).getGGFeaturesCurrentPlotAsync(bot.getToken(), new ApiCallback<>() {
      @Override public void onFailure(ApiException e, int s, Map<String, List<String>> h) { callback.accept("Unknown"); }
      @Override public void onSuccess(GetGGFeaturesCurrentPlot200Response r, int s, Map<String, List<String>> h) { callback.accept(r.getData() != null ? r.getData().getPlotString() : "Unknown"); }
      @Override public void onUploadProgress(long a, long b, boolean c) {}
      @Override public void onDownloadProgress(long a, long b, boolean c) {}
    });
  }

  /**
   * Fetches all tickets matching the given status filter.
   *
   * @param addon      the addon instance
   * @param statusEnum ticket status filter (e.g. {@code "OPEN"}, {@code "CLOSED"}, or empty for all)
   * @param callback   called with the filtered ticket list, or an empty list on failure
   * @throws ApiException if the API request fails
   */
  public static void getTickets(GGBot addon, String statusEnum, Consumer<List<Ticket>> callback) throws ApiException {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.gettickets")) {
      callback.accept(new ArrayList<>()); return;
    }
    if (!GGBot.isAuthenticated()) { callback.accept(new ArrayList<>()); return; }
    Bot bot = getBotFromCache(addon);
    if (bot == null) { callback.accept(new ArrayList<>()); return; }
    ApiClient client = createApiClient(addon, "de.ggbot.addon.api.gettickets");
    new ModulesApi(client).getTicketsAsync(bot.getToken(), statusEnum, new ApiCallback<>() {
      @Override public void onFailure(ApiException e, int s, Map<String, List<String>> h) { callback.accept(new ArrayList<>()); }
      @Override public void onSuccess(List<Ticket> r, int s, Map<String, List<String>> h) { callback.accept(r); }
      @Override public void onUploadProgress(long a, long b, boolean c) {}
      @Override public void onDownloadProgress(long a, long b, boolean c) {}
    });
  }

  /**
   * Maps a log level string to its display color.
   *
   * @param level raw log level (e.g. {@code "info"}, {@code "error"})
   * @return the corresponding {@link TextColor}
   */
  private static TextColor getColorForLevel(String level) {
    return switch (level.toLowerCase()) {
      case "debug" -> NamedTextColor.BLUE;
      case "info" -> NamedTextColor.GREEN;
      case "warn", "warning" -> NamedTextColor.YELLOW;
      case "error" -> NamedTextColor.RED;
      case "plugin" -> NamedTextColor.AQUA;
      default -> NamedTextColor.GRAY;
    };
  }

  /**
   * Formats a raw log level into a padded display label (e.g. {@code "[Info]  "}).
   *
   * @param levelRaw the raw log level string
   * @return the formatted label
   */
  private static String formatLevel(String levelRaw) {
    String formatted = levelRaw.substring(0, 1).toUpperCase() + levelRaw.substring(1).toLowerCase();
    int diff = Math.max(0, 4 - formatted.length());
    return "[" + formatted + "]" + "\u2007".repeat(diff + 1);
  }

  /**
   * Formats a log entry's timestamp in the user's local timezone.
   *
   * @param zoneId   the target timezone
   * @param logEntry the log entry to format
   * @return a formatted timestamp string (e.g. {@code "13.03 14:22:01"})
   */
  public static String getTimestampForZone(ZoneId zoneId, BotLogEntry logEntry) {
    java.time.ZonedDateTime zoned = logEntry.getTimestampInstant().atZone(zoneId);
    return zoned.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM HH:mm:ss"));
  }
}
