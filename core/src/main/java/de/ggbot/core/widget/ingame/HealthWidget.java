package de.ggbot.core.widget.ingame;

import de.ggbot.core.GGBot;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;

public class HealthWidget extends TextHudWidget<TextHudWidgetConfig> {

  public static TextLine Health;
  public HealthWidget(){
    super("health");
    this.bindCategory(GGBot.getInstance().labyAPI().hudWidgetRegistry().categoryRegistry().getById("botingame"));
  }

  public void load(TextHudWidgetConfig config){
    super.load(config);
    Health = createLine(Component.translatable("ggbot.widget.health.name"), Component.translatable("ggbot.widget.unknown"));

  }
}
