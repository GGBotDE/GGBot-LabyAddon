package de.ggbot.core.gui.botmenu;

import de.ggbot.core.overlay.OverlayBox;
import de.ggbot.core.overlay.OverlayColor;
import de.ggbot.core.overlay.OverlayManager;
import de.ggbot.core.overlay.WorldPosition;
import de.ggbot.core.utils.TextUtil;
import de.ggbot.sdk.model.KickArea;
import de.ggbot.sdk.model.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns the selected bot's kick areas (SDK {@link KickArea} models) into labelled
 * world overlay boxes when the "highlight kick areas" toggle is active.
 */
public final class KickAreaHighlighter {

  private static final String ID_PREFIX = "ggbot-kickarea-";
  private static final int MAX_AREAS = 256;

  private static List<KickArea> lastAreas = new ArrayList<>();

  private KickAreaHighlighter() {}

  /** Stores the latest kick areas and rebuilds the overlays. */
  public static void update(List<KickArea> areas) {
    lastAreas = areas == null ? new ArrayList<>() : areas;
    refresh();
  }

  /** Rebuilds kick-area overlays from the stored list based on the toggle state. */
  public static void refresh() {
    OverlayManager manager = OverlayManager.getInstance();
    for (int i = 0; i < MAX_AREAS; i++) {
      manager.removeById(ID_PREFIX + i);
    }
    if (!BotMenuState.get().isHighlightKickAreas()) {
      return;
    }

    int index = 0;
    for (KickArea area : lastAreas) {
      Position start = area.getStartPos();
      Position end = area.getEndPos();
      if (start == null || end == null
          || start.getX() == null || end.getX() == null) {
        continue;
      }
      WorldPosition min = new WorldPosition(
          Math.min(start.getX(), end.getX()),
          Math.min(start.getY(), end.getY()),
          Math.min(start.getZ(), end.getZ()));
      WorldPosition max = new WorldPosition(
          Math.max(start.getX(), end.getX()) + 1,
          Math.max(start.getY(), end.getY()) + 1,
          Math.max(start.getZ(), end.getZ()) + 1);
      de.ggbot.core.cfg.OverlaySubConfig cfg =
          de.ggbot.core.GGBot.getInstance().configuration().overlaySub;
      int color = cfg.kickAreaColor.get();
      int fill = Boolean.TRUE.equals(cfg.fillAreas.get())
          ? OverlayColor.withAlpha(color, cfg.fillOpacity.get()) : 0;
      OverlayBox box = new OverlayBox(ID_PREFIX + index, min, max,
          color, fill, cfg.lineWidth.get());
      if (area.getName() != null) {
        box.withLabel(TextUtil.stripColors(area.getName()));
      }
      manager.addBox(box);
      index++;
      if (index >= MAX_AREAS) {
        break;
      }
    }
  }
}
