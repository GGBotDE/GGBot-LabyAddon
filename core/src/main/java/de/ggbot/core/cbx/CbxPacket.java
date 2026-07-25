package de.ggbot.core.cbx;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A single parsed CBX (Client Bot Exchange) packet.
 *
 * <p>Wire format (JSON):
 * <pre>{@code
 * { "endpoint": "...", "action": "...", "status": "ok"|"error",
 *   "data": {...}, "requestId": "...", "updateType": "..." }
 * }</pre>
 *
 * <p>All fields are optional on the wire. Unknown fields are ignored on
 * purpose: the CBX protocol evolves independently of the addon, and the
 * addon is not always updated when the CBX is (forward compatibility).
 */
public final class CbxPacket {

  private final String endpoint;
  private final String action;
  private final String status;
  private final String requestId;
  private final String updateType;
  private final JsonElement data;

  private CbxPacket(String endpoint, String action, String status, String requestId,
      String updateType, JsonElement data) {
    this.endpoint = endpoint;
    this.action = action;
    this.status = status;
    this.requestId = requestId;
    this.updateType = updateType;
    this.data = data;
  }

  /**
   * Parses a packet from a received JSON object. Missing fields become
   * {@code null}; extra fields are ignored.
   *
   * @param json the received JSON object
   * @return the parsed packet
   */
  public static CbxPacket fromJson(JsonObject json) {
    return new CbxPacket(
        stringOrNull(json, "endpoint"),
        stringOrNull(json, "action"),
        stringOrNull(json, "status"),
        stringOrNull(json, "requestId"),
        stringOrNull(json, "updateType"),
        json.get("data"));
  }

  private static String stringOrNull(JsonObject json, String key) {
    JsonElement element = json.get(key);
    return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
  }

  /** Returns the endpoint name, or {@code null}. */
  public String getEndpoint() { return endpoint; }

  /** Returns the action name, or {@code null}. */
  public String getAction() { return action; }

  /** Returns the status ({@code "ok"} or {@code "error"}), or {@code null}. */
  public String getStatus() { return status; }

  /** Returns the request correlation id, or {@code null} for pushed packets. */
  public String getRequestId() { return requestId; }

  /** Returns the partial-update type (hudElements pushes), or {@code null}. */
  public String getUpdateType() { return updateType; }

  /** Returns the raw data element, or {@code null}. */
  public JsonElement getData() { return data; }

  /** Returns the data as an object, or {@code null} when absent or not an object. */
  public JsonObject getDataObject() {
    return data != null && data.isJsonObject() ? data.getAsJsonObject() : null;
  }

  /** Returns whether the packet reports success. */
  public boolean isOk() {
    return "ok".equals(status);
  }

  @Override
  public String toString() {
    return "CbxPacket{" + endpoint + "/" + action + " status=" + status + '}';
  }
}
