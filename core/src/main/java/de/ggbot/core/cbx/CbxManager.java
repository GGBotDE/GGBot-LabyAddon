package de.ggbot.core.cbx;

import com.google.gson.JsonObject;
import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import de.ggbot.core.widget.ingame.FoodWidget;
import de.ggbot.core.widget.ingame.HealthWidget;
import de.ggbot.core.widget.ingame.LevelWidget;
import de.ggbot.core.widget.ingame.PlayersWidget;
import de.ggbot.core.widget.ingame.PositionWidget;
import de.ggbot.sdk.model.Bot;
import de.ggbot.sdk.model.Position;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.labymod.api.client.component.Component;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.network.server.ServerDisconnectEvent;
import net.labymod.api.event.client.network.server.ServerJoinEvent;

/**
 * Lifecycle owner of the CBX (Client Bot Exchange) connection for the
 * currently selected bot.
 *
 * <p>Connection policy:
 * <ul>
 *   <li>{@code de.ggbot.addon.cbx.onlogin} enabled: connect eagerly on server
 *       join (and on bot start / offline-to-online transitions), so widgets
 *       like health update instantly from the live pushes.</li>
 *   <li>only {@code de.ggbot.addon.cbx} enabled: connect lazily when the
 *       control feature is entered.</li>
 *   <li>both disabled: CBX is never used; every consumer falls back to the
 *       HTTP endpoints (the fallback exists on every path regardless).</li>
 * </ul>
 *
 * <p>The CBX may end the connection at any point. The manager reconnects with
 * an incrementally growing backoff (a fresh session token is requested from
 * the API for every attempt, since tokens are short-lived) and gives up
 * whenever the prerequisites stop holding: addon disabled, logged out, no
 * server connection, or the bot going offline. Disabling the addon
 * disconnects immediately; re-enabling connects again under the same policy.
 */
public final class CbxManager {

  private static final String F_CBX = "de.ggbot.addon.cbx";
  private static final String F_CBX_ON_LOGIN = "de.ggbot.addon.cbx.onlogin";

  /** Backoff: first retry after 3s, then x1.6 per attempt, capped at 5min. */
  private static final long BACKOFF_INITIAL_MS = 3_000L;
  private static final double BACKOFF_FACTOR = 1.6;
  private static final long BACKOFF_MAX_MS = 300_000L;

  /** Delay before connecting after a bot start (the bot needs to log in). */
  private static final long BOT_START_CONNECT_DELAY_MS = 5_000L;

  /**
   * Limits the frequency of HUD updates to reduce resource usage.
   */
  private static final long POSITION_WIDGET_INTERVAL_MS = 1_000L;

  private static final CbxManager INSTANCE = new CbxManager();

  public static CbxManager get() {
    return INSTANCE;
  }

  private final ScheduledExecutorService executor =
      Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ggbot-cbx");
        t.setDaemon(true);
        return t;
      });

  private final BotLiveState liveState = new BotLiveState();

  private volatile CbxClient client;
  private volatile String connectedBotToken;

  /** Whether a connection is currently wanted (drives reconnects). */
  private volatile boolean desired = false;

  /** Whether the control feature is active (drives the movement subscription). */
  private volatile boolean controlActive = false;

  private volatile boolean movementSubscribed = false;
  private long lastPositionUpdate = 0L;

  private volatile Boolean lastKnownOnline = null;
  private int reconnectAttempt = 0;
  private ScheduledFuture<?> pendingConnect;

  /**
   * Whether the current "cannot connect" episode was already logged. Keeps the
   * log to one line per episode instead of one per retry, since every retry
   * failing is expected and harmless (the HTTP endpoints cover everything).
   */
  private boolean loggedConnectFailure = false;

  private CbxManager() {
  }

  // ---- lifecycle events ----------------------------------------------------

  /** Eager connect on server join when the on-login flag allows it. */
  @Subscribe
  public void onServerJoin(ServerJoinEvent event) {
    requestEagerConnect();
  }

  /** Server leave ends the session; a rejoin starts a fresh one. */
  @Subscribe
  public void onServerDisconnect(ServerDisconnectEvent event) {
    stopConnection("server disconnect");
  }

  /** Called when the addon's enabled switch is turned on. */
  public void onAddonEnabled() {
    requestEagerConnect();
  }

  /** Called when the addon's enabled switch is turned off. */
  public void onAddonDisabled() {
    stopConnection("addon disabled");
  }

  /** Called after the user started the selected bot. */
  public void onBotStarted() {
    if (!eagerAllowed()) return;
    startAttempt(BOT_START_CONNECT_DELAY_MS);
  }

  /**
   * Called after every bot list refresh. Connects on an offline-to-online
   * transition (a bot that was offline cannot have had a CBX session) and
   * tears the session down when the bot went offline.
   */
  public void onBotStatusRefresh() {
    GGBot addon = GGBot.getInstance();
    if (addon == null) return;
    Bot bot = BotRequests.getBotFromCache(addon);
    boolean online = bot != null && Boolean.TRUE.equals(bot.getOnline());
    Boolean previous = lastKnownOnline;
    lastKnownOnline = online;

    if (!online) {
      stopConnection("bot offline");
      return;
    }
    if ((previous == null || !previous) && eagerAllowed()) {
      // Offline -> online: a fresh, prompt connect attempt.
      startAttempt(0);
    } else if (desired && !isConnected() && !hasPendingAttempt()) {
      // A wanted connection whose retry loop has gone idle (e.g. after a
      // transient prerequisite miss): resume it without resetting the backoff,
      // so this frequent hook cannot turn into a fast retry storm.
      scheduleReconnect();
    }
    syncMovementSubscription();
  }

  /** Called when the user selects a different bot; the session is per bot. */
  public void onSelectedBotChanged() {
    stopConnection("bot changed");
    lastKnownOnline = null;
    requestEagerConnect();
  }

  /**
   * Called when the control feature starts. Connects lazily when the eager
   * on-login flag is disabled but CBX itself is allowed; with CBX fully
   * disabled this is a no-op and the control feature stays on the HTTP
   * endpoints.
   */
  public void onControlStarted() {
    controlActive = true;
    if (!cbxAllowed()) return;
    if (isConnected()) {
      desired = true;
      syncMovementSubscription();
    } else {
      startAttempt(0);
    }
  }

  /** Called when the control feature ends; drops the movement subscription. */
  public void onControlStopped() {
    controlActive = false;
    syncMovementSubscription();
    // Keep the connection: the hud pushes stay useful, and if it was only
    // opened for control it dies with the next lifecycle event anyway.
  }

  // ---- data access for the request layer -----------------------------------

  /** Returns whether a session for the given bot is currently open. */
  public boolean isConnectedFor(Bot bot) {
    return clientFor(bot) != null;
  }

  /**
   * Returns the open client of the given bot's session, or {@code null} when
   * there is none. Callers must send on the returned reference instead of on
   * the field, since a concurrent disconnect or teardown clears the field at
   * any point.
   */
  private CbxClient clientFor(Bot bot) {
    CbxClient current = client;
    if (current == null || !current.isOpen()) return null;
    if (bot == null || bot.getToken() == null
        || !bot.getToken().equals(connectedBotToken)) {
      return null;
    }
    return current;
  }

  /**
   * Sends a control state over CBX.
   *
   * @return {@code true} when sent; {@code false} means "use the endpoint"
   */
  public boolean trySetControlState(Bot bot, String control, boolean state) {
    CbxClient current = clientFor(bot);
    if (current == null) return false;
    JsonObject data = new JsonObject();
    data.addProperty("control", control);
    data.addProperty("state", state);
    return current.send("movement", "setControlState", data);
  }

  /**
   * Sends an absolute rotation over CBX. Values use the exact same convention
   * as the HTTP rotate endpoint (both feed {@code bot.setLook}).
   *
   * @return {@code true} when sent; {@code false} means "use the endpoint"
   */
  public boolean tryRotateBot(Bot bot, float yaw, float pitch) {
    CbxClient current = clientFor(bot);
    if (current == null) return false;
    JsonObject data = new JsonObject();
    data.addProperty("yaw", yaw);
    data.addProperty("pitch", pitch);
    return current.send("movement", "setLook", data);
  }

  /**
   * Returns the live location from the movement subscription, or {@code null}
   * when there is no fresh value (callers fall back to the endpoint).
   */
  public Position getLiveLocation(Bot bot) {
    if (!isConnectedFor(bot)) return null;
    return liveState.getFreshLocation();
  }

  /**
   * Provides the current health value, or -1 if the bot is not connected.
   *
   * @param bot The bot for which to check health
   * @return The health value as a double, or -1 if the bot is not connected
   */
  public double getLiveHealth(Bot bot) {
    return isConnectedFor(bot) ? liveState.getFreshHealth() : -1;
  }

  /**
   * Provides the current food level, or -1 if the bot is not connected.
   *
   * @param bot The bot for which to check food level
   * @return The food level as a double, or -1 if the bot is not connected
   */
  public double getLiveFood(Bot bot) {
    return isConnectedFor(bot) ? liveState.getFreshFood() : -1;
  }

  /**
   * Provides the current experience level, or -1 if the bot is not connected.
   *
   * @param bot The bot for which to check experience level
   * @return The experience level as an integer, or -1 if the bot is not connected
   */
  public int getLiveLevel(Bot bot) {
    return isConnectedFor(bot) ? liveState.getFreshLevel() : -1;
  }

  // ---- connection handling -------------------------------------------------

  private boolean isConnected() {
    CbxClient current = client;
    return current != null && current.isOpen();
  }

  private boolean cbxAllowed() {
    GGBot addon = GGBot.getInstance();
    return addon != null
        && Boolean.TRUE.equals(addon.configuration().enabled().get())
        && addon.getVersioningHandler().isFeatureEnabled(F_CBX);
  }

  private boolean eagerAllowed() {
    return cbxAllowed()
        && GGBot.getInstance().getVersioningHandler().isFeatureEnabled(F_CBX_ON_LOGIN);
  }

  private void requestEagerConnect() {
    if (!eagerAllowed()) return;
    startAttempt(0);
  }

  /** Whether connecting makes sense right now. */
  private boolean prerequisitesMet() {
    GGBot addon = GGBot.getInstance();
    if (addon == null || !cbxAllowed()) return false;
    if (!GGBot.isAuthenticated()) return false;
    if (!addon.labyAPI().serverController().isConnected()) return false;
    Bot bot = BotRequests.getBotFromCache(addon);
    // An offline bot has no CBX to connect to; the status refresh hook
    // triggers a new attempt once it comes online.
    return bot != null && Boolean.TRUE.equals(bot.getOnline());
  }

  /**
   * Starts a fresh connect episode: marks the connection as wanted, resets the
   * backoff and allows one failure log again. Used by lifecycle signals (join,
   * bot start, offline-to-online, control start) that legitimately warrant a
   * prompt attempt.
   */
  private synchronized void startAttempt(long delayMs) {
    desired = true;
    reconnectAttempt = 0;
    loggedConnectFailure = false;
    scheduleAttempt(delayMs);
  }

  /**
   * Schedules exactly one pending connect attempt, cancelling any previously
   * scheduled one first. This is the single choke point that guarantees there
   * is never more than one reconnect loop in flight (the earlier duplicate
   * scheduling caused the rapid "connect failed" retry storm in the log).
   */
  private synchronized void scheduleAttempt(long delayMs) {
    if (!desired) return;
    if (pendingConnect != null) {
      pendingConnect.cancel(false);
    }
    pendingConnect = executor.schedule(this::connectNow, delayMs, TimeUnit.MILLISECONDS);
  }

  /** Whether a connect attempt is already scheduled and not yet run. */
  private synchronized boolean hasPendingAttempt() {
    return pendingConnect != null && !pendingConnect.isDone();
  }

  private void connectNow() {
    if (!desired || isConnected() || !prerequisitesMet()) return;

    GGBot addon = GGBot.getInstance();
    Bot bot = BotRequests.getBotFromCache(addon);
    if (bot == null || bot.getToken() == null) return;

    try {
      String connectUri = fetchConnectUri(addon, bot);
      if (connectUri == null) {
        onConnectFailed("no session");
        return;
      }

      CbxClient newClient = new CbxClient(this::handlePacket, this::onDisconnected);
      newClient.connect(connectUri).get(15, TimeUnit.SECONDS);

      // The handshake takes a while; a teardown in the meantime means nobody
      // wants this connection anymore, so it must not be published (it would
      // stay open with no lifecycle event ever closing it again).
      if (!desired || !prerequisitesMet()) {
        newClient.close();
        return;
      }
      synchronized (this) {
        if (!desired) {
          newClient.close();
          return;
        }
        this.client = newClient;
        this.connectedBotToken = bot.getToken();
        this.reconnectAttempt = 0;
        this.loggedConnectFailure = false;
      }
      addon.logger().info("[CBX] Connected for bot " + bot.getLinkName());

      // hudElements pushes start once a client sends its first packet on the
      // endpoint; getStatus doubles as the registration and initial snapshot.
      newClient.send("hudElements", "getStatus", null);
      movementSubscribed = false;
      syncMovementSubscription();
    } catch (Exception e) {
      onConnectFailed(String.valueOf(e.getMessage()));
    }
  }

  private void onConnectFailed(String reason) {
    GGBot addon = GGBot.getInstance();
    // One line per episode, not per retry: further retries fail silently and
    // grow the backoff, and the HTTP endpoints keep everything working.
    if (addon != null && !loggedConnectFailure) {
      loggedConnectFailure = true;
      addon.logger().info("[CBX] Connect failed (" + reason
          + "); staying on HTTP endpoints and retrying quietly in the background");
    }
    scheduleReconnect();
  }

  /** Unexpected connection loss reported by the client. */
  private void onDisconnected(String reason) {
    GGBot addon = GGBot.getInstance();
    if (addon != null) {
      addon.logger().info("[CBX] Disconnected: " + reason);
    }
    this.client = null;
    this.connectedBotToken = null;
    this.movementSubscribed = false;
    // A real disconnect starts a new episode, so allow one failure log again.
    this.loggedConnectFailure = false;
    scheduleReconnect();
  }

  /**
   * Schedules the next reconnect with an incrementally growing delay (capped at
   * {@link #BACKOFF_MAX_MS}), replacing any pending attempt so only one loop
   * ever runs. Safe to be slow: every consumer has the HTTP endpoint fallback
   * while disconnected.
   */
  private synchronized void scheduleReconnect() {
    if (!desired) return;
    reconnectAttempt++;
    long delay = (long) (BACKOFF_INITIAL_MS * Math.pow(BACKOFF_FACTOR, reconnectAttempt - 1));
    delay = Math.min(delay, BACKOFF_MAX_MS);
    scheduleAttempt(delay);
  }

  /** Deliberate teardown; no reconnect until a new lifecycle event wants one. */
  private synchronized void stopConnection(String reason) {
    desired = false;
    reconnectAttempt = 0;
    loggedConnectFailure = false;
    if (pendingConnect != null) {
      pendingConnect.cancel(false);
      pendingConnect = null;
    }
    CbxClient current = client;
    client = null;
    connectedBotToken = null;
    movementSubscribed = false;
    if (current != null) {
      current.close();
      GGBot addon = GGBot.getInstance();
      if (addon != null) {
        addon.logger().info("[CBX] Connection closed (" + reason + ")");
      }
    }
  }

  /**
   * Controls movement subscription based on active mode or widget usage.
   */
  private void syncMovementSubscription() {
    CbxClient current = client;
    if (current == null || !current.isOpen()) return;
    boolean wanted = controlActive || isWidgetEnabled(PositionWidget.WIDGET_ID);
    if (wanted == movementSubscribed) return;
    if (current.send("movement", wanted ? "subscribe" : "unsubscribe", null)) {
      movementSubscribed = wanted;
    }
  }

  /**
   * Requests a fresh CBX session via the SDK and builds the WebSocket connect
   * URI from it. Sessions are short-lived and single-purpose, so this runs
   * before every attempt. Returns {@code null} when no session could be
   * obtained (feature disabled, bot offline, network error).
   */
  private String fetchConnectUri(GGBot addon, Bot bot) {
    try {
      de.ggbot.sdk.model.CbxSession session = BotRequests.createCbxSession(addon, bot);
      return buildConnectUri(session);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Builds the full WebSocket URI (including the {@code ?id=} auth parameter)
   * from a CBX session, tolerating a missing {@code url} by falling back to
   * {@code ws://host:port}.
   */
  static String buildConnectUri(de.ggbot.sdk.model.CbxSession session) {
    if (session == null || session.getToken() == null || session.getToken().isEmpty()) {
      return null;
    }
    String url = session.getUrl();
    if (url == null || url.isEmpty()) {
      if (session.getHost() == null || session.getHost().isEmpty()) return null;
      int port = session.getPort() != null ? session.getPort() : 8081;
      url = "ws://" + session.getHost() + ":" + port;
    }
    return url + (url.contains("?") ? "&" : "/?") + "id=" + session.getToken();
  }

  // ---- incoming packets ----------------------------------------------------

  /**
   * Dispatches received packets. Everything not recognized - endpoints,
   * actions, update types, fields - is ignored on purpose: the CBX protocol
   * evolves without the addon always being updated alongside it.
   */
  private void handlePacket(CbxPacket packet) {
    String endpoint = packet.getEndpoint();
    if (endpoint == null) return;

    switch (endpoint) {
      case "hudElements" -> handleHudPacket(packet);
      case "movement" -> handleMovementPacket(packet);
      default -> { /* unknown endpoint: ignore */ }
    }
  }

  private void handleHudPacket(CbxPacket packet) {
    JsonObject data = packet.getDataObject();
    if (data == null) return;

    JsonObject player = hudSection(packet, data, "player");
    if (player != null) {
      liveState.applyPlayer(player);
      updatePlayerWidgets();
    }

    JsonObject tablist = hudSection(packet, data, "tablist");
    if (tablist != null) {
      liveState.applyTablist(tablist);
      int count = liveState.getFreshPlayerCount();
      if (count >= 0 && isWidgetEnabled(PlayersWidget.WIDGET_ID)) {
        PlayersWidget.update(count);
      }
    }
  }

  /**
   * Extracts HUD data sections based on update type and section name.
   *
   * @param packet The CBX packet containing HUD data
   * @param data The JSON data object from the packet
   * @param name The name of the HUD section to extract
   * @return The extracted HUD section as a JSON object, or null if not found
   */
  private static JsonObject hudSection(CbxPacket packet, JsonObject data, String name) {
    if ("partialUpdate".equals(packet.getAction())) {
      return name.equals(packet.getUpdateType()) ? data : null;
    }
    return data.has(name) && data.get(name).isJsonObject() ? data.getAsJsonObject(name) : null;
  }

  private void handleMovementPacket(CbxPacket packet) {
    JsonObject data = packet.getDataObject();
    if (data == null) return;

    // Pushed updates carry the snapshot directly; the subscribe response
    // wraps it in "snapshot".
    JsonObject snapshot = data.has("snapshot") && data.get("snapshot").isJsonObject()
        ? data.getAsJsonObject("snapshot") : data;
    if (snapshot.has("position")) {
      liveState.applyMovement(snapshot);
      updatePositionWidget();
    }
  }

  /**
   * Updates the corresponding widgets immediately whenever live player values
   * change, without waiting for the next stats timer cycle. The timer remains
   * active as a fallback and continues handling data not provided by CBX.
   */
  private void updatePlayerWidgets() {
    double health = liveState.getFreshHealth();
    if (health >= 0 && isWidgetEnabled(HealthWidget.WIDGET_ID)) {
      HealthWidget.update(Component.text(health + " ")
          .append(Component.translatable("ggbot.widget.health.unit")));
    }
    double food = liveState.getFreshFood();
    if (food >= 0 && isWidgetEnabled(FoodWidget.WIDGET_ID)) {
      FoodWidget.update(FoodWidget.format(food));
    }
    int level = liveState.getFreshLevel();
    if (level >= 0 && isWidgetEnabled(LevelWidget.WIDGET_ID)) {
      LevelWidget.update(level);
    }
  }

  private void updatePositionWidget() {
    long now = System.currentTimeMillis();
    if (now - lastPositionUpdate < POSITION_WIDGET_INTERVAL_MS) return;
    Position position = liveState.getFreshLocation();
    if (position == null || !isWidgetEnabled(PositionWidget.WIDGET_ID)) return;
    lastPositionUpdate = now;
    PositionWidget.update(PositionWidget.format(position));
  }

  private boolean isWidgetEnabled(String id) {
    GGBot addon = GGBot.getInstance();
    if (addon == null) return false;
    var widget = addon.labyAPI().hudWidgetRegistry().getById(id);
    return widget != null && widget.isEnabled();
  }
}
