package de.ggbot.core.widget;

import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;

public class StatusWidget extends TextHudWidget<TextHudWidgetConfig> {

  public static TextLine Status;
  public StatusWidget(){
    super("status");
  }

  public void load(TextHudWidgetConfig config){
    super.load(config);
    Status = createLine("Status", "Unknown");

  }
}
