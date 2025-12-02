package de.ggbot.core.gui.shop.utils;

import de.ggbot.sdk.model.BlockPosition;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;
import java.util.List;

public class CustomSellItem {
  private final String id;
  private final String name;
  private final String type;
  private final String nbt;
  private final float price;
  private final long quantity;
  private final BlockPosition position;
  private final NBTParser.ParsedNBT parsedNBT;

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

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getNbt() {
    return nbt;
  }

  public float getPrice() {
    return price;
  }

  public long getQuantity() {
    return quantity;
  }

  public BlockPosition getPosition() {
    return position;
  }

  public Icon getIcon() {
    ResourceLocation resourceLocation = ResourceLocation.create(
        "minecraft", "textures/item/" + this.getType().toLowerCase() + ".png"
    );
    if (!resourceLocation.exists()) {
      resourceLocation = ResourceLocation.create(
          "minecraft", "textures/block/" + this.getType().toLowerCase() + ".png"
      );
    }
    if(!resourceLocation.exists()) {
      resourceLocation = ResourceLocation.create(
          "minecraft", "textures/item/barrier.png"
      );
    }

    return Icon.texture(resourceLocation);
  }

  public List<String> getLore() {
    return parsedNBT.getLore();
  }

  public List<NBTParser.Enchantment> getEnchantments() {
    return parsedNBT.getEnchantments();
  }

  public Integer getDamage() {
    return parsedNBT.getDamage();
  }

  public boolean hasDamage() {
    return parsedNBT.hasDamage();
  }

  // Get the display name with § codes (if custom name exists)
  public String getDisplayName() {
    String customName = NBTParser.extractDisplayName(nbt);
    return customName != null ? customName : name;
  }
}