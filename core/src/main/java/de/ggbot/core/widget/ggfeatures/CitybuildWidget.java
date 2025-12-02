package de.ggbot.core.widget.ggfeatures;

import de.ggbot.core.GGBot;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;

public class CitybuildWidget extends TextHudWidget<TextHudWidgetConfig> {

  public static TextLine Citybuild;
  public CitybuildWidget(){
    super("cb");
    this.bindCategory(GGBot.getInstance().labyAPI().hudWidgetRegistry().categoryRegistry().getById("botggfeatures"));
  }

  public void load(TextHudWidgetConfig config){
    super.load(config);
    Citybuild = createLine(Component.translatable("ggbot.widget.citybuild.name"), Component.translatable("ggbot.widget.unknown"));

  }
}
