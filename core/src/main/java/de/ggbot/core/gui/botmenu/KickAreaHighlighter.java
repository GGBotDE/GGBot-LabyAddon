package de.ggbot.core.gui.botmenu;

import de.ggbot.core.cfg.OverlaySubConfig;
import de.ggbot.core.overlay.OverlayBox;
import de.ggbot.core.overlay.OverlayColor;
import de.ggbot.core.overlay.OverlayManager;
import de.ggbot.core.overlay.WorldPosition;
import de.ggbot.core.utils.TextUtil;
import de.ggbot.sdk.model.KickArea;
import de.ggbot.sdk.model.Position;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts SDK {@link KickArea} models into labelled boxes displayed in the world.
 * Areas are rendered whenever the "highlight kick areas" option is enabled or
 * when a specific area has been selected from the kick area list in the menu.
 */
public final class KickAreaHighlighter {

  private static final String ID_PREFIX = "ggbot-kickarea-";
  private static final int MAX_AREAS = 256;

  /**
   * Maintains kick areas associated with each bot to facilitate overlay updates.
   */
  private static final Map<Long, List<KickArea>> byBot = new LinkedHashMap<>();

  /**
   * Manages overlay IDs for active kick areas to ensure accurate rendering and cleanup.
   */
  private static final Set<String> drawn = new HashSet<>();

  private KickAreaHighlighter() {}

  /**
   * Creates a unique identifier for kick areas to manage their rendering and visibility.
   *
   * @param botId the ID of the bot associated with the kick area
   * @param area the kick area object containing additional data
   * @return a unique string identifier for the overlay
   */
  public static String overlayId(long botId, KickArea area) {
    return ID_PREFIX + botId + ":" + (area.getId() != null ? area.getId() : "h" + area.hashCode());
  }

  /**
   * Removes all kick areas for inactive bots to stop associated overlays.
   */
  public static void clear() {
    synchronized (byBot) {
      byBot.clear();
    }
    refresh();
  }

  /**
   * Updates a bot's kick areas and initiates overlay regeneration.
   *
   * @param botId the ID of the bot whose kick areas are being updated
   * @param areas the list of kick areas associated with the bot
   */
  public static void update(long botId, List<KickArea> areas) {
    synchronized (byBot) {
      byBot.put(botId, areas == null ? new ArrayList<>() : areas);
    }
    refresh();
  }

  /**
   * Regenerates kick-area overlays using current settings and stored data.
   */
  public static void refresh() {
    OverlayManager manager = OverlayManager.getInstance();
    OverlaySubConfig cfg = de.ggbot.core.GGBot.getInstance().configuration().overlaySub;
    int color = cfg.kickAreaColor.get();
    int fill = Boolean.TRUE.equals(cfg.fillAreas.get())
        ? OverlayColor.withAlpha(color, cfg.fillOpacity.get()) : 0;
    boolean showAll = BotMenuState.get().isHighlightKickAreas();

    synchronized (byBot) {
      for (String id : drawn) {
        manager.removeById(id);
      }
      drawn.clear();
      for (Map.Entry<Long, List<KickArea>> entry : byBot.entrySet()) {
        for (KickArea area : entry.getValue()) {
          String id = overlayId(entry.getKey(), area);
          if (!showAll && !BotMenuState.get().isAreaVisible(id)) {
            continue;
          }
          OverlayBox box = toBox(id, area, color, fill, cfg.lineWidth.get());
          if (box == null) {
            continue;
          }
          manager.addBox(box);
          drawn.add(id);
          if (drawn.size() >= MAX_AREAS) {
            return;
          }
        }
      }
    }
  }

  /**
   * Verifies if a Position object contains valid X, Y, and Z coordinates.
   *
   * @param pos the Position object to validate
   * @return true if all coordinates are present, false otherwise
   */
  private static boolean complete(Position pos) {
    return pos != null && pos.getX() != null && pos.getY() != null && pos.getZ() != null;
  }

  /**
   * Generates a bounding box for a kick area if both corners are fully defined.
   *
   * @param id the unique identifier for the overlay
   * @param area the kick area containing start and end positions
   * @param color the color of the overlay
   * @param fill the fill style for the overlay
   * @param lineWidth the width of the overlay line
   * @return the generated OverlayBox or null if positions are incomplete
   */
  private static OverlayBox toBox(String id, KickArea area, int color, int fill, float lineWidth) {
    Position start = area.getStartPos();
    Position end = area.getEndPos();
    if (!complete(start) || !complete(end)) {
      return null;
    }
    WorldPosition min = new WorldPosition(
        Math.min(start.getX(), end.getX()),
        Math.min(start.getY(), end.getY()),
        Math.min(start.getZ(), end.getZ()));
    WorldPosition max = new WorldPosition(
        Math.max(start.getX(), end.getX()) + 1,
        Math.max(start.getY(), end.getY()) + 1,
        Math.max(start.getZ(), end.getZ()) + 1);
    OverlayBox box = new OverlayBox(id, min, max, color, fill, lineWidth);
    if (area.getName() != null) {
      box.withLabel(TextUtil.stripColors(area.getName()));
    }
    return box;
  }
}
