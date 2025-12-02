package de.ggbot.core.widget.ggfeatures;

import de.ggbot.core.GGBot;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;

public class PlotWidget extends TextHudWidget<TextHudWidgetConfig> {

  public static TextLine Plot;
  public PlotWidget(){
    super("plot");
    this.bindCategory(GGBot.getInstance().labyAPI().hudWidgetRegistry().categoryRegistry().getById("botggfeatures"));
  }

  public void load(TextHudWidgetConfig config){
    super.load(config);
    Plot = createLine(Component.translatable("ggbot.widget.plot.name"), Component.translatable("ggbot.widget.unknown"));

  }
}
