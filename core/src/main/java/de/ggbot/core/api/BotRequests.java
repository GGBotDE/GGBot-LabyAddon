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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static net.labymod.api.client.component.format.NamedTextColor.BLUE;
import static net.labymod.api.client.component.format.NamedTextColor.GRAY;

/**
 * Handles all bot-related API operations such as fetching, starting, stopping,
 * sending commands, retrieving logs, and accessing GGFeatures module data.
 */
public class BotRequests {


  /**
   * List of all bots fetched from the API.
   */
  public static List<Bot> bots = new ArrayList<>();

  /**
   * Updates the global bot list by requesting all available bots from the API.
   *
   * @param addon Addon instance used to load the OAuth token
   * @throws ApiException if the API request fails
   */
  public static void updateBotList(GGBot addon) throws ApiException {
    if(!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.updatebotlist")) return;
    GGBot.code = addon.configuration().token.get();
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath(addon.getVersioningHandler().getBaseUrlForFeature("de.ggbot.addon.api.updatebotlist"));
    OAuth oauth2 = (OAuth) defaultClient.getAuthentication("oauth2");
    oauth2.setAccessToken(GGBot.code);

    BotsApi api = new BotsApi(defaultClient);
    bots = api.getAllBots();
  }

  /**
   * Returns the status string of the currently configured bot.
   *
   * @param addon Addon instance used to load the OAuth token
   * @return Bot status as string or "Unknown" if not found
   * @throws ApiException if the API request fails
   */
  public static String getStatus(GGBot addon) throws ApiException {
    if(!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.getstatus")) return "Unknown";
    GGBot.code = addon.configuration().token.get();
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath(addon.getVersioningHandler().getBaseUrlForFeature("de.ggbot.addon.api.getstatus"));
    OAuth oauth2 = (OAuth) defaultClient.getAuthentication("oauth2");
    oauth2.setAccessToken(GGBot.code);

    BotsApi api = new BotsApi(defaultClient);
    long num = Long.parseLong(GGBot.getInstance().configuration().botlist.get().replaceAll("\\D+", ""));

    for (Bot bot : api.getAllBots()) {
      if (bot.getId() == num) {
        return bot.getStatus();
      }
    }

    return "Unknown";
  }

  /**
   * Returns the link name of the configured bot.
   *
   * @param addon Addon instance used to load the OAuth token
   * @return Bot link name or "Unknown"
   * @throws ApiException if the API request fails
   */
  public static String getName(GGBot addon) throws ApiException {
      if(!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.getname")) return "Unknown";
    GGBot.code = addon.configuration().token.get();
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath(addon.getVersioningHandler().getBaseUrlForFeature("de.ggbot.addon.api.getname"));
    OAuth oauth2 = (OAuth) defaultClient.getAuthentication("oauth2");
    oauth2.setAccessToken(GGBot.code);

    BotsApi api = new BotsApi(defaultClient);
    long num = Long.parseLong(GGBot.getInstance().configuration().botlist.get().replaceAll("\\D+", ""));

    for (Bot bot : api.getAllBots()) {
      if (bot.getId() == num) {
        return bot.getLinkName();
      }
    }

    return "Unknown";
  }

  /**
   * Starts the configured bot asynchronously.
   *
   * @param addon Addon instance used to access token and bot selection
   * @throws ApiException if the API request fails
   */
  public static void startBot(GGBot addon) throws ApiException {
    if(!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.startbot")) return;
    GGBot.code = addon.configuration().token.get();
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath(addon.getVersioningHandler().getBaseUrlForFeature("de.ggbot.addon.api.startbot"));
    OAuth oauth2 = (OAuth) defaultClient.getAuthentication("oauth2");
    oauth2.setAccessToken(GGBot.code);

    BotsApi api = new BotsApi(defaultClient);
    for (Bot bot : api.getAllBots()) {
      long num = Long.parseLong(GGBot.getInstance().configuration().botlist.get().replaceAll("\\D+", ""));
      if (bot.getId() == num) {
        api.startBotAsync(bot.getToken(), Callbacks.startCallback);
      }
    }
  }

  /**
   * Stops the configured bot asynchronously.
   *
   * @param addon Addon instance used to access token and bot selection
   * @throws ApiException if the API request fails
   */
  public static void stopBot(GGBot addon) throws ApiException {
    if(!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.stopbot")) return;
    GGBot.code = addon.configuration().token.get();
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath(addon.getVersioningHandler().getBaseUrlForFeature("de.ggbot.addon.api.stopbot"));
    OAuth oauth2 = (OAuth) defaultClient.getAuthentication("oauth2");
    oauth2.setAccessToken(GGBot.code);

    BotsApi api = new BotsApi(defaultClient);
    for (Bot bot : api.getAllBots()) {
      long num = Long.parseLong(GGBot.getInstance().configuration().botlist.get().replaceAll("\\D+", ""));
      if (bot.getId() == num) {
        api.stopBotAsync(bot.getToken(), Callbacks.stopCallback);
      }
    }
  }

  /**
   * Checks whether the configured bot is currently online.
   *
   * @param addon Addon instance
   * @return true if the bot is online, false otherwise
   * @throws ApiException if the API request fails
   */
  public static boolean isOnline(GGBot addon) throws ApiException {
    if(!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.isonline")) return false;
    GGBot.code = addon.configuration().token.get();
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath(addon.getVersioningHandler().getBaseUrlForFeature("de.ggbot.addon.api.isonline"));
    OAuth oauth2 = (OAuth) defaultClient.getAuthentication("oauth2");
    oauth2.setAccessToken(GGBot.code);

    BotsApi api = new BotsApi(defaultClient);
    for (Bot bot : api.getAllBots()) {
      long num = Long.parseLong(GGBot.getInstance().configuration().botlist.get().replaceAll("\\D+", ""));
      if (bot.getId() == num) {
        return bot.getOnline();

      }
    }
    return false;
  }

  /**
   * Sends a command to the configured bot asynchronously.
   *
   * @param addon Addon instance used to load the OAuth token
   * @param command The command string to be executed by the bot
   * @throws ApiException if the API request fails
   */
  public static void sendCommand(GGBot addon, String command) throws ApiException {
      if(!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.sendcommand")) return;
    GGBot.code = addon.configuration().token.get();
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath(addon.getVersioningHandler().getBaseUrlForFeature("de.ggbot.addon.api.sendcommand"));
    OAuth oauth2 = (OAuth) defaultClient.getAuthentication("oauth2");
    oauth2.setAccessToken(GGBot.code);

    BotsApi api = new BotsApi(defaultClient);
    for (Bot bot : api.getAllBots()) {
      long num = Long.parseLong(GGBot.getInstance().configuration().botlist.get().replaceAll("\\D+", ""));
      if (bot.getId() == num) {
        String token = bot.getToken();
        api.sendCommandToBotAsync(token, new SendCommandToBotRequest().command(command), Callbacks.sendCommandCallback);
      }
    }
  }


  /**
   * Keeps track of already displayed log entries to prevent duplicates.
   */
  public static final Set<String> sentLogIds = new HashSet<>();

  /**
   * Fetches all logs from the configured bot asynchronously and displays them
   * in the user's client. New logs are filtered so each entry is shown only once.
   *
   * This method uses OkHttp directly because logs are not part of the SDK yet.
   *
   * @throws ApiException if the API request fails
   */
  public static void logsAsync() throws ApiException {
    if(!GGBot.getInstance().getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.logs")) return;
    if(!GGBot.isAuth && GGBot.isExpired){
      return;
    }
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath(GGBot.getInstance().getVersioningHandler().getBaseUrlForFeature("de.ggbot.addon.api.logs"));
    OAuth oauth2 = (OAuth) defaultClient.getAuthentication("oauth2");
    oauth2.setAccessToken(GGBot.code);

    BotsApi api = new BotsApi(Configuration.getDefaultApiClient());
    OkHttpClient client = new OkHttpClient();
    Gson gson = new Gson();

    for (Bot bot : api.getAllBots()) {
      long num = Long.parseLong(GGBot.getInstance().configuration().botlist.get().replaceAll("\\D+", ""));
      if (bot.getId() == num) {
        String token = bot.getToken();

        Request request = new Request.Builder()
            .url(GGBot.getInstance().getVersioningHandler().getBaseUrlForFeature("de.ggbot.addon.api.logs")+"/bot/" + token + "/logs")
            .addHeader("Authorization", "Bearer " + oauth2.getAccessToken())
            .build();

        client.newCall(request).enqueue(new Callback() {
          @Override
          public void onFailure(@NotNull Call call, @NotNull IOException e) {
            e.printStackTrace();
          }

          @Override
          public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
            assert response.body() != null;
            String body = response.body().string();

            Type listType = new TypeToken<List<BotLogEntry>>() {}.getType();
            List<BotLogEntry> entries = gson.fromJson(body, listType);

            if (entries == null || entries.isEmpty()) return;

            entries.sort(Comparator.comparing(BotLogEntry::getTimestampInstant));
            ZoneId zoneId = ZoneId.systemDefault();
            for (BotLogEntry log : entries) {
              String logId = log.getMessage() + log.getTimestamp();
              if (!sentLogIds.contains(logId)) {
                Component logsEntry = Component.translatable("ggbot.messages.log.time", BLUE, Component.text(getTimestampForZone(zoneId, log)))
                    .append(Component.text(" ", BLUE))
                    .append(Component.text(formatLevel(log.getLevel()), getColorForLevel(log.getLevel())))
                    .append(Component.translatable("ggbot.messages.log.textfilter",GRAY,Component.text(log.getMessage())));
                GGBot.getInstance().displayMessage(logsEntry);
                sentLogIds.add(logId);
              }
            }
          }
        });
      }
    }
  }

  /**
   * Retrieves the current money amount of the bot via GGFeatures.
   *
   * @param addon Addon instance
   * @param callback Callback returning the money value (0.0 if unavailable)
   * @throws ApiException if the API request fails
   */
  public static void getMoney(GGBot addon, Consumer<Double> callback) throws ApiException {
    if(!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.getmoney")) {
      callback.accept(0.0);
      return;
    }
    if (!GGBot.isAuth && GGBot.isExpired) {
      callback.accept(0.0);
      return;
    }

    GGBot.code = addon.configuration().token.get();

    ApiClient client = Configuration.getDefaultApiClient();
    client.setBasePath(GGBot.getInstance().getVersioningHandler().getBaseUrlForFeature("de.ggbot.addon.api.getmoney"));
    OAuth oauth = (OAuth) client.getAuthentication("oauth2");
    oauth.setAccessToken(GGBot.code);

    BotsApi bots = new BotsApi(client);
    long botId = Long.parseLong(GGBot.getInstance().configuration().botlist.get().replaceAll("\\D+", ""));

    for (Bot bot : bots.getAllBots()) {
      if (bot.getId() == botId) {
        String token = bot.getToken();
        ModulesApi modules = new ModulesApi(client);

          modules.getGGFeaturesMoneyAsync(token, new ApiCallback<>() {
          @Override
          public void onFailure(ApiException e, int statusCode, Map<String, List<String>> headers) {
            callback.accept(0.0);
          }

          @Override
          public void onSuccess(GetGGFeaturesMoney200Response result, int statusCode, Map<String, List<String>> headers) {
            callback.accept(result.getData() != null ? result.getData() : 0.0);
          }

          @Override public void onUploadProgress(long a, long b, boolean c) {}
          @Override public void onDownloadProgress(long a, long b, boolean c) {}
        });
        return;
      }
    }

    callback.accept(0.0);
  }

  /**
   * Retrieves the current bot health from the in-game API.
   *
   * @param addon Addon instance
   * @param callback Callback receiving the health value (0.0 if unavailable)
   * @throws ApiException if the API request fails
   */
  public static void getHealth(GGBot addon, Consumer<Double> callback) throws ApiException {
      if(!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.gethealth")) {
        callback.accept(0.0);
        return;
      }
    if (!GGBot.isAuth && GGBot.isExpired) {
      callback.accept(0.0);
      return;
    }

    GGBot.code = addon.configuration().token.get();

    ApiClient client = Configuration.getDefaultApiClient();
    client.setBasePath(GGBot.getInstance().getVersioningHandler().getBaseUrlForFeature("de.ggbot.addon.api.gethealth"));
    OAuth oauth = (OAuth) client.getAuthentication("oauth2");
    oauth.setAccessToken(GGBot.code);

    BotsApi bots = new BotsApi(client);
    long botId = Long.parseLong(GGBot.getInstance().configuration().botlist.get().replaceAll("\\D+", ""));

    for (Bot bot : bots.getAllBots()) {
      if (bot.getId() == botId) {
        String token = bot.getToken();
        IngameApi ingameApi = new IngameApi();
        ingameApi.getBotHealthAsync(token, new ApiCallback<>() {
          @Override
          public void onFailure(ApiException e, int statusCode,
              Map<String, List<String>> responseHeaders) {
            callback.accept(0.0);
          }

          @Override
          public void onSuccess(GetBotHealth200Response result, int statusCode,
              Map<String, List<String>> responseHeaders) {
            callback.accept(result.getData() != null ? result.getData() : 0.0);
          }

          @Override
          public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {}

          @Override
          public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {}
        });
        return;
      }
    }
  }

  /**
   * Retrieves the bot’s current Citybuild server name (e.g. "CB1").
   *
   * @param addon Addon instance
   * @param callback Callback receiving the citybuild string or "Unknown"
   * @throws ApiException if the API request fails
   */
  public static void getCitybuild(GGBot addon, Consumer<String> callback) throws ApiException {
      if(!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.getcitybuild")) {
        callback.accept("Unknown");
        return;
      }
    if (!GGBot.isAuth && GGBot.isExpired) {
      callback.accept("Unknown");
      return;
    }

    GGBot.code = addon.configuration().token.get();

    ApiClient client = Configuration.getDefaultApiClient();
    client.setBasePath(GGBot.getInstance().getVersioningHandler().getBaseUrlForFeature("de.ggbot.addon.api.getcitybuild"));
    OAuth oauth = (OAuth) client.getAuthentication("oauth2");
    oauth.setAccessToken(GGBot.code);

    BotsApi bots = new BotsApi(client);
    long botId = Long.parseLong(GGBot.getInstance().configuration().botlist.get().replaceAll("\\D+", ""));

    for (Bot bot : bots.getAllBots()) {
      if (bot.getId() == botId) {
        String token = bot.getToken();
        ModulesApi modules = new ModulesApi(client);

        modules.getGGFeaturesCityBuildAsync(token, new ApiCallback<>() {
          @Override
          public void onFailure(ApiException e, int statusCode, Map<String, List<String>> headers) {
            callback.accept("Unknown");
          }

          @Override
          public void onSuccess(GetGGFeaturesCityBuild200Response result, int statusCode,
              Map<String, List<String>> responseHeaders) {
            callback.accept(result.getData() != null ? result.getData() : "Unknown");
          }

          @Override public void onUploadProgress(long a, long b, boolean c) {}
          @Override public void onDownloadProgress(long a, long b, boolean c) {}
        });
        return;
      }
    }

    callback.accept("Unknown");
  }

  /**
   * Retrieves the bot’s current plot string (e.g. "0;0").
   *
   * @param addon Addon instance
   * @param callback Callback receiving the plot string or "Unknown"
   * @throws ApiException if the API request fails
   */
  public static void getPlot(GGBot addon, Consumer<String> callback) throws ApiException {
      if(!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.getplot")) {
        callback.accept("Unknown");
        return;
      }
    if (!GGBot.isAuth && GGBot.isExpired) {
      callback.accept("Unknown");
      return;
    }

    GGBot.code = addon.configuration().token.get();

    ApiClient client = Configuration.getDefaultApiClient();
    client.setBasePath(GGBot.getInstance().getVersioningHandler().getBaseUrlForFeature("de.ggbot.addon.api.getplot"));
    OAuth oauth = (OAuth) client.getAuthentication("oauth2");
    oauth.setAccessToken(GGBot.code);

    BotsApi bots = new BotsApi(client);
    long botId = Long.parseLong(GGBot.getInstance().configuration().botlist.get().replaceAll("\\D+", ""));

    for (Bot bot : bots.getAllBots()) {
      if (bot.getId() == botId) {
        String token = bot.getToken();
        ModulesApi modules = new ModulesApi(client);

        modules.getGGFeaturesCurrentPlotAsync(token, new ApiCallback<>() {
          @Override
          public void onFailure(ApiException e, int statusCode, Map<String, List<String>> headers) {
            callback.accept("Unknown");
          }

          @Override
          public void onSuccess(GetGGFeaturesCurrentPlot200Response result, int statusCode,
              Map<String, List<String>> responseHeaders) {
            callback.accept(result.getData() != null ? result.getData().getPlotString() : "Unknown");
          }

          @Override public void onUploadProgress(long a, long b, boolean c) {}
          @Override public void onDownloadProgress(long a, long b, boolean c) {}
        });
        return;
      }
    }

    callback.accept("Unknown");
  }

  /**
   * Fetches all tickets matching the given status (e.g. "OPEN", "CLOSED").
   *
   * @param addon Addon instance
   * @param statusEnum Ticket status filter
   * @param callback Callback returning the ticket list
   * @throws ApiException if the API request fails
   */
  public static void getTickets(GGBot addon, String statusEnum, Consumer<List<Ticket>> callback) throws ApiException {
      if(!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.gettickets")) {
        callback.accept(new ArrayList<>());
        return;
      }
    if (!GGBot.isAuth && GGBot.isExpired) {
      callback.accept(new ArrayList<>());
      return;
    }
    GGBot.code = addon.configuration().token.get();

    ApiClient client = Configuration.getDefaultApiClient();
    client.setBasePath(GGBot.getInstance().getVersioningHandler().getBaseUrlForFeature("de.ggbot.addon.api.gettickets"));
    OAuth oauth = (OAuth) client.getAuthentication("oauth2");
    oauth.setAccessToken(GGBot.code);

    BotsApi bots = new BotsApi(client);
    long botId = Long.parseLong(GGBot.getInstance().configuration().botlist.get().replaceAll("\\D+", ""));

    for (Bot bot : bots.getAllBots()) {
      if (bot.getId() == botId) {
        String token = bot.getToken();
        ModulesApi modules = new ModulesApi(client);
        modules.getTicketsAsync(token, statusEnum, new ApiCallback<>() {
          @Override
          public void onFailure(ApiException e, int statusCode,
              Map<String, List<String>> responseHeaders) {
              callback.accept(new ArrayList<>());
          }

          @Override
          public void onSuccess(List<Ticket> result, int statusCode,
              Map<String, List<String>> responseHeaders) {
              callback.accept(result);
          }

          @Override
          public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {

          }

          @Override
          public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {

          }
        });
        return;
      }
    }
    callback.accept(new ArrayList<>());
  }

  /**
   * Returns the display color matching a log level string.
   *
   * @param level Log level string
   * @return Corresponding TextColor
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
   * Formats a log level into a padded readable label (e.g. "[Info]").
   *
   * @param levelRaw Raw log level string
   * @return Formatted label
   */
  private static String formatLevel(String levelRaw) {
    String formatted = levelRaw.substring(0, 1).toUpperCase() +
        levelRaw.substring(1).toLowerCase();

    int width = 4;

    String space = "\u2007";

    int diff = width - formatted.length();
    if (diff < 0) diff = 0;

    return "[" + formatted + "]" + space.repeat(diff + 1);
  }

  /**
   * Formats a log timestamp to the user's local time zone.
   *
   * @param zoneId Target timezone
   * @param logEntry The log entry
   * @return Formatted timestamp string
   */
  public static String getTimestampForZone(ZoneId zoneId, BotLogEntry logEntry) {
    java.time.ZonedDateTime zoned = logEntry.getTimestampInstant().atZone(zoneId);
    java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd.MM HH:mm:ss");
    return zoned.format(fmt);
  }
}
