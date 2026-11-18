package de.ggbot.core.cbx;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.ggbot.core.GGBot;
import de.ggbot.core.api.ApiParsers;
import de.ggbot.core.api.BotRequests;
import de.ggbot.sdk.core.ApiException;
import de.ggbot.sdk.model.AfkInspect;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Manages the account's AFK CBX session, providing access to all AFK bots
 * through a single wrapper. The session remains active only while the
 * AFK fleets GUI is open. When no session is available, consumers
 * automatically fall back to the HTTP endpoints.
 */
public final class AfkCbxManager {

  private static final String F_CBX = "de.ggbot.addon.cbx";

  private static final long BACKOFF_INITIAL_MS = 3_000L;
  private static final double BACKOFF_FACTOR = 1.6;
  private static final long BACKOFF_MAX_MS = 300_000L;

  /**
   * Specifies the delay before attempting to connect to CBX after bots have started.
   */
  private static final long BOT_START_CONNECT_DELAY_MS = 5_000L;

  /**
   * Defines the timeout duration for inspect operations in the CBX service.
   */
  private static final long INSPECT_TIMEOUT_MS = 4_000L;

  private static final int STATUS_CONFLICT = 409;

  private static final AfkCbxManager INSTANCE = new AfkCbxManager();

  public static AfkCbxManager get() {
    return INSTANCE;
  }

  private final ScheduledExecutorService executor =
      Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ggbot-afk-cbx");
        t.setDaemon(true);
        return t;
      });

  private final Gson gson = new Gson();
  private final Map<String, Consumer<AfkInspect>> pendingInspects = new ConcurrentHashMap<>();

  private volatile CbxClient client;
  private volatile boolean desired = false;
  private int reconnectAttempt = 0;
  private ScheduledFuture<?> pendingConnect;

  private AfkCbxManager() {
  }

  /**
   * Activates the CBX session if it is not already running.
   */
  public synchronized void open() {
    if (desired) return;
    desired = true;
    reconnectAttempt = 0;
    scheduleAttempt(0);
  }

  /**
   * Closes the CBX session and cancels any pending operations.
   */
  public synchronized void close() {
    desired = false;
    if (pendingConnect != null) {
      pendingConnect.cancel(false);
      pendingConnect = null;
    }
    CbxClient current = client;
    client = null;
    if (current != null) current.close();
    pendingInspects.values().forEach(callback -> callback.accept(null));
    pendingInspects.clear();
  }

  /**
   * Invoked after bots start; signals no active session if no bots are running.
   */
  public synchronized void onBotsStarted() {
    if (desired && !isConnected()) {
      reconnectAttempt = 0;
      scheduleAttempt(BOT_START_CONNECT_DELAY_MS);
    }
  }

  /**
   * Attempts to connect to CBX immediately if bots are active and no session is present.
   */
  public synchronized void ensureConnected() {
    if (desired && !isConnected() && (pendingConnect == null || pendingConnect.isDone())) {
      reconnectAttempt = 0;
      scheduleAttempt(0);
    }
  }

  /**
   * Verifies if the CBX session is currently active and operational.
   */
  public boolean isConnected() {
    CbxClient current = client;
    return current != null && current.isOpen();
  }

  /**
   * Attempts to send a chat message or command through the CBX session.
   *
   * @param tokens The list of bot tokens to target
   * @param text The message or command content to send
   * @return true if the message was successfully sent, false otherwise
   */
  public boolean tryChat(List<String> tokens, String text) {
    CbxClient current = client;
    if (current == null || !current.isOpen() || tokens.isEmpty()) return false;
    JsonObject data = botsData(tokens);
    data.addProperty("text", text);
    return current.send("afk", "chat", data);
  }

  /**
   * Attempts to send a walk command through the CBX session.
   *
   * @param tokens The list of bot tokens to target
   * @param forward The amount to move forward
   * @param strafe The amount to move sideways
   * @param turn The amount to rotate
   * @param jump Whether to perform a jump action
   * @return true if the command was successfully sent, false otherwise
   */
  public boolean tryWalk(List<String> tokens, double forward, double strafe, double turn,
      boolean jump) {
    CbxClient current = client;
    if (current == null || !current.isOpen() || tokens.isEmpty()) return false;
    return current.send("afk", "walk", walkData(tokens, forward, strafe, turn, jump));
  }

  /**
   * Initiates an inspection of a bot via CBX using a unique request ID for tracking.
   *
   * @param token The bot token to inspect
   * @param callback The callback to handle the inspection result
   * @return true if the inspection request was successfully sent, false otherwise
   */
  public boolean tryInspect(String token, Consumer<AfkInspect> callback) {
    CbxClient current = client;
    if (current == null || !current.isOpen()) return false;
    String requestId = UUID.randomUUID().toString();
    JsonObject data = new JsonObject();
    data.addProperty("bot", token);
    pendingInspects.put(requestId, callback);
    if (!current.send("afk", "inspect", data, requestId)) {
      pendingInspects.remove(requestId);
      return false;
    }
    executor.schedule(() -> {
      Consumer<AfkInspect> pending = pendingInspects.remove(requestId);
      if (pending != null) pending.accept(null);
    }, INSPECT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    return true;
  }

  private static JsonObject botsData(List<String> tokens) {
    JsonObject data = new JsonObject();
    JsonArray bots = new JsonArray();
    tokens.forEach(bots::add);
    data.add("bots", bots);
    return data;
  }

  private static JsonObject walkData(List<String> tokens, double forward, double strafe,
      double turn, boolean jump) {
    JsonObject data = botsData(tokens);
    data.addProperty("forward", forward);
    data.addProperty("strafe", strafe);
    data.addProperty("turn", turn);
    data.addProperty("jump", jump);
    return data;
  }

  private boolean connectAllowed() {
    GGBot addon = GGBot.getInstance();
    return addon != null && desired && GGBot.isAuthenticated()
        && Boolean.TRUE.equals(addon.configuration().enabled().get())
        && addon.getVersioningHandler().isFeatureEnabled(F_CBX);
  }

  private synchronized void scheduleAttempt(long delayMs) {
    if (!desired) return;
    if (pendingConnect != null) {
      pendingConnect.cancel(false);
    }
    pendingConnect = executor.schedule(this::connectNow, delayMs, TimeUnit.MILLISECONDS);
  }

  private synchronized void scheduleReconnect() {
    if (!desired) return;
    reconnectAttempt++;
    long delay = (long) (BACKOFF_INITIAL_MS * Math.pow(BACKOFF_FACTOR, reconnectAttempt - 1));
    scheduleAttempt(Math.min(delay, BACKOFF_MAX_MS));
  }

  private void connectNow() {
    if (isConnected() || !connectAllowed()) return;
    try {
      String body = BotRequests.createAfkCbxSession(GGBot.getInstance());
      if (body == null) return;

      String connectUri = CbxManager.buildConnectUri(ApiParsers.parseSession(body));
      if (connectUri == null) {
        log("unusable session, fields: " + ApiParsers.sessionFields(body));
        scheduleReconnect();
        return;
      }
      
      CbxClient newClient = new CbxClient(this::handlePacket, this::onDisconnected);
      newClient.connect(connectUri).get(15, TimeUnit.SECONDS);
      synchronized (this) {
        if (!desired) {
          newClient.close();
          return;
        }
        client = newClient;
        reconnectAttempt = 0;
      }
      log("connected");
    } catch (ApiException e) {
      log("session failed with " + e.getCode() + ": " + e.getResponseBody());
      // Retries bot startup on 409 conflict errors.
      if (e.getCode() != STATUS_CONFLICT) scheduleReconnect();
    } catch (Exception e) {
      log("connect failed: " + e.getMessage());
      scheduleReconnect();
    }
  }

  private void log(String message) {
    GGBot addon = GGBot.getInstance();
    if (addon != null) addon.logger().info("[AFK CBX] " + message);
  }

  private void onDisconnected(String reason) {
    log("disconnected: " + reason);
    client = null;
    scheduleReconnect();
  }

  private void handlePacket(CbxPacket packet) {
    if ("afk".equals(packet.getEndpoint()) && "inspect".equals(packet.getAction())) {
      handleInspect(packet);
    }
  }

  private void handleInspect(CbxPacket packet) {
    if (packet.getRequestId() == null) return;
    Consumer<AfkInspect> callback = pendingInspects.remove(packet.getRequestId());
    if (callback == null) return;
    JsonObject data = packet.getDataObject();
    callback.accept(data != null && packet.isOk() ? gson.fromJson(data, AfkInspect.class) : null);
  }
}
