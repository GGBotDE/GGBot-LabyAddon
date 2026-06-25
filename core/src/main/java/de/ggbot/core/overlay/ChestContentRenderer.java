package de.ggbot.core.overlay;

import de.ggbot.core.gui.botmenu.BotMenuState;
import de.ggbot.core.gui.botmenu.ChestItemCache;
import de.ggbot.core.gui.botmenu.ItemIcons;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.render.matrix.Stack;
import net.labymod.api.client.world.phys.hit.BlockHitResult;
import net.labymod.api.client.world.phys.hit.HitResult;
import net.labymod.api.event.Phase;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.render.overlay.IngameOverlayRenderEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Shows the contents of a buy/sell chest when the player looks at it, and marks
 * the chest(s) containing an item matching the menu's item search.
 *
 * <p>Double chests are handled by also checking the four horizontal neighbours of
 * the looked-at block (we can't read block state version-independently, so we
 * aggregate adjacent chest entries - which is exactly the second half of a double
 * chest).
 *
 * <p>Guideline note (per-frame allocation): the search-marker overlays are only
 * rebuilt when the search term changes, and the chest-content text is only built
 * while the "show chest contents" toggle is active and the player is looking at a
 * matching chest. In the common case (toggle off) this listener does no
 * allocation, so the per-frame impact is negligible.
 */
public class ChestContentRenderer {

  private static final String SEARCH_PREFIX = "ggbot-itemsearch-";
  private static final String LOOK_ID = "ggbot-chestlook";
  private static final String LOOK_ID2 = "ggbot-chestlook2";

  private String lastSearch = "";
  private boolean sawPost = false;
  private net.labymod.api.client.gui.screen.state.ScreenCanvas canvas;

  @Subscribe
  public void onOverlayRender(IngameOverlayRenderEvent event) {
    if (event.phase() == Phase.POST) {
      sawPost = true;
    }
    // Prefer POST; if a version never emits POST, draw on PRE instead.
    if (!(event.phase() == Phase.POST || (!sawPost && event.phase() == Phase.PRE))) return;

    updateSearchMarkers();
    updateLookHighlight(event);
  }

  private void updateSearchMarkers() {
    String search = BotMenuState.get().getItemSearch();
    if (search.equals(lastSearch)) return;
    lastSearch = search;

    OverlayManager m = OverlayManager.getInstance();
    for (int i = 0; i < 64; i++) {
      m.removeById(SEARCH_PREFIX + i);
    }
    if (search.isBlank()) return;

    List<ChestItemCache.Entry> matches = ChestItemCache.matching(search);
    int i = 0;
    for (ChestItemCache.Entry e : matches) {
      m.addBox(OverlayBox.singleBlock(SEARCH_PREFIX + i, e.x(), e.y(), e.z(),
          OverlayColor.MAGENTA, OverlayColor.withAlpha(OverlayColor.MAGENTA, 50)));
      i++;
      if (i >= 64) break;
    }
  }

  private void updateLookHighlight(IngameOverlayRenderEvent event) {
    OverlayManager m = OverlayManager.getInstance();
    m.removeById(LOOK_ID);
    m.removeById(LOOK_ID2);

    if (!BotMenuState.get().isShowChestContents()) return;

    HitResult hit = Laby.labyAPI().minecraft().getHitResult();
    if (hit == null || hit.type() != HitResult.HitType.BLOCK
        || !(hit instanceof BlockHitResult block)) {
      return;
    }

    int bx = (int) Math.floor(block.getBlockPosition().getX());
    int by = (int) Math.floor(block.getBlockPosition().getY());
    int bz = (int) Math.floor(block.getBlockPosition().getZ());

    // Aggregate the looked block plus horizontal neighbours (double chest halves).
    List<ChestItemCache.Entry> items = new ArrayList<>(ChestItemCache.itemsAtChest(bx, by, bz));
    Set<int[]> highlighted = new LinkedHashSet<>();
    if (!items.isEmpty()) highlighted.add(new int[]{bx, by, bz});

    int[][] neighbours = {{bx + 1, by, bz}, {bx - 1, by, bz}, {bx, by, bz + 1}, {bx, by, bz - 1}};
    for (int[] n : neighbours) {
      List<ChestItemCache.Entry> ni = ChestItemCache.itemsAtChest(n[0], n[1], n[2]);
      if (!ni.isEmpty()) {
        items.addAll(ni);
        highlighted.add(n);
      }
    }

    if (items.isEmpty()) return;

    int idx = 0;
    for (int[] pos : highlighted) {
      m.addBox(OverlayBox.singleBlock(idx == 0 ? LOOK_ID : LOOK_ID2,
          pos[0], pos[1], pos[2], OverlayColor.CYAN));
      idx++;
      if (idx >= 2) break;
    }

    Stack stack = event.stack();
    // Alternate path (1.21.8+): draw via the new canvas API instead of the legacy
    // render pipeline (which produces no output on those versions).
    this.canvas = altRenderMode() ? event.context().canvas() : null;
    // Position is configurable as a percentage of the scaled screen size.
    de.ggbot.core.cfg.OverlaySubConfig cfg =
        de.ggbot.core.GGBot.getInstance().configuration().overlaySub;
    float sw = Laby.labyAPI().minecraft().minecraftWindow().getScaledWidth();
    float sh = Laby.labyAPI().minecraft().minecraftWindow().getScaledHeight();
    float x = sw * (cfg.chestContentsX.get() / 100f);
    float y = sh * (cfg.chestContentsY.get() / 100f);
    drawText(stack, Component.translatable("ggbot.botmenu.chest.contents", NamedTextColor.AQUA),
        x, y);
    y += 12;
    for (ChestItemCache.Entry e : items) {
      Icon icon = ItemIcons.forItemType(e.itemType());
      if (canvas != null) {
        canvas.submitIcon(icon, x, y - 1, 10, 10);
      } else {
        icon.render(stack, x, y - 1, 10, 10);
      }
      Component line = Component.text(e.name(), NamedTextColor.YELLOW)
          .append(Component.text(" [" + e.shop() + "]", NamedTextColor.GRAY));
      drawText(stack, line, x + 12, y);
      y += 11;
    }
    this.canvas = null;
  }

  private boolean altRenderMode() {
    return de.ggbot.core.utils.RenderEngine.useCanvas();
  }

  /** Fixed text-box width on the canvas so all lines share one horizontal centre
   *  (the canvas centres text within [x, x+maxWidth]; a per-line width scatters them). */
  private static final int CHEST_TEXT_BOX = 100;

  private void drawText(Stack stack, Component text, float x, float y) {
    if (canvas != null) {
      // (component, x, y, color, scale, maxWidth); -1 = opaque, scale 1.0 avoids
      // inheriting a previous scale. A constant box keeps the column consistent.
      canvas.submitComponent(text, x, y, -1, 1.0f, CHEST_TEXT_BOX);
      return;
    }
    Laby.references().renderPipeline().componentRenderer().builder()
        .text(text)
        .pos(x, y)
        .shadow(true)
        .render(stack);
  }
}
