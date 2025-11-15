package de.ggbot.core.api;

import de.ggbot.core.GGBot;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.Configuration;
import org.openapitools.client.api.BotsApi;
import org.openapitools.client.auth.OAuth;
import org.openapitools.client.model.Bot;
import org.openapitools.client.model.SendCommandToBotRequest;
import java.util.ArrayList;
import java.util.List;

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
}
