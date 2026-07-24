package de.ggbot.core.cbx;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * Minimal CBX (Client Bot Exchange) WebSocket client.
 *
 * <p>Implements the basics of the CBX protocol: JSON packets of the shape
 * {@code {endpoint, action, data, requestId}} over a WebSocket authenticated
 * via the {@code ?id=} query parameter. Anything the client does not
 * recognize - unknown endpoints, actions, fields or malformed frames - is
 * ignored silently, because the CBX evolves independently and the addon is
 * not always updated alongside it.
 *
 * <p>The transport-level ping the server sends every 30s is answered
 * automatically by {@link WebSocket}. Connection loss is reported through the
 * {@code onDisconnected} callback; reconnecting is the owner's job (see
 * {@link CbxManager}).
 */
public final class CbxClient implements WebSocket.Listener {

  /** Connect timeout for the WebSocket handshake. */
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

  private final Consumer<CbxPacket> packetHandler;
  private final Consumer<String> disconnectHandler;
  private final StringBuilder partialText = new StringBuilder();

  private volatile WebSocket webSocket;
  private volatile boolean closed = false;

  /**
   * Creates a client.
   *
   * @param packetHandler     invoked for every parsed packet (on a transport
   *                          thread; keep the work small)
   * @param disconnectHandler invoked once when the connection ends for any
   *                          reason, with a short description
   */
  public CbxClient(Consumer<CbxPacket> packetHandler, Consumer<String> disconnectHandler) {
    this.packetHandler = packetHandler;
    this.disconnectHandler = disconnectHandler;
  }

  /**
   * Opens the WebSocket connection.
   *
   * @param uri the full CBX URI including the {@code ?id=} auth parameter
   * @return a future completing when the handshake finished
   */
  public CompletableFuture<Void> connect(String uri) {
    return HttpClient.newHttpClient()
        .newWebSocketBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .buildAsync(URI.create(uri), this)
        .thenAccept(ws -> this.webSocket = ws);
  }

  /**
   * Sends a packet, fire-and-forget.
   *
   * @param endpoint the CBX endpoint name
   * @param action   the action name
   * @param data     the data object, may be {@code null}
   * @return {@code true} when the packet was handed to the transport
   */
  public boolean send(String endpoint, String action, JsonObject data) {
    WebSocket ws = this.webSocket;
    if (ws == null || closed) return false;

    JsonObject packet = new JsonObject();
    packet.addProperty("endpoint", endpoint);
    packet.addProperty("action", action);
    if (data != null) packet.add("data", data);
    try {
      ws.sendText(packet.toString(), true);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /** Returns whether the connection is (still) open. */
  public boolean isOpen() {
    WebSocket ws = this.webSocket;
    return ws != null && !closed && !ws.isOutputClosed();
  }

  /** Closes the connection; the disconnect handler will not be re-invoked. */
  public void close() {
    closed = true;
    WebSocket ws = this.webSocket;
    if (ws != null) {
      try {
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
      } catch (Exception e) {
        ws.abort();
      }
    }
  }

  @Override
  public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
    // Frames may arrive fragmented; accumulate until the final part.
    partialText.append(data);
    if (last) {
      String message = partialText.toString();
      partialText.setLength(0);
      handleMessage(message);
    }
    ws.request(1);
    return null;
  }

  @Override
  public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
    reportDisconnect("closed (" + statusCode + (reason != null && !reason.isEmpty()
        ? ", " + reason : "") + ")");
    return null;
  }

  @Override
  public void onError(WebSocket ws, Throwable error) {
    reportDisconnect("error: " + error.getMessage());
  }

  private void handleMessage(String message) {
    JsonObject json;
    try {
      JsonElement parsed = JsonParser.parseString(message);
      if (!parsed.isJsonObject()) return; // not a packet: ignore
      json = parsed.getAsJsonObject();
    } catch (Exception e) {
      return; // malformed frame: ignore
    }

    try {
      packetHandler.accept(CbxPacket.fromJson(json));
    } catch (Exception e) {
      // A handler bug must never kill the transport thread.
    }
  }

  private void reportDisconnect(String reason) {
    if (closed) return; // deliberate close: not an unexpected disconnect
    closed = true;
    try {
      disconnectHandler.accept(reason);
    } catch (Exception e) {
      // Nothing sensible left to do.
    }
  }
}
