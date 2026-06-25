package de.ggbot.core.gui.botmenu;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import de.ggbot.core.overlay.OverlayBox;
import de.ggbot.core.overlay.OverlayColor;
import de.ggbot.core.overlay.OverlayManager;
import de.ggbot.core.overlay.WorldPosition;
import de.ggbot.sdk.model.Bot;
import net.labymod.api.Laby;
import net.labymod.api.client.world.phys.hit.BlockHitResult;
import net.labymod.api.client.world.phys.hit.HitResult;

/**
 * WorldEdit-style {@code pos1}/{@code pos2} selection. Positions are taken from
 * the block the player is looking at, visualised as overlay boxes, and can be
 * sent to the bot as {@code !pos1}/{@code !pos2} commands.
 */
public final class BotMenuPositions {

  private static final String POS1_ID = "ggbot-worldedit-pos1";
  private static final String POS2_ID = "ggbot-worldedit-pos2";
  private static final String AREA_ID = "ggbot-worldedit-area";

  private BotMenuPositions() {}

  /** Sets pos1 (or pos2) to the block currently looked at, and updates overlays. */
  public static void setFromLook(boolean pos1) {
    WorldPosition block = lookedAtBlock();
    if (block == null) return;
    if (pos1) {
      BotMenuState.get().setPos1(block);
    } else {
      BotMenuState.get().setPos2(block);
    }
    refreshOverlays();
  }

  /** Clears the client-side pos1/pos2 selection and removes its overlay boxes. */
  public static void clearSelection() {
    BotMenuState.get().setPos1(null);
    BotMenuState.get().setPos2(null);
    OverlayManager m = OverlayManager.getInstance();
    m.removeById(POS1_ID);
    m.removeById(POS2_ID);
    m.removeById(AREA_ID);
  }

  /** Rebuilds the pos1/pos2/area overlay boxes from the current selection. */
  public static void refreshOverlays() {
    OverlayManager m = OverlayManager.getInstance();
    m.removeById(POS1_ID);
    m.removeById(POS2_ID);
    m.removeById(AREA_ID);

    WorldPosition p1 = BotMenuState.get().getPos1();
    WorldPosition p2 = BotMenuState.get().getPos2();

    if (p1 != null) {
      m.addBox(blockBox(POS1_ID, p1, OverlayColor.GREEN));
    }
    if (p2 != null) {
      m.addBox(blockBox(POS2_ID, p2, OverlayColor.BLUE));
    }
    if (p1 != null && p2 != null) {
      WorldPosition min = new WorldPosition(
          Math.min(p1.getX(), p2.getX()),
          Math.min(p1.getY(), p2.getY()),
          Math.min(p1.getZ(), p2.getZ()));
      WorldPosition max = new WorldPosition(
          Math.max(p1.getX(), p2.getX()) + 1,
          Math.max(p1.getY(), p2.getY()) + 1,
          Math.max(p1.getZ(), p2.getZ()) + 1);
      m.addBox(new OverlayBox(AREA_ID, min, max,
          OverlayColor.YELLOW, OverlayColor.withAlpha(OverlayColor.YELLOW, 40), 2.0f));
    }
  }

  /** Sends the selected positions to the bot as {@code !pos1}/{@code !pos2}. */
  public static void sendToBot(Bot bot) {
    if (bot == null) return;
    GGBot addon = GGBot.getInstance();
    WorldPosition p1 = BotMenuState.get().getPos1();
    WorldPosition p2 = BotMenuState.get().getPos2();
    if (p1 != null) {
      BotRequests.sendCommandToBot(addon, bot, "!pos1 "
          + (int) p1.getX() + " " + (int) p1.getY() + " " + (int) p1.getZ());
    }
    if (p2 != null) {
      BotRequests.sendCommandToBot(addon, bot, "!pos2 "
          + (int) p2.getX() + " " + (int) p2.getY() + " " + (int) p2.getZ());
    }
  }

  private static OverlayBox blockBox(String id, WorldPosition p, int color) {
    return new OverlayBox(id, p,
        new WorldPosition(p.getX() + 1, p.getY() + 1, p.getZ() + 1), color);
  }

  private static WorldPosition lookedAtBlock() {
    HitResult hit = Laby.labyAPI().minecraft().getHitResult();
    if (hit == null || hit.type() != HitResult.HitType.BLOCK
        || !(hit instanceof BlockHitResult block)) {
      return null;
    }
    return new WorldPosition(
        Math.floor(block.getBlockPosition().getX()),
        Math.floor(block.getBlockPosition().getY()),
        Math.floor(block.getBlockPosition().getZ()));
  }
}
