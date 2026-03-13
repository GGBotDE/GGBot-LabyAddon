package de.ggbot.core.widget.info;

import de.ggbot.core.GGBot;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;

public class StatusWidget extends TextHudWidget<TextHudWidgetConfig> {

  /** Registry ID for this widget. */
  public static final String WIDGET_ID = "status";

  private static TextLine statusLine;

  /**
   * Creates the bot-status HUD widget and binds it to the info category.
   */
  public StatusWidget() {
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
    statusLine = createLine(
        Component.translatable("ggbot.widget.status.name"),
        Component.translatable("ggbot.widget.unknown"));
  }

  /**
   * Updates the displayed bot status.
   *
   * @param value the new value to display
   */
  public static void update(Object value) {
    if (statusLine != null) statusLine.updateAndFlush(value);
  }
}
