package de.ggbot.core.gui.botmenu;

import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.screen.ScreenContext;
import net.labymod.api.client.world.item.ItemStack;

/**
 * How an item is drawn: through the game when it knows the item, so resource packs
 * and mods apply, otherwise with the icon.
 *
 * @param stack the item of the game, or {@code null} if it does not know it
 * @param icon  the icon to fall back to
 */
public record ItemIcon(ItemStack stack, Icon icon) {

  private static final float ITEM_SIZE = 16F;

  /**
   * Manages the control mode for steering a selected bot with mouse and movement keys.
   */
  public void submit(ScreenContext context, float x, float y, float size) {
    float scale = size / ITEM_SIZE;
    context.pushStack();
    context.translate(x, y, 0);
    context.scale(scale, scale, 1);
    context.canvas().submitItem(stack, 0, 0, false);
    context.popStack();
  }
}
