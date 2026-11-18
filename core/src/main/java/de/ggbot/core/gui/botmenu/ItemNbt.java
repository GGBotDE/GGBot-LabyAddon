package de.ggbot.core.gui.botmenu;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.labymod.api.nbt.NBTFactory;
import net.labymod.api.nbt.NBTTag;
import net.labymod.api.nbt.tags.NBTTagCompound;
import net.labymod.api.nbt.tags.NBTTagFloat;
import net.labymod.api.nbt.tags.NBTTagList;
import java.util.Map;

/**
 * Parses raw NBT data into a structured JSON object for item metadata.
 *
 * This class provides methods to read and interpret raw NBT data from the API. The NBT data is structured as a prismarine-nbt tree, where each value is wrapped with a type. Plain trees without type information are also read but cannot be used to rebuild complex tags.
 */
public final class ItemNbt {

  private static final String MODEL_DATA_COMPONENT = "minecraft:custom_model_data";
  private static final String GLINT_COMPONENT = "minecraft:enchantment_glint_override";

  private ItemNbt() {}

  /**
   * Parses NBT data into a JSON object representing item metadata.
   *
   * @param nbt the raw NBT data as a string
   * @return the parsed JSON object or null if parsing fails
   */
  public static JsonObject parse(String nbt) {
    if (nbt == null || nbt.isBlank()) return null;
    try {
      JsonElement element = JsonParser.parseString(nbt);
      return element.isJsonObject() ? element.getAsJsonObject() : null;
    } catch (RuntimeException e) {
      return null;
    }
  }

  /**
   * Extracts custom model data from an item's NBT, returning null if it's missing or invalid.
   *
   * @param nbt the NBT object containing item data
   * @return the custom model data as an integer, or null if not found or invalid
   */
  public static Integer customModelData(JsonObject nbt) {
    JsonElement value = child(nbt, "CustomModelData");
    if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
      return null;

    return value.getAsInt();
  }

  /**
   * Checks if an item is enchanted by examining its NBT data.
   *
   * @param nbt the NBT object containing item data
   * @return true if the item is enchanted, false otherwise
   */
  public static boolean isEnchanted(JsonObject nbt) {
    return hasEntries(child(nbt, "ench")) || hasEntries(child(nbt, "Enchantments"));
  }

  /**
   * Converts NBT data into a compound tag for older versions of the game.
   *
   * @param factory the NBT factory used to create tags
   * @param nbt the NBT data to convert
   * @return the compound tag or null if the input is invalid
   */
  public static NBTTagCompound toCompound(NBTFactory factory, JsonObject nbt) {
    if (!isNode(nbt) || !"compound".equals(nbt.get("type").getAsString())) return null;
    return (NBTTagCompound) tag(factory, "compound", nbt.get("value"));
  }

  /**
   * Constructs item components for newer game versions that use component-based data storage.
   *
   * @param factory the NBT factory used to create tags
   * @param nbt the NBT data containing item information
   * @param floats indicates if custom model data should be represented as floats
   * @return the constructed components as an NBTTagCompound
   */
  public static NBTTagCompound toComponents(NBTFactory factory, JsonObject nbt, boolean floats) {
    NBTTagCompound components = factory.compound();
    Integer modelData = customModelData(nbt);
    if (modelData != null) {
      components.set(MODEL_DATA_COMPONENT,
          floats ? floatModelData(factory, modelData) : factory.create(modelData.intValue()));
    }
    if (isEnchanted(nbt)) 
      components.set(GLINT_COMPONENT, factory.create(true));
    return components;
  }

  private static NBTTagCompound floatModelData(NBTFactory factory, int modelData) {
    NBTTagList<Float, NBTTagFloat> values = factory.list();
    values.add(factory.create((float) modelData));
    NBTTagCompound compound = factory.compound();
    compound.set("floats", values);
    return compound;
  }

  private static boolean isNode(JsonElement element) {
    return element != null && element.isJsonObject()
        && element.getAsJsonObject().has("type") && element.getAsJsonObject().has("value");
  }

  private static JsonElement child(JsonObject nbt, String key) {
    if (nbt == null) return null;
    JsonElement root = isNode(nbt) ? nbt.get("value") : nbt;
    if (!root.isJsonObject()) return null;
    JsonElement child = root.getAsJsonObject().get(key);
    return isNode(child) ? child.getAsJsonObject().get("value") : child;
  }

  private static boolean hasEntries(JsonElement list) {
    if (isNode(list)) return hasEntries(list.getAsJsonObject().get("value"));
    return list != null && list.isJsonArray() && !list.getAsJsonArray().isEmpty();
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static NBTTag<?> tag(NBTFactory factory, String type, JsonElement value) {
    switch (type) {
      case "compound": {
        NBTTagCompound compound = factory.compound();
        for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
          if (!isNode(entry.getValue())) continue;
          JsonObject node = entry.getValue().getAsJsonObject();
          NBTTag<?> child = tag(factory, node.get("type").getAsString(), node.get("value"));
          if (child != null) compound.set(entry.getKey(), child);
        }
        return compound;
      }
      case "list": {
        JsonObject inner = value.getAsJsonObject();
        String itemType = inner.get("type").getAsString();
        NBTTagList list = factory.list();
        for (JsonElement element : inner.getAsJsonArray("value")) {
          NBTTag<?> child = tag(factory, itemType, element);
          if (child != null) list.add(child);
        }
        return list;
      }
      case "string":
        return factory.create(value.getAsString());
      case "byte":
        return factory.create(value.getAsByte());
      case "short":
        return factory.create(value.getAsShort());
      case "int":
        return factory.create(value.getAsInt());
      case "float":
        return factory.create(value.getAsFloat());
      case "double":
        return factory.create(value.getAsDouble());
      case "long":
        return factory.create(asLong(value));
      case "byteArray": {
        JsonArray array = value.getAsJsonArray();
        byte[] bytes = new byte[array.size()];
        for (int i = 0; i < bytes.length; i++) bytes[i] = array.get(i).getAsByte();
        return factory.create(bytes);
      }
      case "intArray": {
        JsonArray array = value.getAsJsonArray();
        int[] ints = new int[array.size()];
        for (int i = 0; i < ints.length; i++) ints[i] = array.get(i).getAsInt();
        return factory.create(ints);
      }
      default:
        return null;
    }
  }

  /**
   * Converts JSON elements into long values for NBT tag reconstruction.
   *
   * @param value the JSON element to convert
   * @return the long value representing the NBT tag data
   */
  private static long asLong(JsonElement value) {
    if (!value.isJsonArray()) return value.getAsLong();
    JsonArray halves = value.getAsJsonArray();
    return ((long) halves.get(0).getAsInt() << 32) | (halves.get(1).getAsInt() & 0xFFFFFFFFL);
  }
}
