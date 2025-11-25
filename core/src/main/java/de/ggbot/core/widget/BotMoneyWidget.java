package de.ggbot.core.widget;

import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;

public class BotMoneyWidget extends TextHudWidget<TextHudWidgetConfig> {

  public static TextLine BotMoney;
  public BotMoneyWidget(){
    super("botmoney");
  }

  public void load(TextHudWidgetConfig config){
    super.load(config);
    BotMoney = createLine("Bot Money", "0.0$");
  }
}
