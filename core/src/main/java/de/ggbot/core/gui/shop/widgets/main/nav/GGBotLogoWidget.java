package de.ggbot.core.gui.shop.widgets.main.nav;

import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.widget.widgets.renderer.IconWidget;

/** Small logo icon displayed in the shop navigation bar. */
public class GGBotLogoWidget extends IconWidget {

  /** Constructs the widget loading the GGBot logo from the CDN. */
  public GGBotLogoWidget() {
    super(Icon.url("https://www.ggbot.de/assets/img/logo.png"));
  }
}
