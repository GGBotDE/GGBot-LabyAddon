package de.ggbot.core.widget.ticket;

import de.ggbot.core.GGBot;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;

public class TicketOpenAmountWidget extends TextHudWidget<TextHudWidgetConfig> {

  /** Registry ID for this widget. */
  public static final String WIDGET_ID = "openedTickets";

  private static TextLine openTicketAmountLine;

  /**
   * Creates the open-ticket-count HUD widget and binds it to the ticket category.
   */
  public TicketOpenAmountWidget() {
    super(WIDGET_ID);
    this.bindCategory(GGBot.getInstance().labyAPI().hudWidgetRegistry().categoryRegistry().getById("botticket"));
  }

  /**
   * Initializes the text line with its label and a default placeholder value.
   *
   * @param config the widget configuration
   */
  @Override
  public void load(TextHudWidgetConfig config) {
    super.load(config);
    openTicketAmountLine = createLine(
        Component.translatable("ggbot.widget.ticket.open.name"),
        Component.translatable("ggbot.widget.unknown"));
  }

  /**
   * Updates the displayed open-ticket count.
   *
   * @param value the new value to display
   */
  public static void update(Object value) {
    if (openTicketAmountLine != null) openTicketAmountLine.updateAndFlush(value);
  }
}
