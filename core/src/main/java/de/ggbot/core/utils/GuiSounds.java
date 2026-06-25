package de.ggbot.core.utils;

import net.labymod.api.Laby;

/**
 * Small helper for playing the standard UI click sound for GGBot menus, so opening
 * menus and pressing their buttons gives audible feedback.
 */
public final class GuiSounds {

  private GuiSounds() {
  }

  /** Plays the vanilla button-press click sound (best-effort). */
  public static void click() {
    try {
      Laby.labyAPI().minecraft().sounds().playButtonPress();
    } catch (Exception ignored) {
      // Sound is non-essential; never let it break the UI.
    }
  }
}
