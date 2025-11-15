package de.ggbot.core.widget;

import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;

public class BotNameWidget extends TextHudWidget<TextHudWidgetConfig> {

  public static TextLine BotName;
  public BotNameWidget(){
    super("botname");
  }

  public void load(TextHudWidgetConfig config){
    super.load(config);
    BotName = createLine("Bot Name", "Unknown");

  }
}
