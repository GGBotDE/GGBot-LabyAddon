package de.ggbot.core.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import de.ggbot.core.GGBot;
import de.ggbot.core.cbx.AfkCbxManager;
import de.ggbot.core.cbx.CbxManager;
import de.ggbot.core.gui.botmenu.BotMenuState;
import de.ggbot.core.widget.afk.AfkBotsWidget;
import de.ggbot.core.widget.afk.AfkFleetsWidget;
import de.ggbot.sdk.api.AfkApi;
import de.ggbot.sdk.api.IngameApi;
import de.ggbot.sdk.api.ModulesApi;
import de.ggbot.sdk.core.ApiCallback;
import de.ggbot.sdk.core.JSON;
import de.ggbot.sdk.model.AfkBot;
import de.ggbot.sdk.model.AfkFleet;
import de.ggbot.sdk.model.AfkInspect;
import de.ggbot.sdk.model.BuyItem;
import de.ggbot.sdk.model.GetBotHealth200Response;
import de.ggbot.sdk.model.GetGGFeaturesCityBuild200Response;
import de.ggbot.sdk.model.GetGGFeaturesCurrentPlot200Response;
import de.ggbot.sdk.model.GetGGFeaturesMoney200Response;
import de.ggbot.sdk.model.GetKickAreas200Response;
import de.ggbot.sdk.model.GetMCControlStatus200Response;
import de.ggbot.sdk.model.KickArea;
import de.ggbot.sdk.model.ReplaySummary;
import de.ggbot.sdk.model.SellItem;
import de.ggbot.sdk.model.SendCommandToBotRequest;
import de.ggbot.sdk.model.UpdateModuleSettingRequest;
import de.ggbot.sdk.model.Ticket;
import de.ggbot.sdk.model.Ticket.StatusEnum;
import de.ggbot.sdk.model.TicketMessage;
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
import net.labymod.api.client.entity.player.Player;
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
import java.util.Objects;
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
   * Checks if the provided player matches the bot's linked account name.
   *
   * @param bot The bot object to compare against
   * @param player The player object to verify
   * @return True if the player's name matches the bot's linked name
   */
  public static boolean isAccountOf(Bot bot, Player player) {
    return bot != null && player != null && player.getName().equalsIgnoreCase(bot.getLinkName());
  }

  /**
   * Retrieves the bots used to display world overlays and cached shop items.
   * Includes all account bots when the "all bots" option is enabled;
   * otherwise, returns only the currently selected bot.
   *
   * @param addon The GGBot instance containing configuration settings
   * @return A list of all bots if 'all bots' is enabled, otherwise a list containing only the selected bot
   */
  public static List<Bot> getOverlayBots(GGBot addon) {
    if (Boolean.TRUE.equals(addon.configuration().botMenuSub.allBots.get())) {
      return getCachedBots();
    }
    Bot bot = getBotFromCache(addon);
    return bot == null ? Collections.emptyList() : Collections.singletonList(bot);
  }

  /**
   * Returns whether the selected bot is online according to the last cached data.
   * No network request is made - call {@link #updateBotList(GGBot)} first if
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
              createApiClient(addon, "de.ggbot.addon.api.updatebotlist")).getAllBots(null));
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
    String value = addon.configuration().botlist.get();
    // The selection is stored as "Name (id)". Parse the id from the trailing
    // parentheses so a name/description containing digits cannot corrupt the id.
    int open = value.lastIndexOf('(');
    int close = value.lastIndexOf(')');
    if (open >= 0 && close > open) {
      String idStr = value.substring(open + 1, close).replaceAll("\\D+", "");
      if (!idStr.isEmpty()) {
        return Long.parseLong(idStr);
      }
    }
    return Long.parseLong(value.replaceAll("\\D+", ""));
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
        createApiClient(addon, "de.ggbot.addon.api.updatebotlist")).getAllBots(null));
    // Let the CBX react to online-state changes (connect on offline-to-online,
    // tear down when the bot went offline).
    de.ggbot.core.cbx.CbxManager.get().onBotStatusRefresh();
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
    // A freshly started bot has no CBX session yet; give it time to log in.
    de.ggbot.core.cbx.CbxManager.get().onBotStarted();
  }

  /**
   * Starts a specific bot asynchronously.
   *
   * @param addon the addon instance
   * @param bot   the bot to start
   * @throws ApiException if the API request fails
   */
  public static void startBot(GGBot addon, Bot bot) throws ApiException {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.startbot")) return;
    new BotsApi(createApiClient(addon, "de.ggbot.addon.api.startbot"))
        .startBotAsync(bot.getToken(), Callbacks.START);
    // A freshly started bot has no CBX session yet; give it time to log in.
    de.ggbot.core.cbx.CbxManager.get().onBotStarted();
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
   * Stops a specific bot asynchronously.
   *
   * @param addon the addon instance
   * @param bot   the bot to stop
   * @throws ApiException if the API request fails
   */
  public static void stopBot(GGBot addon, Bot bot) throws ApiException {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.stopbot")) return;
    new BotsApi(createApiClient(addon, "de.ggbot.addon.api.stopbot"))
        .stopBotAsync(bot.getToken(), Callbacks.STOP);
  }

  // ===========================================================================
  // Module / settings helpers used by the bot management menu.
  //
  // These use the generated SDK (BotsApi / ModulesApi). The SDK is made tolerant
  // of backend field additions by SdkLeniency (installed at startup), which swaps
  // in a Gson without the strict per-model validation factories. Each call runs on
  // a daemon thread and delivers its result on the render thread; failures are
  // logged verbatim (the SDK's ApiException message includes the offending field
  // and the raw JSON) so the API can be adjusted.
  // ===========================================================================

  // Per-operation feature flags. Each call is gated by its flag (an unknown flag
  // defaults to enabled, see VersioningHandler) and resolves its base URL through
  // {@code getBaseUrlForFeature}, matching the convention used across the addon.
  private static final String F_GET_MODULES = "de.ggbot.addon.api.getmodules";
  private static final String F_GET_MODULE_SETTINGS = "de.ggbot.addon.api.getmodulesettings";
  private static final String F_UPDATE_MODULE_SETTING = "de.ggbot.addon.api.updatemodulesetting";
  private static final String F_GET_KICKAREAS = "de.ggbot.addon.api.getkickareas";
  private static final String F_GET_SHOPITEMS = "de.ggbot.addon.api.getshopitems";
  private static final String F_GET_LOGS = "de.ggbot.addon.api.getlogs";
  private static final String F_SEND_COMMAND = "de.ggbot.addon.api.sendcommandmenu";
  private static final String F_SELL_SCAN = "de.ggbot.addon.api.sellscan";
  private static final String F_BUY_SCAN = "de.ggbot.addon.api.buyscan";
  private static final String F_FOLLOW_PLAYER = "de.ggbot.addon.api.followplayer";
  private static final String F_STOP_FOLLOWING = "de.ggbot.addon.api.stopfollowing";
  private static final String F_SET_CONTROL_STATE = "de.ggbot.addon.api.setcontrolstate";
  private static final String F_ROTATE_BOT = "de.ggbot.addon.api.rotatebot";
  private static final String F_GET_BOT_LOCATION = "de.ggbot.addon.api.getbotlocation";
  private static final String F_CBX = "de.ggbot.addon.cbx";
  private static final String F_GET_AFK = "de.ggbot.addon.api.getafk";
  private static final String F_AFK_CONTROL = "de.ggbot.addon.api.afkcontrol";
  private static final String F_AFK_CHAT = "de.ggbot.addon.api.afkchat";
  private static final String F_AFK_INSPECT = "de.ggbot.addon.api.afkinspect";
  private static final String F_GET_FOOD = "de.ggbot.addon.api.getfood";
  private static final String F_GET_LEVEL = "de.ggbot.addon.api.getlevel";
  private static final String F_REPLAYS = "de.ggbot.addon.api.replays";
  private static final String F_MCCONTROL = "de.ggbot.addon.api.mccontrol";
  private static final String F_GET_REPLAY = "de.ggbot.addon.api.getreplay";
  private static final String F_SEARCH_REPLAYS = "de.ggbot.addon.api.searchreplays";
  private static final String F_DELETE_REPLAY = "de.ggbot.addon.api.deletereplay";

  /** Runs {@code work} on a daemon thread, then {@code onResult} on the render thread. */
  private static <T> void async(GGBot addon, ThrowingSupplier<T> work, Consumer<T> onResult, T fallback) {
    Thread t = new Thread(() -> {
      T value;
      try {
        value = work.get();
      } catch (Exception e) {
        // Pass the message as an argument so brace characters in JSON aren't
        // interpreted as log placeholders.
        addon.logger().error("Bot menu API call failed: {}", String.valueOf(e.getMessage()));
        value = fallback;
      }
      final T result = value;
      Laby.labyAPI().minecraft().executeOnRenderThread(() -> onResult.accept(result));
    }, "ggbot-botmenu");
    t.setDaemon(true);
    t.start();
  }

  @FunctionalInterface
  private interface ThrowingSupplier<T> {
    T get() throws Exception;
  }

  /**
   * Retrieves active module names for a bot, either from module data or offline settings, and passes them to a callback.
   *
   * @param addon The GGBot instance managing module access
   * @param bot The bot object to check for active modules
   * @param cb The callback to handle the retrieved module names
   */
  public static void getActiveModulesAsync(GGBot addon, Bot bot, Consumer<Set<String>> cb) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_GET_MODULES)) {
      cb.accept(null);
      return;
    }
    async(addon, () -> {
      return fetchActiveModules(addon, bot);
    }, cb, null);
  }

  private static Set<String> fetchActiveModules(GGBot addon, Bot bot) throws ApiException {
    BotsApi api = new BotsApi(createApiClient(addon, F_GET_MODULES));
    String body = null;
    try {
      body = rawBody(api.getBotModulesCall(bot.getToken(), null));
      if (ApiParsers.hasEnabledFlags(body)) return ApiParsers.parseModules(body);
    } catch (ApiException | IOException e) {

    }
    Map<String, String> settings = api.getBotSettings(bot.getToken());
    if (body != null && !ApiParsers.hasModuleKeys(settings)) {
      return ApiParsers.parseModules(body);
    }
    return ApiParsers.modulesFromSettings(settings);
  }

  /**
   * Clears previous bot data and initializes module information for the newly selected bot.
   *
   * @param addon The GGBot instance managing bot selection and module data
   * Ensures the active modules are updated when the selected bot changes
   */
  public static void onSelectedBotChanged(GGBot addon) {
    BotMenuState.get().resetForBotChange();
    Bot bot = getBotFromCache(addon);
    if (bot == null) return;
    getActiveModulesAsync(addon, bot, modules -> {
      Bot current = getBotFromCache(addon);
      if (current != null && Objects.equals(current.getId(), bot.getId())) {
        BotMenuState.get().setActiveModules(modules);
      }
    });
  }

  /**
   * Retrieves the raw response body from an API call, handling both successful and failed responses.
   *
   * @param call The API call object to execute
   * @return The raw response body as a string, or an empty string if none
   */
  private static String rawBody(Call call) throws ApiException, IOException {
    try (Response response = call.execute()) {
      String body = response.body() != null ? response.body().string() : "";
      if (!response.isSuccessful()) throw new ApiException(response.code(), body);
      return body;
    }
  }

  private static String rawApi(GGBot addon, String feature, String method, String path)
      throws ApiException, IOException {
    Request request = new Request.Builder()
        .url(addon.getVersioningHandler().getBaseUrlForFeature(feature) + path)
        .addHeader("Authorization", "Bearer " + addon.configuration().token.get())
        .method(method, null)
        .build();
    return rawBody(createApiClient(addon, feature).getHttpClient().newCall(request));
  }

  /**
   * Retrieves a bot's replays, most recent first.
   *
   * @param addon the addon
   * @param bot   the bot
   * @param cb    receives the replays, {@code null} if loading failed
   */
  public static void getReplaysAsync(GGBot addon, Bot bot, Consumer<List<ReplaySummary>> cb) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_REPLAYS)) {
      cb.accept(new ArrayList<>());
      return;
    }

    async(addon, () -> new ModulesApi(createApiClient(addon, F_REPLAYS))
        .listReplays(bot.getToken(), null, null), cb, null);
  }

  /**
   * Loads a replay together with access to its files, see {@link de.ggbot.core.replay.ReplayExport}.
   *
   * @param addon    the addon
   * @param bot      the bot
   * @param replayId the replay
   * @return the replay details as JSON; the replay itself has no model in the SDK
   */
  public static JsonObject getReplay(GGBot addon, Bot bot, String replayId)
      throws ApiException, IOException {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_GET_REPLAY)) {
      throw new IOException("Replays are disabled");
    }

    ModulesApi api = new ModulesApi(createApiClient(addon, F_GET_REPLAY));
    Object replay = api.getReplay(bot.getToken(), replayId);
    return JSON.getGson().toJsonTree(replay).getAsJsonObject();
  }

  /**
   * Queries the events of a bot's replays, see {@link de.ggbot.core.replay.ReplayEvents}.
   *
   * @param addon the addon
   * @param bot   the bot
   * @param query the query string, e.g. {@code text=diamond&kinds=1}
   * @return the snippets that may match, along with access to their files
   */
  public static JsonObject searchReplays(GGBot addon, Bot bot, String query)
      throws ApiException, IOException {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_SEARCH_REPLAYS)) {
      throw new IOException("Replays are disabled");
    }

    String path = "/bot/" + bot.getToken() + "/modules/replay/search?" + query;
    return JsonParser.parseString(rawApi(addon, F_SEARCH_REPLAYS, "GET", path)).getAsJsonObject();
  }

  /**
   * Deletes a replay belonging to a bot.
   *
   * @param addon    the addon
   * @param bot      the bot
   * @param replayId the replay
   * @param cb       receives whether the deletion succeeded
   */
  public static void deleteReplayAsync(GGBot addon, Bot bot, String replayId,
      Consumer<Boolean> cb) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_DELETE_REPLAY)) {
      cb.accept(false);
      return;
    }

    async(addon, () -> {
      ModulesApi api = new ModulesApi(createApiClient(addon, F_DELETE_REPLAY));
      api.deleteReplay(bot.getToken(), replayId);
      return true;
    }, cb, false);
  }

  /**
   * Determines the domain for joining a bot via MCControl, only while its proxy is running.
   *
   * @param addon the addon
   * @param bot   the bot
   * @param cb    receives the domain, {@code null} when it cannot be joined
   */
  public static void getMcControlDomainAsync(GGBot addon, Bot bot, Consumer<String> cb) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_MCCONTROL)) {
      cb.accept(null);
      return;
    }

    async(addon, () -> {
      ModulesApi api = new ModulesApi(createApiClient(addon, F_MCCONTROL));
      GetMCControlStatus200Response status = api.getMCControlStatus(bot.getToken());
      boolean running = status.getData() != null
          && Boolean.TRUE.equals(status.getData().getIsRunning());
      if (!running) return null;

      return ApiParsers.mcControlDomain(api.getMCControlDomains(bot.getToken()));
    }, cb, null);
  }

  /** Fetches the settings map for a single module (e.g. {@code sell}). */
  public static void getModuleSettingsAsync(GGBot addon, Bot bot, String module,
      Consumer<Map<String, String>> cb) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_GET_MODULE_SETTINGS)) {
      cb.accept(new java.util.HashMap<>());
      return;
    }
    async(addon, () -> {
      Map<String, String> map = new ModulesApi(createApiClient(addon, F_GET_MODULE_SETTINGS))
          .getModuleSettings(bot.getToken(), module);
      return map != null ? map : new java.util.HashMap<>();
    }, cb, new java.util.HashMap<>());
  }

  /** Updates a single module setting (key/value) and notifies on completion. */
  public static void updateModuleSettingAsync(GGBot addon, Bot bot, String module,
      String key, String value, Runnable onDone) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_UPDATE_MODULE_SETTING)) return;
    async(addon, () -> {
      new ModulesApi(createApiClient(addon, F_UPDATE_MODULE_SETTING))
          .updateModuleSetting(bot.getToken(), module,
              new UpdateModuleSettingRequest().key(key).value(value));
      return Boolean.TRUE;
    }, ok -> { if (onDone != null) onDone.run(); }, Boolean.FALSE);
  }

  /**
   * Fetches all kick areas (part of the ggfeatures module) through the SDK.
   *
   * <p>The SDK is made tolerant of backend field/number drift by {@link SdkLeniency},
   * so {@link ModulesApi#getKickAreas(String)} and its {@link KickArea} model are
   * used directly.
   */
  public static void getKickAreasAsync(GGBot addon, Bot bot, Consumer<List<KickArea>> cb) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_GET_KICKAREAS)) {
      cb.accept(new ArrayList<>());
      return;
    }
    async(addon, () -> {
      GetKickAreas200Response response =
          new ModulesApi(createApiClient(addon, F_GET_KICKAREAS)).getKickAreas(bot.getToken());
      if (response != null && response.getData() != null) {
        return response.getData();
      }
      return new ArrayList<KickArea>();
    }, cb, new ArrayList<>());
  }

  /** Fetches the bot's sell-shop items (with chest positions) through the SDK. */
  public static void getSellItemsAsync(GGBot addon, Bot bot, Consumer<List<SellItem>> cb) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_GET_SHOPITEMS)) {
      cb.accept(new ArrayList<>());
      return;
    }
    async(addon, () -> new ModulesApi(createApiClient(addon, F_GET_SHOPITEMS))
        .getPublicSellItems(bot.getToken()), cb, new ArrayList<>());
  }

  /** Fetches the bot's buy-shop items (with chest positions) through the SDK. */
  public static void getBuyItemsAsync(GGBot addon, Bot bot, Consumer<List<BuyItem>> cb) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_GET_SHOPITEMS)) {
      cb.accept(new ArrayList<>());
      return;
    }
    async(addon, () -> new ModulesApi(createApiClient(addon, F_GET_SHOPITEMS))
        .getPublicBuyItems(bot.getToken()), cb, new ArrayList<>());
  }

  /**
   * Fetches recent bot log lines via the SDK.
   *
   * <p>The SDK declares {@code getBotLogs} as {@code List<String>}, but the live
   * API may return objects ({@code {timestamp, level, message}}). When that
   * structural mismatch occurs the SDK call throws; we log it and fall back to a
   * lenient raw parse so the log still displays. Fix the API to return strings (or
   * regenerate the SDK) to use the typed path.
   */
  public static void getLogLinesAsync(GGBot addon, Bot bot, Consumer<List<String>> cb) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_GET_LOGS)) {
      cb.accept(new ArrayList<>());
      return;
    }
    async(addon, () -> {
      try {
        // Preferred path: the generated SDK.
        List<String> lines = new BotsApi(createApiClient(addon, F_GET_LOGS)).getBotLogs(bot.getToken());
        return lines != null ? lines : new ArrayList<String>();
      } catch (Exception sdkMismatch) {
        // Compatibility fallback: the SDK declares getBotLogs as List<String>, but
        // the backend currently returns log objects ({timestamp, level, message}),
        // which the typed call cannot represent. Parse those leniently so the log
        // still displays. Remove this once the backend returns plain strings.
        return rawLogLines(addon, bot);
      }
    }, cb, new ArrayList<>());
  }

  /** Lenient raw fetch+parse of the logs endpoint (compatibility fallback only). */
  private static List<String> rawLogLines(GGBot addon, Bot bot) throws IOException {
    List<String> lines = new ArrayList<>();
    ApiClient client = createApiClient(addon, F_GET_LOGS);
    OAuth oauth = (OAuth) client.getAuthentication("oauth2");
    String baseUrl = addon.getVersioningHandler().getBaseUrlForFeature(F_GET_LOGS);
    Request request = new Request.Builder()
        .url(baseUrl + "/bot/" + bot.getToken() + "/logs")
        .addHeader("Authorization", "Bearer " + oauth.getAccessToken())
        .build();
    try (Response response = new OkHttpClient().newCall(request).execute()) {
      if (response.body() == null) return lines;
      String body = response.body().string();
      if (body.isEmpty()) return lines;
      com.google.gson.JsonElement el = com.google.gson.JsonParser.parseString(body);
      com.google.gson.JsonArray arr = null;
      if (el.isJsonArray()) {
        arr = el.getAsJsonArray();
      } else if (el.isJsonObject() && el.getAsJsonObject().has("data")
          && el.getAsJsonObject().get("data").isJsonArray()) {
        arr = el.getAsJsonObject().getAsJsonArray("data");
      }
      if (arr != null) {
        for (com.google.gson.JsonElement e : arr) {
          if (e.isJsonPrimitive()) {
            lines.add(e.getAsString());
          } else if (e.isJsonObject()) {
            com.google.gson.JsonObject o = e.getAsJsonObject();
            String level = o.has("level") && !o.get("level").isJsonNull()
                ? o.get("level").getAsString() : null;
            String msg = o.has("message") && !o.get("message").isJsonNull()
                ? o.get("message").getAsString() : o.toString();
            lines.add((level != null ? "[" + level + "] " : "") + msg);
          }
        }
      }
    }
    return lines;
  }

  /** Sends a raw command to a specific bot asynchronously. */
  public static void sendCommandToBot(GGBot addon, Bot bot, String command) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_SEND_COMMAND)) return;
    async(addon, () -> {
      new BotsApi(createApiClient(addon, F_SEND_COMMAND)).sendCommandToBotAsync(bot.getToken(),
          new SendCommandToBotRequest().command(command), Callbacks.SEND_COMMAND);
      return Boolean.TRUE;
    }, ok -> {}, Boolean.FALSE);
  }

  /** Triggers a sell-chest scan (sell module). */
  public static void startSellScanAsync(GGBot addon, Bot bot) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_SELL_SCAN)) return;
    async(addon, () -> new ModulesApi(createApiClient(addon, F_SELL_SCAN))
        .startSellItemScan(bot.getToken()), r -> {}, null);
  }

  /** Triggers a buy-chest scan (buy module). */
  public static void startBuyScanAsync(GGBot addon, Bot bot) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_BUY_SCAN)) return;
    async(addon, () -> new ModulesApi(createApiClient(addon, F_BUY_SCAN))
        .startBuyItemScan(bot.getToken()), r -> {}, null);
  }

  /** Makes the bot follow the given player (utils module). */
  public static void followPlayerAsync(GGBot addon, Bot bot, String playerName) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_FOLLOW_PLAYER)) return;
    async(addon, () -> new IngameApi(createApiClient(addon, F_FOLLOW_PLAYER))
        .followPlayer(bot.getToken(),
            new de.ggbot.sdk.model.FollowPlayerRequest().playerName(playerName)), r -> {}, null);
  }

  /** Stops the bot from following. */
  public static void stopFollowingAsync(GGBot addon, Bot bot) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_STOP_FOLLOWING)) return;
    async(addon, () -> new IngameApi(createApiClient(addon, F_STOP_FOLLOWING))
        .stopFollowing(bot.getToken()), r -> {}, null);
  }

  // ---- control-mode (synchronous; called from a dedicated control thread) ----
  // Every control call prefers the live CBX connection and transparently
  // falls back to the HTTP endpoint whenever CBX is disabled, disconnected
  // or mid-reconnect, so control keeps working in every scenario.

  /** Sets a single movement control state on the bot (CBX first, endpoint fallback). */
  public static void setControlState(GGBot addon, Bot bot, String control, boolean state)
      throws ApiException {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_SET_CONTROL_STATE)) return;
    if (de.ggbot.core.cbx.CbxManager.get().trySetControlState(bot, control, state)) return;
    new IngameApi(createApiClient(addon, F_SET_CONTROL_STATE)).setControlState(bot.getToken(),
        new de.ggbot.sdk.model.ControlStateRequest()
            .control(de.ggbot.sdk.model.ControlStateRequest.ControlEnum.fromValue(control))
            .state(state));
  }

  /** Rotates the bot's view (CBX first, endpoint fallback). */
  public static void rotateBot(GGBot addon, Bot bot, float yaw, float pitch) throws ApiException {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_ROTATE_BOT)) return;
    if (de.ggbot.core.cbx.CbxManager.get().tryRotateBot(bot, yaw, pitch)) return;
    new BotsApi(createApiClient(addon, F_ROTATE_BOT)).rotateBot(bot.getToken(), yaw, pitch);
  }

  /**
   * Creates a CBX (Client Bot Exchange) session for the given bot via the SDK.
   * Returns the session details (connection token and where to connect to), or
   * {@code null} when the feature is disabled.
   *
   * @param addon the addon instance
   * @param bot   the bot to open a session for
   * @return the CBX session, or {@code null}
   * @throws ApiException if the API request fails (e.g. the bot is offline)
   */
  public static de.ggbot.sdk.model.CbxSession createCbxSession(GGBot addon, Bot bot)
      throws ApiException {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_CBX)) return null;
    return new IngameApi(createApiClient(addon, F_CBX)).createCbxSession(bot.getToken());
  }

  /**
   * Generates a CBX session for AFK bots associated with the account, returning the raw JSON response.
   *
   * @param addon The GGBot instance managing CBX session creation
   * @return The raw JSON string representing the CBX session data
   */
  public static String createAfkCbxSession(GGBot addon) throws ApiException, IOException {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_CBX)) return null;
    return rawBody(new AfkApi(createApiClient(addon, F_CBX)).createAfkCbxSessionCall(null));
  }

  private static void afkPost(GGBot addon, String feature, List<String> paths, JsonObject body) {
    async(addon, () -> {
      String base = addon.getVersioningHandler().getBaseUrlForFeature(feature);
      String token = addon.configuration().token.get();
      AfkHttp.forEach(paths, path -> AfkHttp.post(base + path, token, body));
      return true;
    }, done -> { }, false);
  }

  private static List<String> botPaths(List<String> tokens, String action) {
    List<String> paths = new ArrayList<>();
    for (String token : tokens) {
      paths.add("/afk/bots/" + token + action);
    }
    return paths;
  }

  /**
   * Fetches AFK fleets and bots for the account, providing an empty overview if the feature is disabled or the request fails.
   *
   * @param addon The GGBot instance managing AFK data retrieval
   * @param cb The callback to handle the retrieved AfkOverview data
   */
  public static void getAfkOverviewAsync(GGBot addon, Consumer<AfkOverview> cb) {
    AfkOverview empty = new AfkOverview(new ArrayList<>(), new ArrayList<>());
    if (!addon.getVersioningHandler().isFeatureEnabled(F_GET_AFK)) {
      cb.accept(empty);
      return;
    }
    async(addon, () -> {
      AfkApi api = new AfkApi(createApiClient(addon, F_GET_AFK));
      return new AfkOverview(ApiParsers.parseFleets(rawBody(api.getAfkFleetsCall(null))),
          ApiParsers.parseBots(rawBody(api.getAfkBotsCall(null, null))));
    }, overview -> {
      if (overview == null) {
        cb.accept(empty);
        return;
      }
      updateAfkWidgets(overview.getBots());
      cb.accept(overview);
    }, null);
  }

  /**
   * Refreshes AFK widgets by retrieving bot data in a single API call.
   *
   * @param addon The GGBot instance managing AFK widget updates
   */
  public static void refreshAfkWidgets(GGBot addon) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_GET_AFK)) return;
    async(addon, () -> ApiParsers.parseBots(
        rawBody(new AfkApi(createApiClient(addon, F_GET_AFK)).getAfkBotsCall(null, null))), bots -> {
      if (bots != null) updateAfkWidgets(bots);
    }, null);
  }

  private static void updateAfkWidgets(List<AfkBot> bots) {
    Set<Integer> fleets = new HashSet<>();
    Set<Integer> activeFleets = new HashSet<>();
    int online = 0;
    for (AfkBot bot : bots) {
      fleets.add(bot.getFleet());
      if (AfkOverview.isOnline(bot)) {
        online++;
        activeFleets.add(bot.getFleet());
      }
    }
    AfkBotsWidget.update(online + "/" + bots.size());
    AfkFleetsWidget.update(activeFleets.size() + "/" + fleets.size());
  }

  /**
   * Initiates the startup of all bots within a specified fleet, with the completion handler executed on the render thread.
   *
   * @param addon The GGBot instance managing AFK fleet operations
   * @param fleet The AfkFleet object representing the fleet to start
   * @param onDone The Runnable to execute upon successful completion of the fleet start
   */
  public static void startAfkFleetAsync(GGBot addon, AfkFleet fleet, Runnable onDone) {
    afkControl(addon, api -> api.startAfkFleet(fleet.getId()), onDone);
  }

  /**
   * Initiates the shutdown of all bots within a specified fleet, with the completion handler executed on the render thread.
   *
   * @param addon The GGBot instance managing AFK fleet operations
   * @param fleet The AfkFleet object representing the fleet to stop
   * @param onDone The Runnable to execute upon successful completion of the fleet stop
   */
  public static void stopAfkFleetAsync(GGBot addon, AfkFleet fleet, Runnable onDone) {
    afkControl(addon, api -> api.stopAfkFleet(fleet.getId()), onDone);
  }

  /**
   * Starts specified AFK bots, with the completion handler executed on the render thread.
   *
   * @param addon The GGBot instance managing AFK bot operations
   * @param tokens The list of bot tokens to start
   * @param onDone The Runnable to execute upon successful completion of bot startup
   */
  public static void startAfkBotsAsync(GGBot addon, List<String> tokens, Runnable onDone) {
    afkControl(addon, api -> AfkHttp.forEach(tokens, api::startAfkBot), onDone);
  }

  /**
   * Stops specified AFK bots, with the completion handler executed on the render thread.
   *
   * @param addon The GGBot instance managing AFK bot operations
   * @param tokens The list of bot tokens to stop
   * @param onDone The Runnable to execute upon successful completion of bot shutdown
   */
  public static void stopAfkBotsAsync(GGBot addon, List<String> tokens, Runnable onDone) {
    afkControl(addon, api -> AfkHttp.forEach(tokens, api::stopAfkBot), onDone);
  }

  private static void afkControl(GGBot addon, AfkAction action, Runnable onDone) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_AFK_CONTROL)) {
      Laby.labyAPI().minecraft().executeOnRenderThread(onDone);
      return;
    }
    async(addon, () -> {
      action.run(new AfkApi(createApiClient(addon, F_AFK_CONTROL)));
      return true;
    }, done -> onDone.run(), false);
  }

  @FunctionalInterface
  private interface AfkAction {
    void run(AfkApi api) throws Exception;
  }

  /**
   * Sends a message to online bots within a fleet, prioritizing CBX if available.
   *
   * @param addon The GGBot instance managing AFK fleet communication
   * @param fleet The AfkFleet object representing the target fleet
   * @param onlineTokens The list of tokens for online bots in the fleet
   * @param text The message content to send to the bots
   */
  public static void chatAfkFleetAsync(GGBot addon, AfkFleet fleet, List<String> onlineTokens,
      String text) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_AFK_CHAT)) return;
    if (AfkCbxManager.get().tryChat(onlineTokens, text)) return;
    JsonObject body = new JsonObject();
    body.addProperty("message", text);
    afkPost(addon, F_AFK_CHAT, List.of("/afk/fleets/" + fleet.getId() + "/chat"), body);
  }

  /**
   * Sends a message to AFK bots, utilizing CBX for efficient delivery if available.
   *
   * @param addon The GGBot instance managing AFK bot communication
   * @param tokens The list of bot tokens to message
   * @param text The message content to send to the bots
   */
  public static void chatAfkBotsAsync(GGBot addon, List<String> tokens, String text) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_AFK_CHAT)) return;
    if (AfkCbxManager.get().tryChat(tokens, text)) return;
    JsonObject body = new JsonObject();
    body.addProperty("message", text);
    afkPost(addon, F_AFK_CHAT, botPaths(tokens, "/chat"), body);
  }

  /**
   * Directs AFK bots to move with specified forward, strafe, turn, and jump parameters.
   *
   * @param addon The GGBot instance managing AFK bot control
   * @param tokens The list of bot tokens to control
   * @param forward The amount to move forward
   * @param strafe The amount to move sideways
   * @param turn The amount to rotate
   * @param jump Whether to perform a jump action
   */
  public static void walkAfkBotsAsync(GGBot addon, List<String> tokens, double forward,
      double strafe, double turn, boolean jump) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_AFK_CONTROL)) return;
    if (AfkCbxManager.get().tryWalk(tokens, forward, strafe, turn, jump)) return;
    JsonObject body = new JsonObject();
    body.addProperty("forward", forward);
    body.addProperty("strafe", strafe);
    body.addProperty("turn", turn);
    body.addProperty("jump", jump);
    afkPost(addon, F_AFK_CONTROL, botPaths(tokens, "/walk"), body);
  }

  /**
   * Initiates an inspection of an AFK bot via CBX, returning null if no response is received.
   *
   * @param addon The GGBot instance managing AFK bot inspection
   * @param bot The AfkBot object to inspect
   * @param cb The callback to handle the inspection result
   */
  public static void inspectAfkBotAsync(GGBot addon, AfkBot bot, Consumer<AfkInspect> cb) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_AFK_INSPECT)) {
      cb.accept(null);
      return;
    }
    boolean sent = AfkCbxManager.get().tryInspect(bot.getToken(), result ->
        Laby.labyAPI().minecraft().executeOnRenderThread(() -> cb.accept(result)));
    if (sent) return;
    async(addon, () -> new AfkApi(createApiClient(addon, F_AFK_INSPECT))
        .inspectAfkBot(bot.getToken()), cb, null);
  }

  /**
   * Fetches the food level of a bot using CBX if available, returning null if the operation fails.
   *
   * @param addon The GGBot instance managing food level retrieval
   * @param bot The Bot object to check for food level
   * @return The food level as a double value, or null if the operation fails
   */
  public static Double getFood(GGBot addon, Bot bot) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_GET_FOOD)) return null;
    double live = CbxManager.get().getLiveFood(bot);
    if (live >= 0) return live;
    try {
      return new IngameApi(createApiClient(addon, F_GET_FOOD)).getBotFood(bot.getToken()).getData();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Retrieves the experience level of a bot using CBX if available, returning null if the operation fails.
   *
   * @param addon The GGBot instance managing experience level retrieval
   * @param bot The Bot object to check for experience level
   * @return The experience level as an integer value, or null if the operation fails
   */
  public static Integer getLevel(GGBot addon, Bot bot) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_GET_LEVEL)) return null;
    int live = CbxManager.get().getLiveLevel(bot);
    if (live >= 0) return live;
    try {
      var data = new IngameApi(createApiClient(addon, F_GET_LEVEL))
          .getBotExperience(bot.getToken()).getData();
      return data != null ? data.getLevel() : null;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Returns the bot's current world position, or {@code null} on failure.
   * Served from the live CBX movement subscription when fresh data is
   * available, otherwise from the HTTP endpoint.
   */
  public static de.ggbot.sdk.model.Position getBotLocation(GGBot addon, Bot bot) {
    if (!addon.getVersioningHandler().isFeatureEnabled(F_GET_BOT_LOCATION)) return null;
    de.ggbot.sdk.model.Position live = de.ggbot.core.cbx.CbxManager.get().getLiveLocation(bot);
    if (live != null) return live;
    try {
      var resp = new IngameApi(createApiClient(addon, F_GET_BOT_LOCATION)).getBotLocation(bot.getToken());
      return resp != null ? resp.getData() : null;
    } catch (Exception e) {
      return null;
    }
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
    for (Bot bot : new BotsApi(client).getAllBots(null)) {
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
        if (body == null || body.isEmpty()) return;

        // The endpoint may return either a bare array or an object wrapping the
        // array under "data" ({"success":true,"data":[...]}). Unwrap if needed.
        com.google.gson.JsonElement root = com.google.gson.JsonParser.parseString(body);
        com.google.gson.JsonElement arrayElement = root;
        if (root.isJsonObject() && root.getAsJsonObject().has("data")
            && root.getAsJsonObject().get("data").isJsonArray()) {
          arrayElement = root.getAsJsonObject().get("data");
        }
        if (!arrayElement.isJsonArray()) return;

        Type listType = new TypeToken<List<BotLogEntry>>() {}.getType();
        List<BotLogEntry> entries = new Gson().fromJson(arrayElement, listType);
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
    double live = CbxManager.get().getLiveHealth(bot);
    if (live >= 0) { callback.accept(live); return; }
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
   * Fetches the messages of a single ticket. The callback is always invoked on the
   * render thread so it can update the UI directly.
   *
   * @param addon    the addon instance
   * @param ticketId the ticket ID
   * @param callback receives the messages (empty on any error)
   */
  public static void getTicketMessagesAsync(GGBot addon, int ticketId,
      Consumer<List<TicketMessage>> callback) {
    Runnable empty = () -> Laby.labyAPI().minecraft()
        .executeOnRenderThread(() -> callback.accept(new ArrayList<>()));
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.getticketmessages")
        || !GGBot.isAuthenticated()) {
      empty.run();
      return;
    }
    Bot bot = getBotFromCache(addon);
    if (bot == null) {
      empty.run();
      return;
    }
    try {
      ApiClient client = createApiClient(addon, "de.ggbot.addon.api.getticketmessages");
      new ModulesApi(client).getTicketMessagesAsync(bot.getToken(), ticketId, new ApiCallback<>() {
        @Override public void onFailure(ApiException e, int s, Map<String, List<String>> h) { empty.run(); }
        @Override public void onSuccess(List<TicketMessage> r, int s, Map<String, List<String>> h) {
          Laby.labyAPI().minecraft().executeOnRenderThread(() -> callback.accept(r));
        }
        @Override public void onUploadProgress(long a, long b, boolean c) {}
        @Override public void onDownloadProgress(long a, long b, boolean c) {}
      });
    } catch (ApiException e) {
      empty.run();
    }
  }

  /**
   * Fetches tickets, always invoking the callback on the render thread (for UI use).
   *
   * @param addon      the addon instance
   * @param statusEnum status filter ({@code "open"}, {@code "closed"} or empty)
   * @param callback   receives the tickets (empty on any error)
   */
  public static void getTicketsForMenuAsync(GGBot addon, String statusEnum,
      Consumer<List<Ticket>> callback) {
    try {
      getTickets(addon, statusEnum, tickets ->
          Laby.labyAPI().minecraft().executeOnRenderThread(() -> callback.accept(tickets)));
    } catch (ApiException e) {
      Laby.labyAPI().minecraft().executeOnRenderThread(() -> callback.accept(new ArrayList<>()));
    }
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
