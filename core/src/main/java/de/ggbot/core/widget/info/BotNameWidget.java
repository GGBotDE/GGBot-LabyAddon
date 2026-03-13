package de.ggbot.core.widget.info;

import de.ggbot.core.GGBot;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;

public class BotNameWidget extends TextHudWidget<TextHudWidgetConfig> {

  /** Registry ID for this widget. */
  public static final String WIDGET_ID = "botname";

  private static TextLine botNameLine;

  /**
   * Creates the bot-name HUD widget and binds it to the info category.
   */
  public BotNameWidget() {
    super(WIDGET_ID);
    this.bindCategory(GGBot.getInstance().labyAPI().hudWidgetRegistry().categoryRegistry().getById("botinfo"));
  }

  /**
   * Initializes the text line with its label and a default placeholder value.
   *
   * @param config the widget configuration
   */
  @Override
  public void load(TextHudWidgetConfig config) {
    super.load(config);
    botNameLine = createLine(
        Component.translatable("ggbot.widget.botname.name"),
        Component.translatable("ggbot.widget.unknown"));
  }

  /**
   * Updates the displayed bot name.
   *
   * @param value the new value to display
   */
  public static void update(Object value) {
    if (botNameLine != null) botNameLine.updateAndFlush(value);
  }
}
