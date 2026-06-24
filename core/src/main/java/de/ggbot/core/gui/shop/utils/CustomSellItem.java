package de.ggbot.core.gui.shop.utils;

import de.ggbot.sdk.model.BlockPosition;
import de.ggbot.sdk.model.SellItem;
import de.ggbot.sdk.model.SellItemPrice;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;
import java.util.List;

/**
 * A parsed sell item combining data from the API's {@link SellItem} and one of its
 * {@link SellItemPrice} variants. Holds everything the shop UI needs to display and
 * process a single purchasable entry.
 */
public class CustomSellItem {
  private final String id;
  private final String name;
  private final String type;
  private final String nbt;
  private final float price;
  private final long quantity;
  private final BlockPosition position;
  private final NBTParser.ParsedNBT parsedNBT;

  /**
   * @param id       unique item identifier
   * @param name     display name
   * @param type     Minecraft item/block type key (e.g. {@code "diamond_sword"})
   * @param nbt      raw NBT JSON string, may be {@code null}
   * @param price    price per unit
   * @param quantity stack size per purchase
   * @param position chest position in the bot's shop
   */
  public CustomSellItem(String id, String name, String type, String nbt, float price, long quantity, BlockPosition position) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.nbt = nbt;
    this.price = price;
    this.quantity = quantity;
    this.position = position;
    this.parsedNBT = NBTParser.parse(nbt);
  }

  /** @return unique item identifier */
  public String getId() {
    return id;
  }

  /** @return display name configured in the shop */
  public String getName() {
    return name;
  }

  /** @return Minecraft item/block type key (e.g. {@code "diamond_sword"}) */
  public String getType() {
    return type;
  }

  /** @return raw NBT JSON string, may be {@code null} */
  public String getNbt() {
    return nbt;
  }

  /** @return price per purchase */
  public float getPrice() {
    return price;
  }

  /** @return number of items delivered per purchase (stack size) */
  public long getQuantity() {
    return quantity;
  }

  /** @return chest position in the bot's shop layout */
  public BlockPosition getPosition() {
    return position;
  }

  public Icon getIcon() {
    String typeName = this.getType().toLowerCase();
    // Strip namespace prefix (e.g. "minecraft:diamond_sword" → "diamond_sword")
    if (typeName.contains(":")) {
      typeName = typeName.substring(typeName.indexOf(':') + 1);
    }
    // We need to find a working solution for reliably loading item textures, since the vanilla Minecraft resource location.
    // That, currently however, does not work so for now we use manually added item textures. (The same one we use for our own webpanel.)
    /*ResourceLocation resourceLocation = ResourceLocation.create(
        "minecraft", "textures/item/" + typeName + ".png"
    );
    if (!resourceLocation.exists()) {
      resourceLocation = ResourceLocation.create(
          "minecraft", "textures/block/" + typeName + ".png"
      );
    }*/
    ResourceLocation resourceLocation = ResourceLocation.create(
        "ggbot", "themes/vanilla/textures/icons/items/" + typeName + ".png"
    );
    if (!resourceLocation.exists()) {
      resourceLocation = ResourceLocation.create(
          "minecraft", "themes/vanilla/textures/icons/items/barrier.png"
      );
    }
    return Icon.texture(resourceLocation);
  }

  /** @return lore lines parsed from the item's NBT data */
  public List<String> getLore() {
    return parsedNBT.getLore();
  }

  /** @return enchantments parsed from the item's NBT data */
  public List<NBTParser.Enchantment> getEnchantments() {
    return parsedNBT.getEnchantments();
  }

  /** @return item damage value, or {@code null} if absent */
  public Integer getDamage() {
    return parsedNBT.getDamage();
  }

  /** @return {@code true} if the NBT contained a damage tag */
  public boolean hasDamage() {
    return parsedNBT.hasDamage();
  }

  /**
   * Returns the display name with § colour codes if the item has a custom name,
   * otherwise falls back to the plain {@link #getName()} value.
   *
   * @return best available display string for this item
   */
  public String getDisplayName() {
    String customName = NBTParser.extractDisplayName(nbt);
    return customName != null ? customName : name;
  }
}