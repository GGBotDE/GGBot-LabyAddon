package de.ggbot.core.gui.botmenu;

import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;

/**
 * Resolves a Minecraft item type to its bundled texture icon, mirroring the
 * shop's resolution (the addon ships item textures under
 * {@code themes/vanilla/textures/icons/items/}).
 */
public final class ItemIcons {

  private ItemIcons() {}

  public static Icon forItemType(String itemType) {
    String typeName = itemType == null ? "" : itemType.toLowerCase();
    if (typeName.contains(":")) {
      typeName = typeName.substring(typeName.indexOf(':') + 1);
    }
    ResourceLocation location = ResourceLocation.create(
        "ggbot", "themes/vanilla/textures/icons/items/" + typeName + ".png");
    if (typeName.isEmpty() || !location.exists()) {
      location = ResourceLocation.create(
          "ggbot", "themes/vanilla/textures/icons/items/barrier.png");
    }
    return Icon.texture(location);
  }
}
