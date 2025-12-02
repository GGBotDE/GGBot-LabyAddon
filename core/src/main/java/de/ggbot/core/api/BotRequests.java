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
 * Verwaltet Bot-bezogene API-Anfragen wie Abruf, Start und Stopp.
 */
public class BotRequests {

  /**
   * Liste aller Bots, geladen aus der API.
   */
  public static List<Bot> bots = new ArrayList<>();

  /**
   * Aktualisiert die Botliste über die API.
   *
   * @param addon Instanz des Addons, um Token zu laden
   * @throws ApiException falls die API-Anfrage fehlschlägt
   */
  public static void updateBotList(GGBot addon) throws ApiException {
    GGBot.code = addon.configuration().token.get();
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.ggbot.de/api");
    OAuth oauth2 = (OAuth) defaultClient.getAuthentication("oauth2");
    oauth2.setAccessToken(GGBot.code);

    BotsApi api = new BotsApi(defaultClient);
    bots = api.getAllBots();
  }

  public static String getStatus(GGBot addon) throws ApiException {
    GGBot.code = addon.configuration().token.get();
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.ggbot.de/api");
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
  public static String getName(GGBot addon) throws ApiException {
    GGBot.code = addon.configuration().token.get();
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.ggbot.de/api");
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
   * Startet den ausgewählten Bot asynchron.
   *
   * @param addon Instanz des Addons, um Token und Botliste zu lesen
   * @throws ApiException falls die API-Anfrage fehlschlägt
   */
  public static void startBot(GGBot addon) throws ApiException {
    GGBot.code = addon.configuration().token.get();
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.ggbot.de/api");
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
   * Stoppt den ausgewählten Bot asynchron.
   *
   * @param addon Instanz des Addons, um Token und Botliste zu lesen
   * @throws ApiException falls die API-Anfrage fehlschlägt
   */
  public static void stopBot(GGBot addon) throws ApiException {
    GGBot.code = addon.configuration().token.get();
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.ggbot.de/api");
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
   * Prüft, ob der konfigurierte Bot online ist.
   *
   * @param addon GGBot Addon-Instanz
   * @return true wenn Bot online, sonst false
   * @throws ApiException wenn API-Aufruf fehlschlägt
   */
  public static boolean isOnline(GGBot addon) throws ApiException {
    GGBot.code = addon.configuration().token.get();
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.ggbot.de/api");
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

  public static void sendCommand(GGBot addon, String command) throws ApiException {
    GGBot.code = addon.configuration().token.get();
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.ggbot.de/api");
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
  public static final Set<String> sentLogIds = new HashSet<>();

  public static void logsAsync() throws ApiException {
    if(!GGBot.isAuth && GGBot.isExpired){
      return;
    }
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://api.ggbot.de/api");
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
            .url("https://api.ggbot.de/api/bot/" + token + "/logs")
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

  public static void getMoney(GGBot addon, Consumer<Double> callback) throws ApiException {
    if (!GGBot.isAuth && GGBot.isExpired) {
      callback.accept(0.0);
      return;
    }

    GGBot.code = addon.configuration().token.get();

    ApiClient client = Configuration.getDefaultApiClient();
    client.setBasePath("https://api.ggbot.de/api");
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

  public static void getHealth(GGBot addon, Consumer<Double> callback) throws ApiException {
    if (!GGBot.isAuth && GGBot.isExpired) {
      callback.accept(0.0);
      return;
    }

    GGBot.code = addon.configuration().token.get();

    ApiClient client = Configuration.getDefaultApiClient();
    client.setBasePath("https://api.ggbot.de/api");
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
  public static void getCitybuild(GGBot addon, Consumer<String> callback) throws ApiException {
    if (!GGBot.isAuth && GGBot.isExpired) {
      callback.accept("Unknown");
      return;
    }

    GGBot.code = addon.configuration().token.get();

    ApiClient client = Configuration.getDefaultApiClient();
    client.setBasePath("https://api.ggbot.de/api");
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

  public static void getPlot(GGBot addon, Consumer<String> callback) throws ApiException {
    if (!GGBot.isAuth && GGBot.isExpired) {
      callback.accept("Unknown");
      return;
    }

    GGBot.code = addon.configuration().token.get();

    ApiClient client = Configuration.getDefaultApiClient();
    client.setBasePath("https://api.ggbot.de/api");
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

  public static void getTickets(GGBot addon, String statusEnum, Consumer<List<Ticket>> callback) throws ApiException {
    if (!GGBot.isAuth && GGBot.isExpired) {
      callback.accept(new ArrayList<>());
      return;
    }
    GGBot.code = addon.configuration().token.get();

    ApiClient client = Configuration.getDefaultApiClient();
    client.setBasePath("https://api.ggbot.de/api");
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
  private static String formatLevel(String levelRaw) {
    String formatted = levelRaw.substring(0, 1).toUpperCase() +
        levelRaw.substring(1).toLowerCase();

    int width = 4;

    String space = "\u2007";

    int diff = width - formatted.length();
    if (diff < 0) diff = 0;

    return "[" + formatted + "]" + space.repeat(diff + 1);
  }
  public static String getTimestampForZone(ZoneId zoneId, BotLogEntry logEntry) {
    java.time.ZonedDateTime zoned = logEntry.getTimestampInstant().atZone(zoneId);
    java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd.MM HH:mm:ss");
    return zoned.format(fmt);
  }
}
