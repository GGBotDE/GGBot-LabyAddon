package de.ggbot.core.generated.settings;

import de.ggbot.core.cfg.BotDropDown;
import java.lang.Override;
import java.util.List;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.configuration.settings.widget.WidgetStorage;

public final class DefaultWidgetStorage implements WidgetStorage {
  @Override
  public void store(List<Class<? extends Widget>> widgets) {
    widgets.add(BotDropDown.class);
  }
}
