package de.ggbot.core.gui.shop.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;

public class NBTParser {

  public static class ParsedNBT {
    private final List<String> lore;
    private final List<Enchantment> enchantments;
    private final Integer damage;

    public ParsedNBT(List<String> lore, List<Enchantment> enchantments, Integer damage) {
      this.lore = lore;
      this.enchantments = enchantments;
      this.damage = damage;
    }

    public List<String> getLore() {
      return lore;
    }

    public List<Enchantment> getEnchantments() {
      return enchantments;
    }

    public Integer getDamage() {
      return damage;
    }

    public boolean hasDamage() {
      return damage != null;
    }
  }

  public static class Enchantment {
    private final String id;
    private final int level;

    public Enchantment(String id, int level) {
      this.id = id;
      this.level = level;
    }

    public String getId() {
      return id;
    }

    public int getLevel() {
      return level;
    }

    public String getDisplayLevel() {
      switch (level) {
        case 1: return "I";
        case 2: return "II";
        case 3: return "III";
        case 4: return "IV";
        case 5: return "V";
        default: return String.valueOf(level);
      }
    }

    public String getDisplayName() {
      // Convert enchantment IDs to display names
      switch (id) {
        case "power": return "Power";
        case "punch": return "Punch";
        case "unbreaking": return "Unbreaking";
        case "luck_of_the_sea": return "Luck of the Sea";
        case "sharpness": return "Sharpness";
        case "protection": return "Protection";
        case "fire_aspect": return "Fire Aspect";
        case "looting": return "Looting";
        case "fortune": return "Fortune";
        case "efficiency": return "Efficiency";
        case "silk_touch": return "Silk Touch";
        case "mending": return "Mending";
        default: return id;
      }
    }
  }

  /**
   * Parse NBT string into structured data
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
      e.printStackTrace();
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
   * Convert Minecraft JSON text format to legacy format with § codes
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

  private static String getColorCode(String colorName) {
    switch (colorName.toLowerCase()) {
      case "black": return "§0";
      case "dark_blue": return "§1";
      case "dark_green": return "§2";
      case "dark_aqua": return "§3";
      case "dark_red": return "§4";
      case "dark_purple": return "§5";
      case "gold": return "§6";
      case "gray": return "§7";
      case "dark_gray": return "§8";
      case "blue": return "§9";
      case "green": return "§a";
      case "aqua": return "§b";
      case "red": return "§c";
      case "light_purple": return "§d";
      case "yellow": return "§e";
      case "white": return "§f";
      default: return "";
    }
  }

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
      e.printStackTrace();
    }

    return null;
  }
}