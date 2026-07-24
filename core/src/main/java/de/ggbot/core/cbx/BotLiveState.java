package de.ggbot.core.cbx;

import com.google.gson.JsonObject;
import de.ggbot.sdk.model.Position;

/**
 * Latest live data received for a bot over CBX. Updated from the movement
 * subscription and the hudElements pushes; read by the request layer as a
 * fast, endpoint-free data source with the HTTP endpoints as fallback.
 */
public final class BotLiveState {

  /** How long a live value is considered current before falling back. */
  private static final long FRESHNESS_MS = 5_000L;

  private volatile double x, y, z;
  private volatile float yaw, pitch;
  private volatile long locationUpdatedAt;

  private volatile double health = -1;
  private volatile long healthUpdatedAt;

  /** Applies a movement snapshot ({@code movement}/{@code update} packet). */
  void applyMovement(JsonObject data) {
    JsonObject position = data.has("position") && data.get("position").isJsonObject()
        ? data.getAsJsonObject("position") : null;
    if (position != null) {
      this.x = doubleOr(position, "x", this.x);
      this.y = doubleOr(position, "y", this.y);
      this.z = doubleOr(position, "z", this.z);
    }
    this.yaw = (float) doubleOr(data, "yaw", this.yaw);
    this.pitch = (float) doubleOr(data, "pitch", this.pitch);
    this.locationUpdatedAt = System.currentTimeMillis();
  }

  /** Applies a hudElements player block (health and so on). */
  void applyPlayer(JsonObject player) {
    if (player.has("health") && player.get("health").isJsonPrimitive()) {
      this.health = player.get("health").getAsDouble();
      this.healthUpdatedAt = System.currentTimeMillis();
    }
  }

  private static double doubleOr(JsonObject json, String key, double fallback) {
    return json.has(key) && json.get(key).isJsonPrimitive()
        ? json.get(key).getAsDouble() : fallback;
  }

  /** Returns the live location, or {@code null} when none is fresh. */
  public Position getFreshLocation() {
    if (System.currentTimeMillis() - locationUpdatedAt > FRESHNESS_MS) return null;
    return new Position().x(x).y(y).z(z);
  }

  /** Returns the live health, or {@code -1} when none is fresh. */
  public double getFreshHealth() {
    if (System.currentTimeMillis() - healthUpdatedAt > FRESHNESS_MS) return -1;
    return health;
  }

  /** Returns the live yaw (radians, bot convention). */
  public float getYaw() { return yaw; }

  /** Returns the live pitch (radians, bot convention). */
  public float getPitch() { return pitch; }
}
