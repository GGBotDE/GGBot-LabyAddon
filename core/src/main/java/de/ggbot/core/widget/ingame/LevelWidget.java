package de.ggbot.core.widget.ingame;

import de.ggbot.core.GGBot;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;

public class LevelWidget extends TextHudWidget<TextHudWidgetConfig> {

  /**
   * Identifies the unique registry ID for the level widget.
   */
  public static final String WIDGET_ID = "botlevel";

  private static TextLine levelLine;

  /**
   * Initializes the level widget with its configuration and category binding.
   */
  public LevelWidget() {
    super(WIDGET_ID);
    this.bindCategory(GGBot.getInstance().labyAPI().hudWidgetRegistry().categoryRegistry().getById("botingame"));
    this.setIcon(Icon.texture(ResourceLocation.create("ggbot", "themes/vanilla/textures/icons/items/experience_bottle.png")));
  }

  /**
   * Sets up the text line with a label and default placeholder using the provided configuration.
   *
   * @param config the widget configuration
   */
  @Override
  public void load(TextHudWidgetConfig config) {
    super.load(config);
    levelLine = createLine(
        Component.translatable("ggbot.widget.level.name"),
        Component.translatable("ggbot.widget.unknown"));
  }

  /**
   * Updates the text line with the new value.
   *
   * @param value the new value to display
   */
  public static void update(Object value) {
    if (levelLine != null) levelLine.updateAndFlush(value);
  }
}
