package de.ggbot.core.widget.info;

import de.ggbot.core.GGBot;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;

public class BotNameWidget extends TextHudWidget<TextHudWidgetConfig> {

  public static TextLine BotName;
  public BotNameWidget(){
    super("botname");
    this.bindCategory(GGBot.getInstance().labyAPI().hudWidgetRegistry().categoryRegistry().getById("botinfo"));
  }

  public void load(TextHudWidgetConfig config){
    super.load(config);
    BotName = createLine(Component.translatable("ggbot.widget.botname.name"), Component.translatable("ggbot.widget.unknown"));

  }
}
