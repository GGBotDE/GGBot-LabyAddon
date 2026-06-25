package de.ggbot.core.gui.shop.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.ggbot.core.GGBot;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight parser that extracts structured data (lore, enchantments, damage) from the
 * raw NBT JSON strings returned by the GGBot API. All public entry points are static and
 * side-effect-free.
 */
public class NBTParser {

  /** Immutable result of a successful {@link NBTParser#parse(String)} call. */
  public static class ParsedNBT {
    private final List<String> lore;
    private final List<Enchantment> enchantments;
    private final Integer damage;

    public ParsedNBT(List<String> lore, List<Enchantment> enchantments, Integer damage) {
      this.lore = lore;
      this.enchantments = enchantments;
      this.damage = damage;
    }

    /** @return lore lines with Minecraft § colour codes, never {@code null} */
    public List<String> getLore() {
      return lore;
    }

    /** @return enchantments applied to the item, never {@code null} */
    public List<Enchantment> getEnchantments() {
      return enchantments;
    }

    /** @return item damage value, or {@code null} if the item has no damage tag */
    public Integer getDamage() {
      return damage;
    }

    /** @return {@code true} if a damage tag was present in the NBT */
    public boolean hasDamage() {
      return damage != null;
    }
  }

  /** Represents a single enchantment tag found in an item's NBT data. */
  public static class Enchantment {
    private final String id;
    private final int level;

    public Enchantment(String id, int level) {
      this.id = id;
      this.level = level;
    }

    /** @return internal enchantment ID (e.g. {@code "sharpness"}) */
    public String getId() {
      return id;
    }

    /** @return numeric enchantment level (1-based) */
    public int getLevel() {
      return level;
    }

    /**
     * @return the enchantment level as a Roman numeral (I–V), or the raw number for level &gt; 5
     */
    public String getDisplayLevel() {
      return switch (level) {
        case 1 -> "I";
        case 2 -> "II";
        case 3 -> "III";
        case 4 -> "IV";
        case 5 -> "V";
        default -> String.valueOf(level);
      };
    }

    /**
     * @return a human-readable English name for this enchantment,
     *         falling back to the raw ID for unknown enchantments
     */
    public String getDisplayName() {
      return switch (id) {
        case "power" -> "Power";
        case "punch" -> "Punch";
        case "unbreaking" -> "Unbreaking";
        case "luck_of_the_sea" -> "Luck of the Sea";
        case "sharpness" -> "Sharpness";
        case "protection" -> "Protection";
        case "fire_aspect" -> "Fire Aspect";
        case "looting" -> "Looting";
        case "fortune" -> "Fortune";
        case "efficiency" -> "Efficiency";
        case "silk_touch" -> "Silk Touch";
        case "mending" -> "Mending";
        default -> id;
      };
    }
  }

  /**
   * Parses an NBT JSON string into a structured {@link ParsedNBT} object.
   * Returns an empty result (no lore, no enchantments, no damage) when
   * {@code nbtString} is {@code null}, blank, or unparseable.
   *
   * @param nbtString raw NBT JSON produced by the bot API, may be {@code null}
   * @return a {@link ParsedNBT} - never {@code null}
   */
  public static ParsedNBT parse(String nbtString) {
    if (nbtString == null || nbtString.trim().isEmpty()) {
      return new ParsedNBT(new ArrayList<>(), new ArrayList<>(), null);
    }

    try {
      JsonObject root = JsonParser.parseString(nbtString).getAsJsonObject();

      List<String> lore = new ArrayList<>();
      List<Enchantment> enchantments = new ArrayList<>();
      Integer damage = null;

      // Parse the root compound
      if (root.has("value")) {
        JsonObject value = root.getAsJsonObject("value");

        // Extract damage
        damage = extractDamage(value);

        // Extract enchantments
        enchantments = extractEnchantments(value);

        // Extract lore
        lore = extractLore(value);
      }

      return new ParsedNBT(lore, enchantments, damage);

    } catch (Exception e) {
      GGBot.getInstance().logger().warn("Failed to parse NBT data", e);
      GGBot.getInstance().getVersioningHandler().reportError(e);
      return new ParsedNBT(new ArrayList<>(), new ArrayList<>(), null);
    }
  }

  private static Integer extractDamage(JsonObject compound) {
    if (compound.has("Damage")) {
      JsonObject damageObj = compound.getAsJsonObject("Damage");
      if (damageObj.has("value")) {
        return damageObj.get("value").getAsInt();
      }
    }
    return null;
  }

  private static List<Enchantment> extractEnchantments(JsonObject compound) {
    List<Enchantment> enchantments = new ArrayList<>();

    if (compound.has("Enchantments")) {
      JsonObject enchObj = compound.getAsJsonObject("Enchantments");
      if (enchObj.has("value")) {
        JsonObject listValue = enchObj.getAsJsonObject("value");
        if (listValue.has("value") && listValue.get("value").isJsonArray()) {
          JsonArray enchArray = listValue.getAsJsonArray("value");

          for (JsonElement enchElement : enchArray) {
            if (enchElement.isJsonObject()) {
              JsonObject ench = enchElement.getAsJsonObject();
              String id = null;
              int level = 1;

              if (ench.has("id")) {
                JsonObject idObj = ench.getAsJsonObject("id");
                if (idObj.has("value")) {
                  id = idObj.get("value").getAsString();
                }
              }

              if (ench.has("lvl")) {
                JsonObject lvlObj = ench.getAsJsonObject("lvl");
                if (lvlObj.has("value")) {
                  level = lvlObj.get("value").getAsInt();
                }
              }

              if (id != null) {
                enchantments.add(new Enchantment(id, level));
              }
            }
          }
        }
      }
    }

    return enchantments;
  }

  private static List<String> extractLore(JsonObject compound) {
    List<String> lore = new ArrayList<>();

    if (compound.has("display")) {
      JsonObject display = compound.getAsJsonObject("display");
      if (display.has("value")) {
        JsonObject displayValue = display.getAsJsonObject("value");

        // Try to get VV|Protocol1_13_2To1_14|Lore first (pre-formatted with § codes)
        if (displayValue.has("VV|Protocol1_13_2To1_14|Lore")) {
          lore = extractLegacyLore(displayValue.getAsJsonObject("VV|Protocol1_13_2To1_14|Lore"));
        }
        // Otherwise, try to extract from modern Lore format
        else if (displayValue.has("Lore")) {
          lore = extractModernLore(displayValue.getAsJsonObject("Lore"));
        }
      }
    }

    return lore;
  }

  private static List<String> extractLegacyLore(JsonObject loreObj) {
    List<String> lore = new ArrayList<>();

    if (loreObj.has("value")) {
      JsonObject listValue = loreObj.getAsJsonObject("value");
      if (listValue.has("value") && listValue.get("value").isJsonArray()) {
        JsonArray loreArray = listValue.getAsJsonArray("value");

        for (JsonElement loreElement : loreArray) {
          if (loreElement.isJsonPrimitive()) {
            String line = loreElement.getAsString();
            if (!line.trim().isEmpty()) {
              lore.add(line);
            }
          }
        }
      }
    }

    return lore;
  }

  private static List<String> extractModernLore(JsonObject loreObj) {
    List<String> lore = new ArrayList<>();

    if (loreObj.has("value")) {
      JsonObject listValue = loreObj.getAsJsonObject("value");
      if (listValue.has("value") && listValue.get("value").isJsonArray()) {
        JsonArray loreArray = listValue.getAsJsonArray("value");

        for (JsonElement loreElement : loreArray) {
          if (loreElement.isJsonPrimitive()) {
            String jsonText = loreElement.getAsString();
            String converted = convertJsonTextToLegacy(jsonText);
            if (!converted.trim().isEmpty()) {
              lore.add(converted);
            }
          }
        }
      }
    }

    return lore;
  }

  /**
   * Converts a Minecraft JSON text component string into a legacy § colour-code string.
   *
   * @param jsonText JSON text component, may be {@code null} or empty
   * @return legacy-formatted string, or the original input if parsing fails
   */
  private static String convertJsonTextToLegacy(String jsonText) {
    if (jsonText == null || jsonText.trim().isEmpty()) {
      return "";
    }

    try {
      JsonObject json = JsonParser.parseString(jsonText).getAsJsonObject();
      StringBuilder result = new StringBuilder();

      processTextComponent(json, result);

      return result.toString();
    } catch (Exception e) {
      // If parsing fails, return the original string
      GGBot.getInstance().getVersioningHandler().reportError(e);
      return jsonText;
    }
  }

  private static void processTextComponent(JsonObject component, StringBuilder result) {
    // Process "extra" array first
    if (component.has("extra") && component.get("extra").isJsonArray()) {
      JsonArray extra = component.getAsJsonArray("extra");
      for (JsonElement element : extra) {
        if (element.isJsonObject()) {
          processTextComponent(element.getAsJsonObject(), result);
        }
      }
    }

    // Add formatting codes
    String formatting = getFormattingCodes(component);
    result.append(formatting);

    // Add text
    if (component.has("text")) {
      result.append(component.get("text").getAsString());
    }
  }

  private static String getFormattingCodes(JsonObject component) {
    StringBuilder codes = new StringBuilder();

    // Color codes
    if (component.has("color")) {
      String color = component.get("color").getAsString();
      codes.append(getColorCode(color));
    }

    // Format codes
    if (component.has("bold") && component.get("bold").getAsBoolean()) {
      codes.append("§l");
    }
    if (component.has("italic") && component.get("italic").getAsBoolean()) {
      codes.append("§o");
    }
    if (component.has("underlined") && component.get("underlined").getAsBoolean()) {
      codes.append("§n");
    }
    if (component.has("strikethrough") && component.get("strikethrough").getAsBoolean()) {
      codes.append("§m");
    }
    if (component.has("obfuscated") && component.get("obfuscated").getAsBoolean()) {
      codes.append("§k");
    }

    return codes.toString();
  }

  /**
   * Maps a Minecraft colour name to the corresponding § colour code.
   *
   * @param colorName lowercase or mixed-case colour name (e.g. {@code "dark_red"})
   * @return the § colour code string, or an empty string for unknown names
   */
  private static String getColorCode(String colorName) {
    return switch (colorName.toLowerCase()) {
      case "black" -> "§0";
      case "dark_blue" -> "§1";
      case "dark_green" -> "§2";
      case "dark_aqua" -> "§3";
      case "dark_red" -> "§4";
      case "dark_purple" -> "§5";
      case "gold" -> "§6";
      case "gray" -> "§7";
      case "dark_gray" -> "§8";
      case "blue" -> "§9";
      case "green" -> "§a";
      case "aqua" -> "§b";
      case "red" -> "§c";
      case "light_purple" -> "§d";
      case "yellow" -> "§e";
      case "white" -> "§f";
      default -> "";
    };
  }

  /**
   * Extracts the custom display name (with § colour codes) from raw NBT JSON.
   *
   * @param nbtString raw NBT JSON string, may be {@code null}
   * @return the display name string, or {@code null} if no custom name is set
   */
  // Helper method to get display name with § codes
  public static String extractDisplayName(String nbtString) {
    if (nbtString == null || nbtString.trim().isEmpty()) {
      return null;
    }

    try {
      JsonObject root = JsonParser.parseString(nbtString).getAsJsonObject();

      if (root.has("value")) {
        JsonObject value = root.getAsJsonObject("value");

        if (value.has("display")) {
          JsonObject display = value.getAsJsonObject("display");
          if (display.has("value")) {
            JsonObject displayValue = display.getAsJsonObject("value");

            // Try legacy format first
            if (displayValue.has("VV|Protocol1_12_2To1_13|Name")) {
              JsonObject nameObj = displayValue.getAsJsonObject("VV|Protocol1_12_2To1_13|Name");
              if (nameObj.has("value")) {
                return nameObj.get("value").getAsString();
              }
            }

            // Try modern format
            if (displayValue.has("Name")) {
              JsonObject nameObj = displayValue.getAsJsonObject("Name");
              if (nameObj.has("value")) {
                String jsonText = nameObj.get("value").getAsString();
                return convertJsonTextToLegacy(jsonText);
              }
            }
          }
        }
      }
    } catch (Exception e) {
      GGBot.getInstance().logger().warn("Failed to extract display name from NBT", e);
      GGBot.getInstance().getVersioningHandler().reportError(e);
    }

    return null;
  }
}