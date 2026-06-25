package de.ggbot.core.utils;

import de.ggbot.core.GGBot;
import net.labymod.api.Laby;

/**
 * Decides whether overlays are drawn through the new canvas-based render engine
 * (required on 1.21.8 and newer, where the legacy render pipeline produces no
 * output) or the legacy pipeline (correct on 1.8 - 1.21.5).
 *
 * <p>The choice is driven by the "Overlay rendering" setting: AUTOMATIC picks the
 * engine from the running Minecraft version, while ALWAYS / OFF force it.
 */
public final class RenderEngine {

  /** Overlay render-engine selection. */
  public enum Mode {
    AUTOMATIC,
    ALWAYS,
    OFF
  }

  /**
   * Lowest protocol version that needs the canvas engine. 1.21.5 is 770; the canvas
   * is needed from 1.21.6+ (and every newer/calendar-versioned release, whose
   * protocol numbers are higher still).
   */
  private static final int CANVAS_MIN_PROTOCOL = 771;

  private RenderEngine() {
  }

  /** Whether overlays should be drawn through the new canvas engine. */
  public static boolean useCanvas() {
    try {
      Mode mode = GGBot.getInstance().configuration().generalSub.renderMode.get();
      if (mode == Mode.ALWAYS) {
        return true;
      }
      if (mode == Mode.OFF) {
        return false;
      }
      return Laby.labyAPI().minecraft().getProtocolVersion() >= CANVAS_MIN_PROTOCOL;
    } catch (Exception e) {
      return false;
    }
  }
}
