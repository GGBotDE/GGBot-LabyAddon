package de.ggbot.core.widget.ggfeatures;

import de.ggbot.core.GGBot;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;

public class BotMoneyWidget extends TextHudWidget<TextHudWidgetConfig> {

  public static TextLine BotMoney;
  public BotMoneyWidget(){
    super("botmoney");
    this.bindCategory(GGBot.getInstance().labyAPI().hudWidgetRegistry().categoryRegistry().getById("botggfeatures"));

  }

  public void load(TextHudWidgetConfig config){
    super.load(config);
    BotMoney = createLine(Component.translatable("ggbot.widget.money.name"), Component.translatable("ggbot.widget.unknown"));
  }
}
