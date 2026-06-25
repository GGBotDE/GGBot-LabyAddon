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

  /** Registered start/end areas, redrawn live when any of their values change. */
  private static final List<AreaDef> AREAS = new ArrayList<>();

  private PositionEditorWidget() {
  }

  /** Appends a single coordinate editor for {@code module}/{@code prefix}. */
  public static void build(VerticalListWidget<Widget> content, Bot bot, String module,
      String prefix, String labelKey) {
    final String overlayId = "ggbot-modarea-" + module + ":" + prefix;
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
  public static void buildAreaToggle(VerticalListWidget<Widget> content, String module,
      String startPrefix, String endPrefix, String labelKey) {
    AreaDef def = new AreaDef(module, startPrefix, endPrefix, labelKey, colorFor(startPrefix));
    registerArea(def);

    ButtonWidget area = ButtonWidget.i18n(areaKey(def.overlayId));
    area.addId("pos-editor-area-btn");
    area.setPressable(() -> {
      boolean visible = !BotMenuState.get().isAreaVisible(def.overlayId);
      BotMenuState.get().setAreaVisible(def.overlayId, visible);
      refreshAreas();
      area.updateComponent(Component.translatable(areaKey(def.overlayId)));
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

  private static void applyServerValue(Map<String, String> settings, String key,
      TextFieldWidget field) {
    if (BotMenuState.get().hasEditValue(key)) {
      return; // Keep the unsaved edit.
    }
    String value = settings == null ? null : settings.get(key);
    // Treat missing/undefined values (including the literal string "null") as empty.
    if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value.trim())) {
      field.setText(value);
      BotMenuState.get().setEditValue(key, value);
    }
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

  private static void registerArea(AreaDef def) {
    synchronized (AREAS) {
      AREAS.removeIf(a -> a.overlayId.equals(def.overlayId));
      AREAS.add(def);
    }
  }

  /** Redraws every visible registered area from the current edit values. */
  private static void refreshAreas() {
    OverlayManager manager = OverlayManager.getInstance();
    synchronized (AREAS) {
      for (AreaDef def : AREAS) {
        if (!BotMenuState.get().isAreaVisible(def.overlayId)) {
          manager.removeById(def.overlayId);
          continue;
        }
        Integer x1 = parse(BotMenuState.get().getEditValue(def.startPrefix + "X"));
        Integer y1 = parse(BotMenuState.get().getEditValue(def.startPrefix + "Y"));
        Integer z1 = parse(BotMenuState.get().getEditValue(def.startPrefix + "Z"));
        Integer x2 = parse(BotMenuState.get().getEditValue(def.endPrefix + "X"));
        Integer y2 = parse(BotMenuState.get().getEditValue(def.endPrefix + "Y"));
        Integer z2 = parse(BotMenuState.get().getEditValue(def.endPrefix + "Z"));
        if (x1 == null || y1 == null || z1 == null
            || x2 == null || y2 == null || z2 == null) {
          manager.removeById(def.overlayId);
          continue;
        }
        // Span the full area inclusive of both corner blocks (max corner + 1).
        int color = colorFor(def.startPrefix);
        manager.addBox(OverlayBox.area(def.overlayId,
            x1, y1, z1, x2 + 1, y2 + 1, z2 + 1,
            color, fillFor(color), lineWidth())
            .withLabel(I18n.getTranslation(def.labelKey)));
      }
    }
  }

  // ---- helpers ------------------------------------------------------------

  private static String showKey(String overlayId) {
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

  /** Immutable definition of a start/end coordinate area shown as one box. */
  private static final class AreaDef {
    final String startPrefix;
    final String endPrefix;
    final String labelKey;
    final int color;
    final String overlayId;

    AreaDef(String module, String startPrefix, String endPrefix, String labelKey, int color) {
      this.startPrefix = startPrefix;
      this.endPrefix = endPrefix;
      this.labelKey = labelKey;
      this.color = color;
      this.overlayId = "ggbot-modarea-" + module + ":" + startPrefix + "-area";
    }
  }
}
