package de.ggbot.core.widget.ticket;

import de.ggbot.core.GGBot;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;

public class TicketAmountWidget extends TextHudWidget<TextHudWidgetConfig> {

  /** Registry ID for this widget. */
  public static final String WIDGET_ID = "tickets";

  private static TextLine ticketAmountLine;

  /**
   * Creates the total-ticket-count HUD widget and binds it to the ticket category.
   */
  public TicketAmountWidget() {
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
    ticketAmountLine = createLine(
        Component.translatable("ggbot.widget.ticket.amount.name"),
        Component.translatable("ggbot.widget.unknown"));
  }

  /**
   * Updates the displayed ticket count.
   *
   * @param value the new value to display
   */
  public static void update(Object value) {
    if (ticketAmountLine != null) ticketAmountLine.updateAndFlush(value);
  }
}
