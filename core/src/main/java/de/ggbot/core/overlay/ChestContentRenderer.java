package de.ggbot.core.overlay;

import de.ggbot.core.gui.botmenu.BotMenuState;
import de.ggbot.core.gui.botmenu.ChestItemCache;
import de.ggbot.core.gui.botmenu.ItemIcon;
import de.ggbot.core.gui.botmenu.ItemIcons;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.render.matrix.Stack;
import net.labymod.api.client.world.phys.hit.BlockHitResult;
import net.labymod.api.client.world.phys.hit.HitResult;
import net.labymod.api.event.Phase;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.render.overlay.IngameOverlayRenderEvent;
import java.util.ArrayList;
import java.util.List;

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

  private static final Component LOOK_TITLE =
      Component.translatable("ggbot.botmenu.chest.contents", NamedTextColor.AQUA);

  private final List<ItemIcon> lookIcons = new ArrayList<>();
  private final List<Component> lookLines = new ArrayList<>();
  private boolean looking = false;
  private int lookX;
  private int lookY;
  private int lookZ;
  private int lookVersion;

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

  /**
   * Executes frame-based updates for chest content highlights.
   *
   * @param event the in-game overlay render event
   */
  private void updateLookHighlight(IngameOverlayRenderEvent event) {
    HitResult hit = Laby.labyAPI().minecraft().getHitResult();
    if (!BotMenuState.get().isShowChestContents() || hit == null
        || hit.type() != HitResult.HitType.BLOCK || !(hit instanceof BlockHitResult block)) {
      clearLook();
      return;
    }

    int bx = (int) Math.floor(block.getBlockPosition().getX());
    int by = (int) Math.floor(block.getBlockPosition().getY());
    int bz = (int) Math.floor(block.getBlockPosition().getZ());
    int version = ChestItemCache.version();
    if (!looking || bx != lookX || by != lookY || bz != lookZ || version != lookVersion) {
      loadLook(bx, by, bz, version);
    }
    if (lookIcons.isEmpty()) return;

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
    drawText(stack, LOOK_TITLE, x, y);
    y += 12;
    for (int i = 0; i < lookIcons.size(); i++) {
      ItemIcon icon = lookIcons.get(i);
      if (icon.stack() == null) {
        if (canvas != null) {
          canvas.submitIcon(icon.icon(), x, y - 1, 10, 10);
        } else {
          icon.icon().render(stack, x, y - 1, 10, 10);
        }
      } else if (canvas != null) {
        icon.submit(event.context(), x, y - 1, 10);
      } else {
        stack.push();
        stack.translate(x, y - 1, 0F);
        stack.scale(10F / 16F, 10F / 16F, 1F);
        Laby.labyAPI().minecraft().itemStackRenderer().renderItemStack(stack, icon.stack(), 0, 0);
        stack.pop();
      }
      drawText(stack, lookLines.get(i), x + 12, y);
      y += 11;
    }
    this.canvas = null;
  }

  private void clearLook() {
    if (!looking) return;
    looking = false;
    lookIcons.clear();
    lookLines.clear();
    OverlayManager m = OverlayManager.getInstance();
    m.removeById(LOOK_ID);
    m.removeById(LOOK_ID2);
  }

  /**
   * Gathers the target block and adjacent double chest components.
   *
   * @param bx the x-coordinate of the block
   * @param by the y-coordinate of the block
   * @param bz the z-coordinate of the block
   * @param version the cache version
   */
  private void loadLook(int bx, int by, int bz, int version) {
    clearLook();
    looking = true;
    lookX = bx;
    lookY = by;
    lookZ = bz;
    lookVersion = version;

    List<ChestItemCache.Entry> items = new ArrayList<>(ChestItemCache.itemsAtChest(bx, by, bz));
    List<int[]> highlighted = new ArrayList<>();
    if (!items.isEmpty()) highlighted.add(new int[]{bx, by, bz});

    int[][] neighbours = {{bx + 1, by, bz}, {bx - 1, by, bz}, {bx, by, bz + 1}, {bx, by, bz - 1}};
    for (int[] n : neighbours) {
      List<ChestItemCache.Entry> ni = ChestItemCache.itemsAtChest(n[0], n[1], n[2]);
      if (!ni.isEmpty()) {
        items.addAll(ni);
        highlighted.add(n);
      }
    }

    OverlayManager m = OverlayManager.getInstance();
    for (int i = 0; i < highlighted.size() && i < 2; i++) {
      int[] pos = highlighted.get(i);
      m.addBox(OverlayBox.singleBlock(i == 0 ? LOOK_ID : LOOK_ID2,
          pos[0], pos[1], pos[2], OverlayColor.CYAN));
    }
    for (ChestItemCache.Entry e : items) {
      lookIcons.add(ItemIcons.forItem(e.itemType(), e.nbt()));
      lookLines.add(Component.text(e.name(), NamedTextColor.YELLOW)
          .append(Component.text(" [" + e.shop() + "]", NamedTextColor.GRAY)));
    }
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
