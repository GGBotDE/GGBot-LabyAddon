package de.ggbot.core.widget.ticket;

import de.ggbot.core.GGBot;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;

public class TicketOpenAmountWidget extends TextHudWidget<TextHudWidgetConfig> {
  public static TextLine TicketAmount;
  public TicketOpenAmountWidget(){
    super("openedTickets");
    this.bindCategory(GGBot.getInstance().labyAPI().hudWidgetRegistry().categoryRegistry().getById("botticket"));
  }

  public void load(TextHudWidgetConfig config){
    super.load(config);
    TicketAmount = createLine(Component.translatable("ggbot.widget.ticket.open.name"), Component.translatable("ggbot.widget.unknown"));
  }
}
