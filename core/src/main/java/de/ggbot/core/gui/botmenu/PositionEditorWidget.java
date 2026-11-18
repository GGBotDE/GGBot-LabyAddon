package de.ggbot.core.gui.botmenu;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import de.ggbot.core.overlay.OverlayBox;
import de.ggbot.core.overlay.OverlayColor;
import de.ggbot.core.overlay.OverlayManager;
import de.ggbot.sdk.model.Bot;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import net.labymod.api.client.world.phys.hit.BlockHitResult;
import net.labymod.api.client.world.phys.hit.HitResult;
import net.labymod.api.util.I18n;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds editors for module block-coordinate settings stored as X/Y/Z triples
 * (e.g. {@code sellAreaStartX/Y/Z}).
 *
 * <p>Implemented as a builder that appends plain widgets (labels, text fields and
 * text buttons) directly to the menu's content list, because those widget types
 * render reliably inside the scrolled list. Each editor offers three editable
 * fields plus clearly named buttons: "from look" (fill from the targeted block),
 * "save" (persist to the bot), "show"/"hide" (toggle a world marker) and "reset"
 * (discard the unsaved edit and reload the saved value).
 *
 * <p>Unsaved edits are kept in {@link BotMenuState} so a value placed in the menu
 * survives closing and reopening it; this lets the user set a position, leave to
 * inspect it in the world, return and confirm. Coordinate areas (a start/end pair,
 * e.g. the sell area) can additionally be shown as a real spanning box via
 * {@link #buildAreaToggle}.
 */
public final class PositionEditorWidget {

  /**
   * Defines all module locations editable through the menu: module, setting prefix, and label key.
   */
  private static final String[][] LOCATIONS = {
      {"sell", "sellAreaStart", "ggbot.botmenu.editor.sellAreaStart"},
      {"sell", "sellAreaEnd", "ggbot.botmenu.editor.sellAreaEnd"},
      {"sell", "sellDropLocation", "ggbot.botmenu.editor.sellDropLocation"},
      {"buy", "chestOverrideLocation", "ggbot.botmenu.editor.chestOverride"},
      {"ggfeatures", "trashItemsIntoChest", "ggbot.botmenu.editor.trashChest"},
  };

  /**
   * Lists corner pairs displayed as a single spanning box: module, start, end, and label key.
   */
  private static final String[][] AREA_PAIRS = {
      {"sell", "sellAreaStart", "sellAreaEnd", "ggbot.botmenu.editor.sellArea"},
  };

  private PositionEditorWidget() {
  }

  /**
   * Adds coordinate editors for all locations associated with a specific module.
   *
   * @param content the container for UI elements
   * @param bot the bot configuration
   * @param module the module name to filter locations by
   */
  public static void buildAll(VerticalListWidget<Widget> content, Bot bot, String module) {
    for (String[] location : LOCATIONS) {
      if (location[0].equals(module)) {
        build(content, bot, module, location[1], location[2]);
      }
    }
  }

  /**
   * Adds a toggle for spanning-box functionality for each corner pair of a module.
   *
   * @param content the container for UI elements
   * @param module the module name to filter corner pairs by
   */
  public static void buildAreaToggles(VerticalListWidget<Widget> content, String module) {
    for (String[] pair : AREA_PAIRS) {
      if (pair[0].equals(module)) {
        buildAreaToggle(content, module, pair[1], pair[2], pair[3]);
      }
    }
  }

  /** Appends a single coordinate editor for {@code module}/{@code prefix}. */
  private static void build(VerticalListWidget<Widget> content, Bot bot, String module,
      String prefix, String labelKey) {
    final String overlayId = pointId(module, prefix);
    final int color = colorFor(prefix);

    ComponentWidget label = ComponentWidget.i18n(labelKey);
    label.addId("pos-editor-label");
    content.addChildInitialized(label);

    final TextFieldWidget xField = field(prefix + "X", overlayId);
    final TextFieldWidget yField = field(prefix + "Y", overlayId);
    final TextFieldWidget zField = field(prefix + "Z", overlayId);

    HorizontalListWidget fields = new HorizontalListWidget();
    fields.addId("pos-editor-row");
    fields.addEntry(xField);
    fields.addEntry(yField);
    fields.addEntry(zField);
    content.addChildInitialized(fields);

    HorizontalListWidget actions = new HorizontalListWidget();
    actions.addId("pos-editor-actions");

    ButtonWidget look = ButtonWidget.i18n("ggbot.botmenu.editor.fromLook", () -> {
      fillFromLook(prefix, xField, yField, zField);
      redraw(overlayId, labelKey, color, prefix);
    });
    look.addId("pos-editor-btn");
    actions.addEntry(look);

    ButtonWidget save = ButtonWidget.i18n("ggbot.botmenu.editor.save", () -> {
      saveValues(bot, module, prefix, xField, yField, zField);
      redraw(overlayId, labelKey, color, prefix);
    });
    save.addId("pos-editor-btn");
    actions.addEntry(save);

    ButtonWidget show = ButtonWidget.i18n(showKey(overlayId));
    show.addId("pos-editor-btn");
    show.setPressable(() -> {
      boolean visible = !BotMenuState.get().isAreaVisible(overlayId);
      BotMenuState.get().setAreaVisible(overlayId, visible);
      if (visible) {
        updateMarker(overlayId, labelKey, color, prefix);
      } else {
        OverlayManager.getInstance().removeById(overlayId);
      }
      show.updateComponent(Component.translatable(showKey(overlayId)));
    });
    actions.addEntry(show);

    ButtonWidget reset = ButtonWidget.i18n("ggbot.botmenu.editor.reset", () ->
        resetValues(bot, module, prefix, overlayId, labelKey, color, xField, yField, zField));
    reset.addId("pos-editor-btn");
    actions.addEntry(reset);

    content.addChildInitialized(actions);

    loadValues(bot, module, prefix, overlayId, labelKey, color, xField, yField, zField);
  }

  /**
   * Appends a button that toggles a spanning box between two corner settings
   * (e.g. the sell area between {@code sellAreaStart} and {@code sellAreaEnd}),
   * letting the user see the whole covered region.
   */
  private static void buildAreaToggle(VerticalListWidget<Widget> content, String module,
      String startPrefix, String endPrefix, String labelKey) {
    final String overlayId = areaId(module, startPrefix);

    ButtonWidget area = ButtonWidget.i18n(areaKey(overlayId));
    area.addId("pos-editor-area-btn");
    area.setPressable(() -> {
      BotMenuState.get().setAreaVisible(overlayId, !BotMenuState.get().isAreaVisible(overlayId));
      refreshAreas();
      area.updateComponent(Component.translatable(areaKey(overlayId)));
    });
    content.addChildInitialized(area);
  }

  // ---- fields -------------------------------------------------------------

  private static TextFieldWidget field(String settingKey, String overlayId) {
    TextFieldWidget f = new TextFieldWidget();
    f.addId("pos-editor-field");
    f.maximalLength(8);
    String existing = BotMenuState.get().getEditValue(settingKey);
    if (existing != null) {
      f.setText(existing);
    }
    f.updateListener(v -> BotMenuState.get().setEditValue(settingKey, v));
    return f;
  }

  /**
   * Loads the saved values for a triple. Values already edited in the menu take
   * precedence (so unsaved edits survive a reopen); otherwise the saved value is
   * applied and remembered as the current edit value.
   */
  private static void loadValues(Bot bot, String module, String prefix, String overlayId,
      String labelKey, int color, TextFieldWidget xField, TextFieldWidget yField,
      TextFieldWidget zField) {
    BotRequests.getModuleSettingsAsync(GGBot.getInstance(), bot, module, settings -> {
      applyServerValue(settings, prefix + "X", xField);
      applyServerValue(settings, prefix + "Y", yField);
      applyServerValue(settings, prefix + "Z", zField);
      redraw(overlayId, labelKey, color, prefix);
    });
  }

  /**
   * Applies a saved value to a field, or stores it if the field is null.
   *
   * @param settings the map of saved settings
   * @param key the key associated with the value
   * @param field the text field to update or null
   */
  private static void applyServerValue(Map<String, String> settings, String key,
      TextFieldWidget field) {
    if (BotMenuState.get().hasEditValue(key)) {
      return; // Keep the unsaved edit.
    }
    String value = settings == null ? null : settings.get(key);
    // Treat missing/undefined values (including the literal string "null") as empty.
    if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value.trim())) {
      if (field != null) {
        field.setText(value);
      }
      BotMenuState.get().setEditValue(key, value);
    }
  }

  /**
   * Toggles the visibility of all module locations and their associated areas.
   * When enabled, previously saved coordinates are loaded first to ensure
   * all markers are displayed, even if the modules tab has never been opened.
   *
   * @param bot the bot configuration
   * @param visible whether to show or hide the elements
   */
  public static void setAllVisible(Bot bot, boolean visible) {
    for (String[] location : LOCATIONS) {
      BotMenuState.get().setAreaVisible(pointId(location[0], location[1]), visible);
    }
    for (String[] pair : AREA_PAIRS) {
      BotMenuState.get().setAreaVisible(areaId(pair[0], pair[1]), visible);
    }
    if (!visible) {
      refreshAll();
      return;
    }
    for (String module : modules()) {
      BotRequests.getModuleSettingsAsync(GGBot.getInstance(), bot, module, settings -> {
        for (String[] location : LOCATIONS) {
          if (!location[0].equals(module)) {
            continue;
          }
          applyServerValue(settings, location[1] + "X", null);
          applyServerValue(settings, location[1] + "Y", null);
          applyServerValue(settings, location[1] + "Z", null);
        }
        refreshAll();
      });
    }
  }

  /**
   * Returns a list of distinct modules that own locations, in the order they were declared.
   *
   * @return a list of module names in declaration order
   */
  private static List<String> modules() {
    List<String> modules = new ArrayList<>();
    for (String[] location : LOCATIONS) {
      if (!modules.contains(location[0])) {
        modules.add(location[0]);
      }
    }
    return modules;
  }

  /**
   * Redraws all location markers and areas based on current visibility and values.
   */
  private static void refreshAll() {
    for (String[] location : LOCATIONS) {
      String overlayId = pointId(location[0], location[1]);
      if (BotMenuState.get().isAreaVisible(overlayId)) {
        updateMarker(overlayId, location[2], colorFor(location[1]), location[1]);
      } else {
        OverlayManager.getInstance().removeById(overlayId);
      }
    }
    refreshAreas();
  }

  private static void fillFromLook(String prefix, TextFieldWidget xField,
      TextFieldWidget yField, TextFieldWidget zField) {
    HitResult hit = Laby.labyAPI().minecraft().getHitResult();
    if (hit == null || hit.type() != HitResult.HitType.BLOCK
        || !(hit instanceof BlockHitResult block)) {
      return;
    }
    setField(xField, prefix + "X", (int) Math.floor(block.getBlockPosition().getX()));
    setField(yField, prefix + "Y", (int) Math.floor(block.getBlockPosition().getY()));
    setField(zField, prefix + "Z", (int) Math.floor(block.getBlockPosition().getZ()));
  }

  private static void setField(TextFieldWidget field, String settingKey, int value) {
    String text = String.valueOf(value);
    field.setText(text);
    BotMenuState.get().setEditValue(settingKey, text);
  }

  // ---- save / reset -------------------------------------------------------

  private static void saveValues(Bot bot, String module, String prefix,
      TextFieldWidget xField, TextFieldWidget yField, TextFieldWidget zField) {
    GGBot addon = GGBot.getInstance();
    writeIfValid(addon, bot, module, prefix + "X", xField.getText());
    writeIfValid(addon, bot, module, prefix + "Y", yField.getText());
    writeIfValid(addon, bot, module, prefix + "Z", zField.getText());
  }

  private static void writeIfValid(GGBot addon, Bot bot, String module, String key, String value) {
    if (parse(value) == null) {
      return;
    }
    BotRequests.updateModuleSettingAsync(addon, bot, module, key, value.trim(), null);
  }

  private static void resetValues(Bot bot, String module, String prefix, String overlayId,
      String labelKey, int color, TextFieldWidget xField, TextFieldWidget yField,
      TextFieldWidget zField) {
    // Clear the fields first (which re-stores empty edits via their listeners), then
    // drop the edits so the reload below restores the saved values instead of "".
    xField.setText("");
    yField.setText("");
    zField.setText("");
    BotMenuState.get().clearEditValue(prefix + "X");
    BotMenuState.get().clearEditValue(prefix + "Y");
    BotMenuState.get().clearEditValue(prefix + "Z");
    loadValues(bot, module, prefix, overlayId, labelKey, color, xField, yField, zField);
  }

  // ---- world markers ------------------------------------------------------

  /** Redraws this point's marker (if shown) and any area that uses it. */
  private static void redraw(String overlayId, String labelKey, int color, String prefix) {
    if (BotMenuState.get().isAreaVisible(overlayId)) {
      updateMarker(overlayId, labelKey, color, prefix);
    }
    refreshAreas();
  }

  private static void updateMarker(String overlayId, String labelKey, int color, String prefix) {
    Integer x = parse(BotMenuState.get().getEditValue(prefix + "X"));
    Integer y = parse(BotMenuState.get().getEditValue(prefix + "Y"));
    Integer z = parse(BotMenuState.get().getEditValue(prefix + "Z"));
    OverlayManager manager = OverlayManager.getInstance();
    if (x == null || y == null || z == null) {
      manager.removeById(overlayId);
      return;
    }
    manager.addBox(OverlayBox.singleBlock(overlayId, x, y, z,
        color, fillFor(color), lineWidth()).withLabel(I18n.getTranslation(labelKey)));
  }

  /**
   * Updates visible corner-pair areas using the current edit values.
   */
  private static void refreshAreas() {
    OverlayManager manager = OverlayManager.getInstance();
    for (String[] pair : AREA_PAIRS) {
      String overlayId = areaId(pair[0], pair[1]);
      if (!BotMenuState.get().isAreaVisible(overlayId)) {
        manager.removeById(overlayId);
        continue;
      }
      Integer x1 = parse(BotMenuState.get().getEditValue(pair[1] + "X"));
      Integer y1 = parse(BotMenuState.get().getEditValue(pair[1] + "Y"));
      Integer z1 = parse(BotMenuState.get().getEditValue(pair[1] + "Z"));
      Integer x2 = parse(BotMenuState.get().getEditValue(pair[2] + "X"));
      Integer y2 = parse(BotMenuState.get().getEditValue(pair[2] + "Y"));
      Integer z2 = parse(BotMenuState.get().getEditValue(pair[2] + "Z"));
      if (x1 == null || y1 == null || z1 == null
          || x2 == null || y2 == null || z2 == null) {
        manager.removeById(overlayId);
        continue;
      }
      // Calculates the full area spanned by both corner blocks, including the max corner + 1.
      int color = colorFor(pair[1]);
      manager.addBox(OverlayBox.area(overlayId,
          Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
          Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, Math.max(z1, z2) + 1,
          color, fillFor(color), lineWidth())
          .withLabel(I18n.getTranslation(pair[3])));
    }
  }

  // ---- helpers ------------------------------------------------------------

  /**
   * Generates a unique identifier for a module location marker.
   *
   * @param module the module name
   * @param prefix the location prefix
   * @return the generated overlay ID
   */
  private static String pointId(String module, String prefix) {
    return "ggbot-modarea-" + module + ":" + prefix;
  }

  /**
   * Creates an overlay ID for a corner-pair area starting at the specified prefix.
   *
   * @param module the module name
   * @param startPrefix the prefix of the starting location
   * @return the generated overlay ID for the area
   */
  private static String areaId(String module, String startPrefix) {
    return pointId(module, startPrefix) + "-area";
  }

  /**
   * Determines the label key for a show/hide button based on overlay visibility.
   *
   * @param overlayId the ID of the overlay
   * @return the label key for the show/hide action
   */
  static String showKey(String overlayId) {
    return BotMenuState.get().isAreaVisible(overlayId)
        ? "ggbot.botmenu.editor.hide" : "ggbot.botmenu.editor.show";
  }

  private static String areaKey(String overlayId) {
    return BotMenuState.get().isAreaVisible(overlayId)
        ? "ggbot.botmenu.editor.hideArea" : "ggbot.botmenu.editor.showArea";
  }

  private static Integer parse(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static int colorFor(String prefix) {
    de.ggbot.core.cfg.OverlaySubConfig cfg =
        GGBot.getInstance().configuration().overlaySub;
    if (prefix.startsWith("sellArea")) {
      return cfg.sellAreaColor.get();
    }
    if (prefix.startsWith("sellDrop")) {
      return cfg.sellDropColor.get();
    }
    if (prefix.startsWith("chestOverride")) {
      return cfg.chestOverrideColor.get();
    }
    if (prefix.startsWith("trash")) {
      return cfg.trashChestColor.get();
    }
    return OverlayColor.CYAN;
  }

  /** Translucent fill colour for {@code base}, or transparent if fill is disabled. */
  private static int fillFor(int base) {
    de.ggbot.core.cfg.OverlaySubConfig cfg =
        GGBot.getInstance().configuration().overlaySub;
    if (!Boolean.TRUE.equals(cfg.fillAreas.get())) {
      return 0;
    }
    return OverlayColor.withAlpha(base, cfg.fillOpacity.get());
  }

  private static float lineWidth() {
    return GGBot.getInstance().configuration().overlaySub.lineWidth.get();
  }
}
