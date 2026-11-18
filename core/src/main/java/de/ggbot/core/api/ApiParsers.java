package de.ggbot.core.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.ggbot.core.gui.botmenu.ItemIcons;
import de.ggbot.sdk.model.AfkBot;
import de.ggbot.sdk.model.AfkFleet;
import de.ggbot.sdk.model.CbxSession;
import de.ggbot.sdk.model.MCControlDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Handles API responses that do not match the structure of the generated SDK
 * models. Fleet entries provide bot tokens directly, while module responses
 * represent {@code commands} as an object.
 * Only the fields required by the addon are extracted, keeping the parser
 * independent of unrelated changes to the response structure.
 */
public final class ApiParsers {

  private static final String MODULE_PREFIX = "module-";

  private ApiParsers() {}

  /**
   * Combines the {@code itemKey}, {@code fallbackKey} and {@code itemType} of a shop item
   * into the item type for {@link ItemIcons}, as described in {@link ItemIcons#withFallback}.
   * The item key identifies the specific item, which may originate from a mod,
   * while the fallback key references the corresponding vanilla item.
   *
   * @param itemKey     the item key, may be {@code null}
   * @param fallbackKey the fallback key, may be {@code null}
   * @param itemType    the item type, may be {@code null}
   * @return the item type for {@link ItemIcons#forItem}
   */
  public static String itemType(String itemKey, String fallbackKey, String itemType) {
    String key = itemId(itemKey);
    if (key == null) return itemType;
    return ItemIcons.withFallback(key, itemId(fallbackKey), itemId(itemType));
  }

  private static String itemId(String id) {
    return id == null ? null : id.replaceFirst("^minecraft:", "");
  }

  /**
   * Parses a CBX session while handling optional wrappers and alternative token field names.
   *
   * @param json The JSON string containing session information
   * @return A CbxSession object populated with extracted session details
   */
  public static CbxSession parseSession(String json) {
    JsonObject object = sessionObject(json);
    CbxSession session = new CbxSession();
    session.setToken(first(object, "token", "id", "sessionId", "accessToken", "key"));
    session.setUrl(first(object, "url", "wsUrl", "uri"));
    session.setHost(string(object, "host"));
    session.setPort(integer(object, "port"));
    return session;
  }

  /**
   * Identifies the fields available in a session response JSON, accounting for nested structures and wrapper elements.
   *
   * @param json The JSON string representing the session response
   * @return A Set containing the names of all fields present in the session data
   */
  public static Set<String> sessionFields(String json) {
    return sessionObject(json).keySet();
  }

  private static JsonObject sessionObject(String json) {
    JsonElement root = JsonParser.parseString(json);
    if (!root.isJsonObject()) return new JsonObject();

    JsonObject object = root.getAsJsonObject();
    for (String wrapper : new String[]{"data", "session"}) {
      if (object.has(wrapper) && object.get(wrapper).isJsonObject()) {
        return object.getAsJsonObject(wrapper);
      }
    }
    return object;
  }

  private static String first(JsonObject object, String... keys) {
    for (String key : keys) {
      String value = string(object, key);
      if (value != null && !value.isEmpty()) return value;
    }
    return null;
  }

  /**
   * Parses fleet information from a JSON response, extracting essential details like ID, name, and server.
   *
   * @param json The JSON string containing fleet data
   * @return A list of AfkFleet objects populated with parsed information
   */
  public static List<AfkFleet> parseFleets(String json) {
    List<AfkFleet> fleets = new ArrayList<>();
    for (JsonElement element : array(json)) {
      if (!element.isJsonObject()) continue;

      JsonObject object = element.getAsJsonObject();
      Integer id = integer(object, "id");
      if (id == null) continue;

      AfkFleet fleet = new AfkFleet();
      fleet.setId(id);
      fleet.setName(string(object, "name"));
      fleet.setServer(string(object, "server"));
      fleets.add(fleet);
    }
    return fleets;
  }

  /**
   * Parses AFK bot data from a JSON response, extracting key attributes like token, fleet, and status.
   *
   * @param json The JSON string containing bot information
   * @return A list of AfkBot objects populated with parsed details
   */
  public static List<AfkBot> parseBots(String json) {
    List<AfkBot> bots = new ArrayList<>();
    for (JsonElement element : array(json)) {
      if (!element.isJsonObject()) continue;

      JsonObject object = element.getAsJsonObject();
      String token = string(object, "token");
      if (token == null) continue;

      AfkBot bot = new AfkBot();
      bot.setToken(token);
      bot.setFleet(integer(object, "fleet"));
      bot.setLinkName(string(object, "linkName"));
      bot.setStatus(status(string(object, "status")));
      bots.add(bot);
    }
    return bots;
  }

  /**
   * Collects module names from JSON data, ensuring both enabled and available flags are set, and normalizes to lowercase.
   *
   * @param json The JSON string containing module information
   * @return A Set of module names in lowercase that are both enabled and available
   */
  public static Set<String> parseModules(String json) {
    Set<String> modules = new HashSet<>();
    for (JsonElement element : array(json)) {
      if (element.isJsonObject()) {
        JsonObject object = element.getAsJsonObject();
        String name = string(object, "name");
        if (name != null && flag(object, "enabled") && flag(object, "available")) {
          modules.add(name.toLowerCase(Locale.ROOT));
        }
      } else if (element.isJsonPrimitive()) {
        modules.add(element.getAsString().toLowerCase(Locale.ROOT));
      }
    }
    return modules;
  }

  /**
   * Verifies the presence of at least one enabled module in the JSON data.
   *
   * @param json The JSON string to check for enabled modules
   * @return True if any module is enabled, false otherwise
   */
  public static boolean hasEnabledFlags(String json) {
    for (JsonElement element : array(json)) {
      if (element.isJsonObject() && element.getAsJsonObject().has("enabled")) return true;
    }
    return false;
  }

  /**
   * Identifies if the bot settings contain any keys related to modules, typically formatted as module-<name>=true.
   *
   * @param settings The map of bot settings to check
   * @return True if module-related keys are present, false otherwise
   */
  public static boolean hasModuleKeys(Map<String, String> settings) {
    if (settings == null) return false;
    for (String key : settings.keySet()) {
      if (key != null && key.startsWith(MODULE_PREFIX)) return true;
    }
    return false;
  }

  /**
   * Retrieves enabled modules from bot settings by examining keys formatted as module-<name>=true.
   *
   * @param settings The map of bot settings to parse
   * @return A Set of module names that are enabled based on the settings
   */
  public static Set<String> modulesFromSettings(Map<String, String> settings) {
    Set<String> modules = new HashSet<>();
    if (settings == null) return modules;
    
    for (Map.Entry<String, String> entry : settings.entrySet()) {
      if (entry.getKey() != null && entry.getKey().startsWith(MODULE_PREFIX)
          && isTrue(entry.getValue())) {
        modules.add(entry.getKey().substring(MODULE_PREFIX.length()).toLowerCase(Locale.ROOT));
      }
    }
    return modules;
  }

  /**
   * Assesses flag values from a JSON object, returning true if the value is explicitly set to true, 1, or missing.
   *
   * @param object The JSON object containing the flag
   * @param key The key associated with the flag
   * @return True if the flag is true, 1, or omitted; false otherwise
   */
  private static boolean flag(JsonObject object, String key) {
    JsonElement value = object.get(key);
    if (value == null || value.isJsonNull()) return true;
    return value.isJsonPrimitive() && isTrue(value.getAsString());
  }

  private static boolean isTrue(String value) {
    return "true".equalsIgnoreCase(value) || "1".equals(value);
  }

  private static AfkBot.StatusEnum status(String value) {
    if (value == null) return null;
    try {
      return AfkBot.StatusEnum.fromValue(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Selects the domain used to connect a bot via MCControl. A managed domain is
   * preferred because it always references the correct server.
   *
   * @param domains the domains belonging to the bot
   * @return the chosen domain, or {@code null} if the bot has none
   */
  public static String mcControlDomain(List<MCControlDomain> domains) {
    if (domains == null) return null;

    String chosen = null;

    for (MCControlDomain entry : domains) {
      String domain = entry.getDomain();
      if (domain == null || domain.isBlank()) continue;

      if ("system".equals(entry.getType())) return domain.trim();
      if (chosen == null) chosen = domain.trim();
    }

    return chosen;
  }

  /**
   * Handles responses that contain either a raw array or an array nested under {@code data}. 
   *
   * @param json The JSON string to process
   * @return The extracted JsonArray or an empty array if none is found
   */
  private static JsonArray array(String json) {
    JsonElement root = JsonParser.parseString(json);
    if (root.isJsonObject() && root.getAsJsonObject().has("data")) {
      root = root.getAsJsonObject().get("data");
    }
    return root.isJsonArray() ? root.getAsJsonArray() : new JsonArray();
  }

  private static String string(JsonObject object, String key) {
    JsonElement value = object.get(key);
    return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
  }

  private static Integer integer(JsonObject object, String key) {
    JsonElement value = object.get(key);
    if (value == null || !value.isJsonPrimitive()) return null;
    try {
      return value.getAsInt();
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
