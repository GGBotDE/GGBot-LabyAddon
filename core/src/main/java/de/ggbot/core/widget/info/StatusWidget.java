package de.ggbot.core.widget.info;

import de.ggbot.core.GGBot;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;

public class StatusWidget extends TextHudWidget<TextHudWidgetConfig> {

  public static TextLine Status;
  public StatusWidget(){
    super("status");
    this.bindCategory(GGBot.getInstance().labyAPI().hudWidgetRegistry().categoryRegistry().getById("botinfo"));
  }

  public void load(TextHudWidgetConfig config){
    super.load(config);
    Status = createLine(Component.translatable("ggbot.widget.status.name"), Component.translatable("ggbot.widget.unknown"));

  }
}
