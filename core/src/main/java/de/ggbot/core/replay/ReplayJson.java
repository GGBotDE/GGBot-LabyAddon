package de.ggbot.core.replay;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Reads values of the replay JSON the api and the replay storage give, which may be missing. */
public final class ReplayJson {

  private ReplayJson() {}

  /**
   * @param object the object, may be {@code null}
   * @param key    the key
   * @return the value as text, {@code null} when absent
   */
  public static String string(JsonObject object, String key) {
    JsonElement value = object == null ? null : object.get(key);
    return value == null || value.isJsonNull() ? null : value.getAsString();
  }

  /**
   * @param object the object, may be {@code null}
   * @param key    the key
   * @return the value as an integer, booleans as 0 and 1, 0 when absent
   */
  public static long number(JsonObject object, String key) {
    JsonElement value = object == null ? null : object.get(key);
    if (value == null || !value.isJsonPrimitive()) return 0;

    if (value.getAsJsonPrimitive().isBoolean()) return value.getAsBoolean() ? 1 : 0;

    try {
      return (long) Double.parseDouble(value.getAsString());
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}
