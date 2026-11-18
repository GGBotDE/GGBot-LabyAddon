package de.ggbot.core.gui.shop.widgets.main.nav;

import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.widget.widgets.renderer.IconWidget;

@AutoWidget
public class GGBotLogoWidget extends IconWidget {

  public GGBotLogoWidget() {
    super(Icon.url("https://www.ggbot.de/assets/img/logo.png"));
  }
}
