package de.ggbot.core.widget.ingame;

import de.ggbot.core.GGBot;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;

public class PlayersWidget extends TextHudWidget<TextHudWidgetConfig> {

  /**
   * Identifies the unique registry ID for the players widget.
   */
  public static final String WIDGET_ID = "botplayers";

  private static TextLine playersLine;

  /**
   * Initializes the players widget with its configuration and category binding.
   */
  public PlayersWidget() {
    super(WIDGET_ID);
    this.bindCategory(GGBot.getInstance().labyAPI().hudWidgetRegistry().categoryRegistry().getById("botingame"));
    this.setIcon(Icon.texture(ResourceLocation.create("ggbot", "themes/vanilla/textures/icons/items/player_head.png")));
  }

  /**
   * Sets up the text line with a label and default placeholder using the provided configuration.
   *
   * @param config the widget configuration
   */
  @Override
  public void load(TextHudWidgetConfig config) {
    super.load(config);
    playersLine = createLine(
        Component.translatable("ggbot.widget.players.name"),
        Component.translatable("ggbot.widget.unknown"));
  }

  /**
   * Updates the text line with the new value.
   *
   * @param value the new value to display
   */
  public static void update(Object value) {
    if (playersLine != null) playersLine.updateAndFlush(value);
  }
}
