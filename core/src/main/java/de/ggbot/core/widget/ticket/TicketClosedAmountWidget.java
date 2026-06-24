package de.ggbot.core.widget.ticket;

import de.ggbot.core.GGBot;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.configuration.loader.annotation.SpriteSlot;
import net.labymod.api.configuration.loader.annotation.SpriteTexture;

public class TicketClosedAmountWidget extends TextHudWidget<TextHudWidgetConfig> {

  /** Registry ID for this widget. */
  public static final String WIDGET_ID = "closedTickets";

  private static TextLine closedTicketAmountLine;

  /**
   * Creates the closed-ticket-count HUD widget and binds it to the ticket category.
   */
  public TicketClosedAmountWidget() {
    super(WIDGET_ID);
    this.bindCategory(GGBot.getInstance().labyAPI().hudWidgetRegistry().categoryRegistry().getById("botticket"));
    this.setIcon(Icon.texture(ResourceLocation.create("ggbot", "themes/vanilla/textures/icons/items/book.png")));
  }

  /**
   * Initializes the text line with its label and a default placeholder value.
   *
   * @param config the widget configuration
   */
  @Override
  public void load(TextHudWidgetConfig config) {
    super.load(config);
    closedTicketAmountLine = createLine(
        Component.translatable("ggbot.widget.ticket.closed.name"),
        Component.translatable("ggbot.widget.unknown"));
  }

  /**
   * Updates the displayed closed-ticket count.
   *
   * @param value the new value to display
   */
  public static void update(Object value) {
    if (closedTicketAmountLine != null) closedTicketAmountLine.updateAndFlush(value);
  }
}
