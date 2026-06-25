package de.ggbot.core.gui.botmenu;

import de.ggbot.core.overlay.WorldPosition;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Process-wide runtime state for the bot management menu and its world overlays.
 *
 * <p>Holds the live toggle states (which mirror the persistent LabyMod settings
 * but can be flipped from the menu), the per-location overlay visibility, the
 * cached set of modules enabled on the selected bot (used to decide which player
 * interactions to show), and the WorldEdit-style {@code pos1}/{@code pos2} selection.
 */
public final class BotMenuState {

  private static final BotMenuState INSTANCE = new BotMenuState();

  public static BotMenuState get() {
    return INSTANCE;
  }

  private boolean highlightKickAreas = false;
  private boolean showChestContents = false;
  private String itemSearch = "";

  /** Keys ({@code module:settingPrefix}) of module locations shown in the world. */
  private final Set<String> visibleAreas = new HashSet<>();

  /**
   * In-progress (possibly unsaved) coordinate edits, keyed by the full setting key
   * (e.g. {@code sellAreaStartX}). Kept process-wide so a value set in the menu
   * survives closing and reopening it (the user can place a position, leave to look
   * at it in the world, come back and confirm) until it is saved or reset.
   */
  private final Map<String, String> editValues = new HashMap<>();

  /** Modules currently enabled on the selected bot (lower-case names). */
  private volatile Set<String> activeModules = new HashSet<>();

  private WorldPosition pos1;
  private WorldPosition pos2;

  private BotMenuState() {}

  public boolean isHighlightKickAreas() {
    return highlightKickAreas;
  }

  public void setHighlightKickAreas(boolean v) {
    this.highlightKickAreas = v;
  }

  public boolean isShowChestContents() {
    return showChestContents;
  }

  public void setShowChestContents(boolean v) {
    this.showChestContents = v;
  }

  /** Returns whether a module location ({@code module:prefix}) is shown in the world. */
  public boolean isAreaVisible(String key) {
    synchronized (visibleAreas) {
      return visibleAreas.contains(key);
    }
  }

  /** Sets whether a module location ({@code module:prefix}) is shown in the world. */
  public void setAreaVisible(String key, boolean visible) {
    synchronized (visibleAreas) {
      if (visible) {
        visibleAreas.add(key);
      } else {
        visibleAreas.remove(key);
      }
    }
  }

  public void setActiveModules(Set<String> modules) {
    this.activeModules = modules == null ? new HashSet<>() : modules;
  }

  public boolean isModuleActive(String module) {
    return activeModules.contains(module);
  }

  /** Whether the active-module set has been populated (false means "unknown yet"). */
  public boolean hasActiveModules() {
    return !activeModules.isEmpty();
  }

  /** Returns the in-progress edit value for a setting key, or {@code null}. */
  public String getEditValue(String key) {
    synchronized (editValues) {
      return editValues.get(key);
    }
  }

  /** Returns whether an in-progress edit value exists for a setting key. */
  public boolean hasEditValue(String key) {
    synchronized (editValues) {
      return editValues.containsKey(key);
    }
  }

  /** Stores an in-progress edit value for a setting key. */
  public void setEditValue(String key, String value) {
    synchronized (editValues) {
      editValues.put(key, value == null ? "" : value);
    }
  }

  /** Removes an in-progress edit value so the saved value is used again. */
  public void clearEditValue(String key) {
    synchronized (editValues) {
      editValues.remove(key);
    }
  }

  public String getItemSearch() {
    return itemSearch == null ? "" : itemSearch;
  }

  public void setItemSearch(String search) {
    this.itemSearch = search == null ? "" : search;
  }

  public WorldPosition getPos1() {
    return pos1;
  }

  public void setPos1(WorldPosition pos1) {
    this.pos1 = pos1;
  }

  public WorldPosition getPos2() {
    return pos2;
  }

  public void setPos2(WorldPosition pos2) {
    this.pos2 = pos2;
  }
}
