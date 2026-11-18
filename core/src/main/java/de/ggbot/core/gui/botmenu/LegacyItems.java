package de.ggbot.core.gui.botmenu;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import de.ggbot.core.GGBot;
import net.labymod.api.Laby;
import net.labymod.api.client.resources.ResourceLocation;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * Maps modern Minecraft item identifiers to their legacy equivalents used by
 * clients running versions older than 1.13. For example, {@code orange_terracotta}
 * is converted to {@code stained_hardened_clay} with metadata value 1.
 * Since the API provides modern item identifiers, this conversion is required
 * for older clients to recognize the items and apply server resource packs.
 * The mapping table is generated from the {@code legacy.json} file provided by
 * PrismarineJS/minecraft-data.
 */
public final class LegacyItems {

  /**
   * Defines the protocol version for item ID flattening in version 1.13.
   */
  private static final int FLATTENING_PROTOCOL = 393;
  private static final String NAMESPACE = "minecraft";
  private static final String TABLE = "legacy_items.json";

  private static Map<String, String> table;

  private LegacyItems() {}

  /**
   * Describes an item from the pre-1.13 era with its path and data.
   */
  public record Item(String path, int data) {}

  /**
   * Locates a legacy item by namespace and modern path, returning null if not found.
   *
   * @param namespace the namespace to check against
   * @param path the modern path of the item
   * @return the Item record if found, null otherwise
   */
  public static Item find(String namespace, String path) {
    if (!NAMESPACE.equals(namespace)
        || Laby.labyAPI().minecraft().getProtocolVersion() >= FLATTENING_PROTOCOL) {
      return null;
    }
    return parse(table().get(path));
  }

  /**
   * Divides a table entry string into its path and data components.
   *
   * @param value the table entry string to split
   * @return an Item record containing the path and data
   */
  public static Item parse(String value) {
    if (value == null) return null;
    
    int separator = value.indexOf(':');
    if (separator < 0) return new Item(value, 0);

    return new Item(value.substring(0, separator), Integer.parseInt(value.substring(separator + 1)));
  }

  private static synchronized Map<String, String> table() {
    if (table == null) {
      table = load();
    }
    return table;
  }

  private static Map<String, String> load() {
    ResourceLocation location = ResourceLocation.create("ggbot", TABLE);
    try (Reader reader = new InputStreamReader(location.openStream(), StandardCharsets.UTF_8)) {
      return new Gson().fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
    } catch (IOException | JsonParseException e) {
      GGBot.getInstance().logger().warn("Legacy item table failed: {}", String.valueOf(e.getMessage()));
      return Collections.emptyMap();
    }
  }
}
